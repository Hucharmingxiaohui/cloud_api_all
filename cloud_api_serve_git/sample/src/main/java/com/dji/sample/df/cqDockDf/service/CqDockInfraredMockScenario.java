package com.dji.sample.df.cqDockDf.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.df.cqDockDf.config.CqDockProperties;
import com.dji.sample.df.substationDf.dao.UniPointMapper2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
// 本机点位id
//  "32e5360075f5469d", "10dcd05424414053", "3302465292ae465f", "a8afbe1d1ad846e9",
//          "eaa9e4b43df34aac", "ed53f8667661488a", "92a8738f5f4b4838", "fb11995c69424381",
/** 仅模拟下级EUA任务和图片列表，分析与测温使用正常业务服务。 */
@Slf4j
@Component
public class CqDockInfraredMockScenario {
    public static final String STATION = "dzlbdz-uav";
    public static final String ROUTE = "a14a7329cda441a18e258b5aa955f52c";
    private static final String TASK_PREFIX = "mock-task-eight-";
    private static final String PICTURE_PREFIX = "mock-scene-eight-";
    private static final List<String> CODES = Collections.unmodifiableList(Arrays.asList(
          "490745d1fef949c1", "834396ba69804447","5f1ae18a16f34fe6","e9f3a0966cd14ce9",
            "3b3f56b0a5d242a7","58121cbaff5f4278", "9c12d377327c4e14","e859abb10a824687"));
    private final CqDockProperties properties;
    private final UniPointMapper2 pointMapper;
    private final Map<String, List<Fixture>> tasks = new ConcurrentHashMap<>();
    private final Map<String, Fixture> pictures = new ConcurrentHashMap<>();

    public CqDockInfraredMockScenario(CqDockProperties properties, UniPointMapper2 pointMapper) {
        this.properties = properties;
        this.pointMapper = pointMapper;
    }

    private boolean enabled() {
        return properties.isMockEnabled() && properties.getMockScenario().isEnabled();
    }

    public boolean matches(String route, String station) {
        return enabled() && ROUTE.equals(route) && STATION.equals(station);
    }

    public void validateDispatch(String route, String station) {
        if (matches(route, station)) loadFixtures();
    }

    public String register(String route, String station) {
        if (!matches(route, station)) return null;
        List<Fixture> fixtures = loadFixtures();
        String task = TASK_PREFIX + UUID.randomUUID();
        for (int i = 0; i < fixtures.size(); i++) {
            Fixture fixture = fixtures.get(i);
            fixture.task = task;
            fixture.id = PICTURE_PREFIX + task.substring(TASK_PREFIX.length()) + "-" + (i + 1);
            pictures.put(fixture.id, fixture);
        }
        tasks.put(task, fixtures);
        log.info("[cq-dock] 八图下级EUA模拟任务已注册：taskId={}, route={}, 前4张可见光、后4张红外，仅模拟下级接口和本地素材，不模拟分析及温度，测温走真实服务", task, route);
        return task;
    }

