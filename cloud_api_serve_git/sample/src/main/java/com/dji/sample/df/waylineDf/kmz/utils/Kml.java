package com.dji.sample.df.waylineDf.kmz.utils;

import com.dji.sample.df.waylineDf.kmz.entity.MissionConfig;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.util.*;

//提取KML航线里的坐标值
public class Kml {
    public static Collection<? extends String> parseXmlWithDom4j(InputStream input) throws Exception {
        SAXReader reader = new SAXReader();
        Document document = null;
        try {
            document = reader.read(input);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        Element root = document.getRootElement();//获取kml文件的根结点
        return getCoordinates(root);
    }

    //遍历当前节点下的全部节点
    public static Collection<? extends String> getCoordinates(Element node) {
        List<String> coordinatesList = new ArrayList<>();
        if ("coordinates".equals(node.getName())) {
            String coordinatesStr = node.getTextTrim();
            String[] coordinatesArr = coordinatesStr.split(",");
            List<String> coordinatesList1 = Arrays.asList(coordinatesArr);
            coordinatesList.add(String.valueOf(coordinatesList1));
        }
        Iterator<Element> iterator = node.elementIterator();
        while (iterator.hasNext()) {
            Element e = iterator.next();
            coordinatesList.addAll(getCoordinates(e));
        }
        return coordinatesList;
    }

    public MissionConfig getMissionConfig(Element node){
        // 获取wpml:missionConfig节点
        Element missionConfigElement = node.element("wpml:missionConfig");
        System.out.println(missionConfigElement);
        return getMissionConfig(node);
    }


}
