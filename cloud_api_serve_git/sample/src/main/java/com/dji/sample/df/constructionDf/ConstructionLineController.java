package com.dji.sample.df.constructionDf;

import com.dji.sample.common.model.CustomClaim;
import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/construction/lines")
public class ConstructionLineController {
    private final ConstructionLineService service;
    public ConstructionLineController(ConstructionLineService service) { this.service = service; }
    private String workspace(HttpServletRequest request) {
        CustomClaim claim = (CustomClaim) request.getAttribute(TOKEN_CLAIM);
        if (claim == null || claim.getWorkspaceId() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录工作空间");
        return claim.getWorkspaceId();
    }
    private HttpResultResponse<Object> guarded(Supplier<Object> call) {
        try { return HttpResultResponse.success(call.get()); }
        catch (ConstructionAreaGeometry.RuleException e) { return HttpResultResponse.error(e.getCode(), e.getMessage()); }
    }
    @GetMapping
    public HttpResultResponse<Object> list(HttpServletRequest request, @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled, @RequestParam(defaultValue = "false") boolean archived) {
        return guarded(() -> service.list(workspace(request), keyword, enabled, archived));
    }
    @GetMapping("/{id}")
    public HttpResultResponse<Object> detail(HttpServletRequest request, @PathVariable String id) { return guarded(() -> service.detail(workspace(request), id)); }
    @GetMapping("/{id}/versions")
    public HttpResultResponse<Object> history(HttpServletRequest request, @PathVariable String id) { return guarded(() -> service.history(workspace(request), id)); }
    @PostMapping
    public HttpResultResponse<Object> create(HttpServletRequest request, @RequestBody Map<String, Object> body) { return guarded(() -> service.create(workspace(request), body)); }
    @PutMapping("/{id}")
    public HttpResultResponse<Object> update(HttpServletRequest request, @PathVariable String id, @RequestBody Map<String, Object> body) { return guarded(() -> service.update(workspace(request), id, body)); }
    @PostMapping("/{id}/archive")
    public HttpResultResponse<Object> archive(HttpServletRequest request, @PathVariable String id, @RequestBody Map<String, Object> body) { return guarded(() -> service.archive(workspace(request), id, body)); }
}