    private List<Fixture> loadFixtures() {
        String sourcePath = properties.getMockScenario().getSourcePath();
        if (sourcePath == null || sourcePath.trim().isEmpty()) {
            throw new IllegalStateException("八图素材目录不能为空：cq-dock.mock-scenario.source-path");
        }
        Path root = Paths.get(sourcePath).toAbsolutePath().normalize();
        if (!Files.isDirectory(root) || !Files.isReadable(root)) {
            throw new IllegalStateException("八图素材目录不存在或不可读，请设置cq-dock.mock-scenario.source-path为当前系统实际目录：" + root);
        }
        List<Fixture> result = new ArrayList<>();
        try (Stream<Path> stream = Files.list(root)) {
            List<Path> files = stream.filter(Files::isRegularFile).collect(Collectors.toList());
            for (int i = 0; i < CODES.size(); i++) {
                String base = "图片" + (i + 1);
                List<Path> candidates = files.stream().filter(path -> path.getFileName().toString()
                        .matches(java.util.regex.Pattern.quote(base) + "(?i:\\.(png|jpg|jpeg))")).collect(Collectors.toList());
                if (candidates.size() != 1) throw new IllegalStateException("照片必须唯一且完整：" + root.resolve(base) + ".png/.jpg/.jpeg");
                Path file = candidates.get(0);
                if (!Files.isReadable(file) || Files.size(file) == 0 || Files.size(file) > CqDockApiService.MAX_DOWNLOAD_BYTES
                        || !file.toRealPath().startsWith(root.toRealPath())) throw new IOException("Unreadable photo: " + file);
                // Validate before registering a task; do not rewrite or watermark the source.
                try (javax.imageio.stream.ImageInputStream input = ImageIO.createImageInputStream(file.toFile())) {
                    Iterator<javax.imageio.ImageReader> readers = ImageIO.getImageReaders(input);
                    if (!readers.hasNext()) throw new IOException("Invalid photo: " + file);
                    javax.imageio.ImageReader reader = readers.next();
                    try {
                        reader.setInput(input, true, true);
                        int width = reader.getWidth(0), height = reader.getHeight(0);
                        if (width <= 0 || height <= 0 || (long) width * height > 200_000_000L) throw new IOException("Invalid dimensions: " + file);
                        javax.imageio.ImageReadParam param = reader.getDefaultReadParam();
                        int sample = Math.max(1, (int) Math.ceil(Math.max(width, height) / 1024.0));
                        param.setSourceSubsampling(sample, sample, 0, 0);
                        final boolean[] warning = {false};
                        reader.addIIOReadWarningListener((source, message) -> warning[0] = true);
                        java.awt.image.BufferedImage image = reader.read(0, param);
                        if (image == null || warning[0]) throw new IOException("Incomplete photo: " + file);
                        image.flush();
                    } finally { reader.dispose(); }
                }
                Fixture fixture = new Fixture(); fixture.file = file; fixture.code = CODES.get(i); fixture.index = i;
                result.add(fixture);
            }
        } catch (IOException e) {
            throw new IllegalStateException("无法读取八张场景照片，请检查目录、格式及权限：" + root, e);
        }
        List<UniPoint> points = pointMapper.selectList(new LambdaQueryWrapper<UniPoint>()
                .eq(UniPoint::getSubCode, STATION).eq(UniPoint::getWaylineId, ROUTE).in(UniPoint::getPointCode, CODES));
        for (Fixture fixture : result) {
            List<UniPoint> matches = points.stream().filter(p -> fixture.code.equals(p.getPointCode())
                    && STATION.equals(p.getSubCode()) && ROUTE.equals(p.getWaylineId())).collect(Collectors.toList());
            if (matches.size() != 1 || matches.get(0).getPointName() == null || matches.get(0).getPointName().trim().isEmpty()) {
                throw new IllegalStateException("场景点位不存在、重复或名称为空，请检查站点和航线：" + fixture.code);
            }
            fixture.name = matches.get(0).getPointName();
        }
        return result;
    }

    public boolean ownsTask(String task) {
        if (task == null || !task.startsWith(TASK_PREFIX)) return false;
        if (!enabled() || !tasks.containsKey(task)) throw unavailable();
        return true;
    }

    public JSONArray pictures(String task) {
        if (!ownsTask(task)) return null;
        JSONArray result = new JSONArray();
        for (Fixture fixture : tasks.get(task)) {
            JSONObject picture = new JSONObject();
            picture.put("id", fixture.id); picture.put("pictureName", fixture.file.getFileName().toString());
            picture.put("pointId", fixture.code); picture.put("pointName", fixture.name);
            picture.put("photoStorage", fixture.index < 4 ? "1" : "2");
            picture.put("shootTime", "2026-11-15 09:54:00");
            result.add(picture);
        }
        return result;
    }

    public boolean ownsPicture(String id) {
        if (id == null || !id.startsWith(PICTURE_PREFIX)) return false;
        Fixture fixture = pictures.get(id);
        if (fixture == null || !ownsTask(fixture.task)) throw unavailable();
        return true;
    }

    public String download(String id, OutputStream output) throws IOException {
        if (!ownsPicture(id)) throw new IllegalArgumentException("Unknown scenario picture");
        Fixture fixture = pictures.get(id);
        Path root = Paths.get(properties.getMockScenario().getSourcePath()).toRealPath();
        if (!Files.isRegularFile(fixture.file) || !Files.isReadable(fixture.file)
                || !fixture.file.toRealPath().startsWith(root) || Files.size(fixture.file) == 0
                || Files.size(fixture.file) > CqDockApiService.MAX_DOWNLOAD_BYTES) {
            throw new IOException("八图素材不存在、不可读或超出配置目录：" + fixture.file);
        }
        Files.copy(fixture.file, output);
        log.info("[cq-dock] 八图素材按原字节复制，不转码：pointCode={}, file={}", fixture.code, fixture.file);
        return fixture.file.getFileName().toString();
    }

    private IllegalStateException unavailable() {
        return new IllegalStateException("本地场景状态不可用或开关已关闭；重启后请重新下发任务");
    }

    private static class Fixture {
        String task;
        String id;
        String code;
        String name;
        Path file;
        int index;
    }
}
