package com.dji.sample.df.frogJumpDf.service;

import com.dji.sample.df.frogJumpDf.model.dto.FrogJumpLandingRecommendationDTO.Endpoint;
import org.w3c.dom.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public final class WpmlLandingEndpointParser {
    private static final int MAX_INPUT = 16 * 1024 * 1024;
    private static final int MAX_INFLATED = 8 * 1024 * 1024;
    private static final String KML = "http://www.opengis.net/kml/2.2";

    private WpmlLandingEndpointParser() {}

    public static Endpoint parse(InputStream input) {
        try {
            byte[] archive = input.readNBytes(MAX_INPUT + 1);
            if (archive.length > MAX_INPUT) throw invalid("KMZ 文件超过 16 MiB 下载大小限制");
            byte[] wpml = null;
            int entries = 0, inflated = 0;
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
                ZipEntry entry;
                byte[] buffer = new byte[8192];
                while ((entry = zip.getNextEntry()) != null) {
                    if (++entries > 128) throw invalid("KMZ 文件超过 128 个压缩条目限制");
                    boolean target = "wpmz/waylines.wpml".equals(entry.getName()) && !entry.isDirectory();
                    if (target && wpml != null) throw invalid("KMZ 文件中存在重复的执行航线 WPML 条目");
                    ByteArrayOutputStream content = target ? new ByteArrayOutputStream() : null;
                    int count;
                    while ((count = zip.read(buffer)) != -1) {
                        inflated += count;
                        if (inflated > MAX_INFLATED) throw invalid("KMZ 文件超过 8 MiB 解压大小限制");
                        if (target) content.write(buffer, 0, count);
                    }
                    if (target) wpml = content.toByteArray();
                    zip.closeEntry();
                }
            }
            if (wpml == null) throw invalid("缺少执行航线文件 wpmz/waylines.wpml");
            return parseXml(wpml);
        } catch (NumberFormatException e) {
            throw invalid("航点坐标、航点序号或航线 ID 的数字格式错误");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw invalid("执行航线 WPML/KMZ 格式无效或不符合安全要求");
        }
    }

    private static Endpoint parseXml(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        var builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new DefaultHandler());
        Document document = builder.parse(new ByteArrayInputStream(xml));
        NodeList folders = document.getElementsByTagNameNS(KML, "Folder");
        // FlightTaskService sends the whole file, with no executed wayline ID selection.
        if (folders.getLength() != 1) throw invalid("无法唯一确定执行航线：WPML 必须仅含一个航线 Folder，请先拆分航线再获取推荐");
        Element folder = (Element) folders.item(0);
        int waylineId = nonnegative(wpmlChild(folder, "waylineId").getTextContent());
        List<Endpoint> points = new ArrayList<>();
        Set<Integer> indices = new HashSet<>();
        for (Node node = folder.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (!(node instanceof Element) || !KML.equals(node.getNamespaceURI()) || !"Placemark".equals(node.getLocalName())) continue;
            Element placemark = (Element) node;
            int index = nonnegative(wpmlChild(placemark, "index").getTextContent());
            if (!indices.add(index)) throw invalid("航点序号重复");
            Element point = child(placemark, KML, "Point");
            String[] coordinates = child(point, KML, "coordinates").getTextContent().trim().split(",", -1);
            if (coordinates.length < 2 || coordinates.length > 3) throw invalid("航点坐标组格式错误");
            double lon = Double.parseDouble(coordinates[0].trim()), lat = Double.parseDouble(coordinates[1].trim());
            if (!validCoordinates(lon, lat)) throw invalid("航点 WGS84 经纬度无效");
            if (coordinates.length == 3 && !Double.isFinite(Double.parseDouble(coordinates[2].trim()))) throw invalid("航点高度无效");
            points.add(new Endpoint(lon, lat, waylineId, index));
        }
        return points.stream().max(Comparator.comparingInt(Endpoint::getWaypointIndex))
                .orElseThrow(() -> invalid("执行航线中没有带序号的 Point 航点"));
    }

    static boolean validCoordinates(Double lon, Double lat) {
        return lon != null && lat != null && Double.isFinite(lon) && Double.isFinite(lat)
                && Math.abs(lon) <= 180 && Math.abs(lat) <= 90 && !(lon == 0 && lat == 0);
    }

    private static Element wpmlChild(Element parent, String name) {
        Element match = null;
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            String ns = n.getNamespaceURI();
            if (n instanceof Element && name.equals(n.getLocalName()) && ns != null && ns.matches("http://www\\.dji\\.com/wpmz/1\\.0\\.[0-9]+")) {
                if (match != null) throw invalid("WPML 字段重复：" + name);
                match = (Element) n;
            }
        }
        if (match == null) throw invalid("缺少 WPML 字段：" + name);
        return match;
    }

    private static Element child(Element parent, String namespace, String name) {
        Element match = null;
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element && namespace.equals(n.getNamespaceURI()) && name.equals(n.getLocalName())) {
                if (match != null) throw invalid("执行航点字段重复：" + name);
                match = (Element) n;
            }
        }
        if (match == null) throw invalid("缺少执行航点字段：" + name);
        return match;
    }

    private static int nonnegative(String value) {
        int number = Integer.parseInt(value.trim());
        if (number < 0) throw invalid("WPML 航点序号或航线 ID 不能为负数");
        return number;
    }

    private static IllegalArgumentException invalid(String reason) { return new IllegalArgumentException(reason); }
}
