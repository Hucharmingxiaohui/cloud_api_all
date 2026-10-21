package com.dji.sample.df.constructionDf;

import com.dji.sample.common.model.CustomClaim;
import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/** Construction area endpoints. Version conflicts return code 409 in a normal body, not HTTP 409. */
@RestController
@RequestMapping("/api/construction/areas")
public class ConstructionAreaController {
    private final ConstructionAreaService service;

    public ConstructionAreaController(ConstructionAreaService service) {
        this.service = service;
    }

    private String workspace(HttpServletRequest request) {
        CustomClaim claim = (CustomClaim) request.getAttribute(TOKEN_CLAIM);
        if (claim == null || claim.getWorkspaceId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录工作空间");
        }
        return claim.getWorkspaceId();
    }

    private HttpResultResponse<Object> guarded(HttpServletRequest request, RuleCall call) {
        try {
            return HttpResultResponse.success(call.run());
        } catch (ConstructionAreaGeometry.RuleException e) {
            return HttpResultResponse.error(e.getCode(), e.getMessage());
        }
    }

    private interface RuleCall {
        Object run();
    }

    @GetMapping
    public HttpResultResponse<Object> list(HttpServletRequest request,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Boolean enabled,
                                           @RequestParam(required = false, defaultValue = "false") Boolean archived) {
        return guarded(request, () -> service.list(workspace(request), keyword, enabled, archived));
    }

    @GetMapping("/{id}")
    public HttpResultResponse<Object> detail(HttpServletRequest request, @PathVariable String id) {
        return guarded(request, () -> service.detail(workspace(request), id));
    }

    @PostMapping
    public HttpResultResponse<Object> create(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return guarded(request, () -> service.create(workspace(request), body));
    }

    @PutMapping("/{id}")
    public HttpResultResponse<Object> update(HttpServletRequest request, @PathVariable String id, @RequestBody Map<String, Object> body) {
        return guarded(request, () -> service.update(workspace(request), id, body));
    }

    @GetMapping("/{id}/versions")
    public HttpResultResponse<Object> history(HttpServletRequest request, @PathVariable String id) {
        return guarded(request, () -> service.history(workspace(request), id));
    }

    @PostMapping("/{id}/archive")
    public HttpResultResponse<Object> archive(HttpServletRequest request, @PathVariable String id,
                                              @RequestParam(required = false, defaultValue = "true") boolean archived) {
        return guarded(request, () -> {
            service.archive(workspace(request), id, archived);
            return null;
        });
    }
}
