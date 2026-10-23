package com.dji.sample.df.constructionDf;

import com.dji.sample.common.model.CustomClaim;
import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/construction/areas/{area}/waylines")
public class ConstructionWaylineController {
    private final ConstructionWaylineService service;
    public ConstructionWaylineController(ConstructionWaylineService service) { this.service=service; }
    private interface Call { Object run(CustomClaim c) throws Exception; }
    private HttpResultResponse<Object> run(HttpServletRequest req,Call call) throws Exception {
        CustomClaim c=(CustomClaim)req.getAttribute(TOKEN_CLAIM);
        if(c==null||c.getWorkspaceId()==null) return HttpResultResponse.error(401,"请先登录工作空间");
        try { return HttpResultResponse.success(call.run(c)); }
        catch(ConstructionAreaGeometry.RuleException e) { return HttpResultResponse.error(e.getCode(),e.getMessage()); }
    }
    @GetMapping public HttpResultResponse<Object> list(HttpServletRequest req,@PathVariable String area) throws Exception { return run(req,c->service.list(c.getWorkspaceId(),area)); }
    @GetMapping("/{id}") public HttpResultResponse<Object> detail(HttpServletRequest req,@PathVariable String area,@PathVariable String id) throws Exception { return run(req,c->service.detail(c.getWorkspaceId(),area,id)); }
    @PostMapping public HttpResultResponse<Object> create(HttpServletRequest req,@PathVariable String area,@RequestBody Map<String,Object> body) throws Exception { return run(req,c->service.create(c.getWorkspaceId(),area,body,c.getUsername())); }
    @PostMapping("/upload") public HttpResultResponse<Object> upload(HttpServletRequest req,@PathVariable String area,@RequestParam("expected_version") int version,@RequestParam("request_id") String request,@RequestParam("file") MultipartFile file) throws Exception {
        return run(req,c->{
            String name=file.getOriginalFilename();
            if(name==null||!name.toLowerCase(java.util.Locale.ROOT).endsWith(".kmz")) throw new ConstructionAreaGeometry.RuleException(400,"请选择 KMZ 文件");
            byte[] bytes;try(java.io.InputStream in=file.getInputStream()) { bytes=ConstructionKmz.readBounded(in); }
            return service.upload(c.getWorkspaceId(),area,version,request,name.substring(0,name.length()-4),bytes,c.getUsername());
        });
    }
    @PostMapping("/{id}/validate") public HttpResultResponse<Object> validate(HttpServletRequest req,@PathVariable String area,@PathVariable String id) throws Exception { return run(req,c->service.validate(c.getWorkspaceId(),area,id)); }
    @DeleteMapping("/{id}") public HttpResultResponse<Object> unlink(HttpServletRequest req,@PathVariable String area,@PathVariable String id) throws Exception { return run(req,c->{ service.unlink(c.getWorkspaceId(),area,id);return null; }); }
}
