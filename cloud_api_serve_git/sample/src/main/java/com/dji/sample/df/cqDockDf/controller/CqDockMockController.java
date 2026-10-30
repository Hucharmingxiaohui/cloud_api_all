package com.dji.sample.df.cqDockDf.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dji.sample.component.AuthInterceptor;
import com.dji.sample.df.cqDockDf.model.dto.CqAssignTaskRequest;
import com.dji.sample.df.cqDockDf.model.dto.CqTaskIdRequest;
import com.dji.sample.df.cqDockDf.config.CqDockProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Opt-in EUA simulator with fixed route fixtures and local task/transfer state. Never dispatches. */
@RestController
@AuthInterceptor.IgnoreAuth
@ConditionalOnProperty(name = "cq-dock.mock-enabled", havingValue = "true")
@RequestMapping("/machineNest/noauth/third")
public class CqDockMockController {
    private static final long RUNNING_DURATION_MS = 15_000L;
    private static final String FIXTURE_ROUTE_ID = "bfd088e2-2252-41b4-8191-ad49fe1e08a6";
    private static final String[] FIXTURE_CODES = {
            "YIXIA-15026048", "YIXIA-84256716", "YIXIA-25246010", "YIXIA-16345084"
    };
    private final Map<String, Scenario> scenarios = new ConcurrentHashMap<>();
    @Autowired
    private CqDockProperties properties;

    private static class Scenario {
        volatile String taskStatus;
        String routeId;
        List<Object> transferStatuses;
        JSONArray pictures;
        int index;
        long advanceAt;
        long assignedAt;
        volatile long completeAt;
        synchronized JSONObject pictures(String taskId) {
            JSONObject data = new JSONObject(); data.put("taskId", taskId);
            data.put("status", transferStatuses.get(index));
            if (index < transferStatuses.size() - 1) index++;
            data.put("pictureList", pictures); return data;
        }
    }

    /** Set exact statuses (including malformed values) for repeatable fail-closed testing. */
    @PostMapping("/mock/scenario")
    public JSONObject scenario(@RequestBody JSONObject request) {
        String id = request.getString("taskId");
        JSONArray statuses = request.getJSONArray("transferStatuses");
        if (id == null || id.trim().isEmpty() || statuses == null || statuses.isEmpty()
                || !(request.get("pictureList") instanceof List)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "taskId, transferStatuses, pictureList required");
        }
        Scenario scenario = new Scenario(); scenario.taskStatus = request.getString("taskStatus");
        scenario.transferStatuses = new ArrayList<>(statuses); scenario.pictures = request.getJSONArray("pictureList");
        // Timer simulation is opt-in per scenario and advances transfer only; taskStatus is unchanged.
        Long delay = request.getLong("autoAdvanceMs");
        if (delay != null && delay > 0) scenario.advanceAt = System.currentTimeMillis() + delay;
        scenarios.put(id, scenario);
        JSONObject data = new JSONObject(); data.put("taskId", id); return success(data);
    }

    @PostMapping("/task/assign_task")
    public JSONObject assignTask(@RequestBody CqAssignTaskRequest request) {
        String id = "mock-task-" + UUID.randomUUID();
        Scenario scenario = new Scenario();
        scenario.taskStatus = "2";
        scenario.routeId = request.getRouteId();
        scenario.assignedAt = System.currentTimeMillis();
        scenario.completeAt = scenario.assignedAt + RUNNING_DURATION_MS;
        scenario.transferStatuses = Arrays.asList("1", "1", "2");
        scenario.pictures = new JSONArray();
        scenarios.put(id, scenario);
        JSONObject data = new JSONObject(); data.put("taskId", id); return success(data);
    }

    @PostMapping("/task/status")
    public JSONObject taskStatus(@RequestBody CqTaskIdRequest request) {
        Scenario scenario = scenarios.get(request.getTaskId());
        if (scenario == null) return missing();
        JSONObject data = new JSONObject(); data.put("taskId", request.getTaskId());
        data.put("status", scenario.completeAt > 0 && System.currentTimeMillis() >= scenario.completeAt
                ? "1" : scenario.taskStatus); return success(data);
    }

    @PostMapping("/picture/list")
    public JSONObject pictureList(@RequestBody CqTaskIdRequest request) {
        Scenario scenario = scenarios.get(request.getTaskId());
        if (scenario == null) return missing();
        synchronized (scenario) {
            JSONObject data = scenario.pictures(request.getTaskId());
            if (scenario.completeAt > 0) data.put("pictureList", buildMockPictures(scenario.routeId));
            return success(data);
        }
    }

    private JSONArray buildMockPictures(String routeId) {
        JSONArray pictures = new JSONArray();
        if (!FIXTURE_ROUTE_ID.equals(routeId)) return pictures;
        String baseUrl = RequestContextHolder.getRequestAttributes() != null
                ? ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString() : properties.getBaseUrl();
        for (int i = 0; i < 4; i++) {
            boolean isVisible = i < 2;
            String name = (isVisible ? "visible" : "ir") + (i % 2 + 1) + ".jpg";
            JSONObject picture = new JSONObject();
            picture.put("id", "mock-pic-" + (i + 1));
            picture.put("pictureUrl", UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path("/machineNest/noauth/third/mock/picture/").pathSegment(name).build().toUriString());
            picture.put("pictureName", name); picture.put("pointId", FIXTURE_CODES[i]);
            picture.put("pointName", "主变区-#1主变-变压器-部件" + (i % 2 + 1));
            picture.put("photoStorage", isVisible ? "1" : "2");
            pictures.add(picture);
        }
        return pictures;
    }

    @GetMapping(value = "/mock/picture/{fileName:.+}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> mockPicture(@PathVariable String fileName) throws IOException {
        Path configured = Paths.get(properties.getMockPicturePath());
        if (!Files.isDirectory(configured)) return ResponseEntity.notFound().build();
        Path root = configured.toRealPath();
        Path file = root.resolve(fileName).normalize();
        if (!file.startsWith(root)) return ResponseEntity.notFound().build();
        if (!Files.isRegularFile(file)) {
            // Preserve the original downloaded-name lookup, restricted to this image directory.
            try (java.util.stream.Stream<Path> files = Files.list(root)) {
                String fragment = fileName.toLowerCase(Locale.ROOT).replace(".jpg", "");
                file = files.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).contains(fragment))
                        .sorted().findFirst().orElse(file);
            }
        }
        if (!file.startsWith(root) || !Files.isRegularFile(file) || !file.toRealPath().startsWith(root)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(Files.readAllBytes(file));
    }

    @Scheduled(fixedDelay = 1000)
    public void advanceSimulation() {
        for (Scenario scenario : scenarios.values()) {
            synchronized (scenario) {
                if (scenario.advanceAt > 0 && System.currentTimeMillis() >= scenario.advanceAt) {
                    scenario.transferStatuses = Collections.singletonList("2");
                    scenario.index = 0; scenario.advanceAt = 0;
                }
            }
        }
    }

    private JSONObject success(JSONObject data) {
        JSONObject result = new JSONObject(); result.put("code", 200); result.put("success", true);
        result.put("msg", "success");
        result.put("data", data); return result;
    }

    private JSONObject missing() {
        JSONObject result = new JSONObject(); result.put("code", 404); result.put("success", false);
        result.put("data", null);
        result.put("msg", "Unknown mock task; configure /mock/scenario first"); return result;
    }
}
