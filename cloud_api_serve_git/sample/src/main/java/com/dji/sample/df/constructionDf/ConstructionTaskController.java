package com.dji.sample.df.constructionDf;

import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;

@RestController
@RequestMapping("/api/construction/tasks/{jobId}")
public class ConstructionTaskController {
    private final ConstructionTaskService service;
    @org.springframework.beans.factory.annotation.Autowired private ConstructionTransferService transfers;
    public ConstructionTaskController(ConstructionTaskService service) { this.service = service; }
    private WaylineJobEntity job(HttpServletRequest request, String jobId) {
        CustomClaim claim = (CustomClaim) request.getAttribute(TOKEN_CLAIM);
        if (claim == null || claim.getWorkspaceId() == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录工作空间");
        return service.requireJob(claim.getWorkspaceId(), jobId);
    }
    @GetMapping
    public HttpResultResponse detail(HttpServletRequest request, @PathVariable String jobId) throws Exception {
        return HttpResultResponse.success(service.detail(job(request, jobId)));
    }
    @PostMapping("/archive")
    public HttpResultResponse archive(HttpServletRequest request, @PathVariable String jobId) throws Exception {
        WaylineJobEntity job = job(request, jobId); service.archive(job);
        return HttpResultResponse.success(service.detail(job));
    }
    @PostMapping("/export")
    public HttpResultResponse export(HttpServletRequest request, @PathVariable String jobId) throws Exception {
        java.nio.file.Path path=service.exportTransfer(job(request,jobId),transfers);
        return HttpResultResponse.success(Map.of("path",path.toString(),"name",path.getFileName().toString()));
    }
    @PostMapping("/photo-associations")
    public HttpResultResponse assign(HttpServletRequest request,@PathVariable String jobId,@RequestBody Map<String,Object> body) throws Exception {
        WaylineJobEntity job=job(request,jobId);CustomClaim claim=(CustomClaim)request.getAttribute(TOKEN_CLAIM);
        try { service.assignPhotos(job,body,claim.getUsername()); }
        catch(ConstructionAreaGeometry.RuleException e){ return HttpResultResponse.error(e.getCode(),e.getMessage()); }
        return HttpResultResponse.success(service.detail(job));
    }
    @GetMapping("/media/{fileId}")
    public ResponseEntity<FileSystemResource> photo(HttpServletRequest request, @PathVariable String jobId, @PathVariable String fileId) throws Exception {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(service.photo(job(request, jobId), fileId)));
    }
    @PostMapping("/analysis/check")
    public HttpResultResponse check(HttpServletRequest request, @PathVariable String jobId) throws Exception {
        job(request, jobId);
        return HttpResultResponse.success(Map.of("connected", service.checkConnection()));
    }
    @PostMapping("/analysis")
    public HttpResultResponse submit(HttpServletRequest request, @PathVariable String jobId, @RequestBody Map<String, String> body) throws Exception {
        return HttpResultResponse.success(service.submit(job(request, jobId), body.get("request_id")));
    }
    @PostMapping("/analysis/refresh")
    public HttpResultResponse refresh(HttpServletRequest request, @PathVariable String jobId) throws Exception {
        return HttpResultResponse.success(service.refresh(job(request, jobId)));
    }
}
