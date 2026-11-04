package com.dji.sample.df.cqDockDf.controller;

import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.df.cqDockDf.model.dto.CqDockHmsView;
import com.dji.sample.df.cqDockDf.model.dto.CqDockMonitoringView;
import com.dji.sample.df.cqDockDf.model.dto.CqDockReadPage;
import com.dji.sample.df.cqDockDf.model.dto.CqDockReadQuery;
import com.dji.sample.df.cqDockDf.service.CqDockReadService;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;

@RestController
@RequestMapping("/api/cqDock")
public class CqDockReadController {
    private static final Set<String> QUERY_NAMES = Set.of("current", "size", "unitCode", "dockSn", "droneSn",
            "keyword", "startTime", "endTime", "level", "gatewayType", "module");
    private static final DateTimeFormatter QUERY_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss")
            .withResolverStyle(ResolverStyle.STRICT);

    private final CqDockReadService service;
    private final Environment environment;

    public CqDockReadController(CqDockReadService service, Environment environment) {
        this.service = service;
        this.environment = environment;
    }

    @GetMapping("/monitoring/status")
    public Map<String, Object> status(HttpServletRequest request) {
        requireAuthenticated(request);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", environment.getProperty("mqtt.CUSTOM.enabled", Boolean.class, true));
        result.put("server_time", Instant.now().toString());
        return result;
    }

    @GetMapping("/monitoring/page")
    public CqDockReadPage<CqDockMonitoringView> monitoringPage(
            HttpServletRequest request, @RequestParam Map<String, String> params) {
        requireAuthenticated(request);
        CqDockReadQuery query = parse(params);
        return service.monitoringPage(query);
    }

    @GetMapping("/hms/page")
    public CqDockReadPage<CqDockHmsView> hmsPage(
            HttpServletRequest request, @RequestParam Map<String, String> params) {
        requireAuthenticated(request);
        CqDockReadQuery query = parse(params);
        return service.hmsPage(query);
    }

    private static void requireAuthenticated(HttpServletRequest request) {
        Object value = request.getAttribute(TOKEN_CLAIM);
        if (!(value instanceof CustomClaim)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        String id = ((CustomClaim) value).getId();
        if (id == null || id.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated account id required");
        }
    }

    private static CqDockReadQuery parse(Map<String, String> params) {
        if (params.keySet().stream().anyMatch(name -> !QUERY_NAMES.contains(name))) {
            throw new IllegalArgumentException("Unknown query parameter");
        }
        CqDockReadQuery query = new CqDockReadQuery();
        if (params.containsKey("current")) query.setCurrent(longValue(params.get("current"), "current"));
        if (params.containsKey("size")) query.setSize(longValue(params.get("size"), "size"));
        query.setUnitCode(params.get("unitCode"));
        query.setDockSn(params.get("dockSn"));
        query.setDroneSn(params.get("droneSn"));
        query.setKeyword(params.get("keyword"));
        query.setGatewayType(params.get("gatewayType"));
        if (params.containsKey("level")) query.setLevel(integer(params.get("level"), "level"));
        if (params.containsKey("module")) query.setModule(integer(params.get("module"), "module"));
        if (params.containsKey("startTime")) query.setStartTime(dateTime(params.get("startTime"), "startTime"));
        if (params.containsKey("endTime")) query.setEndTime(dateTime(params.get("endTime"), "endTime"));
        return query;
    }

    private static long longValue(String value, String name) {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException(name + " must be a number");
        try { return Long.parseLong(value); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(name + " must be a number", e); }
    }

    private static int integer(String value, String name) {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException(name + " must be an integer");
        try { return Integer.parseInt(value); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(name + " must be an integer", e); }
    }

    private static LocalDateTime dateTime(String value, String name) {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException(name + " must be ISO local date-time");
        try { return LocalDateTime.parse(value, QUERY_TIME); }
        catch (DateTimeParseException e) { throw new IllegalArgumentException(name + " must be ISO local date-time", e); }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> invalid(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() == null ? "Invalid request" : e.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> unauthorized(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getReason() == null ? "Request rejected" : e.getReason()));
    }
}
