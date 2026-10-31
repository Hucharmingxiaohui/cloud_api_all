package com.dji.sample.df.constructionAnalysisDf;

import com.dji.sample.common.model.CustomClaim;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.util.*;
import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;

@RestController
@RequestMapping("/api/construction-analysis")
public class AnalysisController {
    private final AnalysisService service;
    public AnalysisController(AnalysisService service){this.service=service;}
    private String workspace(HttpServletRequest request) {
        Object value=request.getAttribute(TOKEN_CLAIM);
        if(!(value instanceof CustomClaim)||((CustomClaim)value).getWorkspaceId()==null)
            throw AnalysisStore.error(HttpStatus.UNAUTHORIZED,"Login to a workspace first");
        return AnalysisStore.id(((CustomClaim)value).getWorkspaceId());
    }
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public static class PhotosRequest {public List<Map<String,Object>> photos;}
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public static class BatchRequest {public String requestId;}
    @GetMapping("/health") public Map<String,Object> health(HttpServletRequest r){workspace(r);return service.health();}
    @GetMapping("/tasks/{taskId}") public Map<String,Object> task(HttpServletRequest r,@PathVariable String taskId)throws Exception {return service.task(workspace(r),taskId);}
    @PutMapping("/tasks/{taskId}/photos") public Map<String,Object> save(HttpServletRequest r,@PathVariable String taskId,@RequestBody PhotosRequest body)throws Exception {return service.savePhotos(workspace(r),taskId,body.photos);}
    @PostMapping("/tasks/{taskId}/batches") public Map<String,Object> submit(HttpServletRequest r,@PathVariable String taskId,@RequestBody BatchRequest body)throws Exception {return service.submit(workspace(r),taskId,body.requestId);}
    @GetMapping("/tasks/{taskId}/batches") public List<Map<String,Object>> batches(HttpServletRequest r,@PathVariable String taskId)throws Exception {return service.batches(workspace(r),taskId);}
    @GetMapping("/tasks/{taskId}/batches/{batchId}") public Map<String,Object> batch(HttpServletRequest r,@PathVariable String taskId,@PathVariable String batchId)throws Exception {return service.refresh(workspace(r),taskId,batchId);}
    @GetMapping("/tasks/{taskId}/photos/{imageId}/original") public ResponseEntity<FileSystemResource> original(HttpServletRequest r,@PathVariable String taskId,@PathVariable String imageId)throws Exception {
        Path p=service.original(workspace(r),taskId,imageId);String ext=p.getFileName().toString().toLowerCase(Locale.ROOT);
        MediaType type=ext.endsWith(".jpg")||ext.endsWith(".jpeg")?MediaType.IMAGE_JPEG:ext.endsWith(".png")?MediaType.IMAGE_PNG:MediaType.APPLICATION_OCTET_STREAM;
        return file(p,type,false);
    }
    @GetMapping("/tasks/{taskId}/batches/{batchId}/photos/{imageId}/result") public ResponseEntity<FileSystemResource> result(HttpServletRequest r,@PathVariable String taskId,@PathVariable String batchId,@PathVariable String imageId)throws Exception {return file(service.result(workspace(r),taskId,batchId,imageId),MediaType.IMAGE_JPEG,false);}
    @PostMapping("/tasks/{taskId}/batches/{batchId}/report") public Map<String,Object> generate(HttpServletRequest r,@PathVariable String taskId,@PathVariable String batchId)throws Exception {return service.generateReport(workspace(r),taskId,batchId);}
    @GetMapping("/tasks/{taskId}/batches/{batchId}/report") public ResponseEntity<FileSystemResource> report(HttpServletRequest r,@PathVariable String taskId,@PathVariable String batchId)throws Exception {return file(service.report(workspace(r),taskId,batchId),MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),true);}
    private ResponseEntity<FileSystemResource> file(Path p,MediaType type,boolean download) {
        return ResponseEntity.ok().contentType(type).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION,(download?"attachment":"inline")+"; filename=\""+p.getFileName()+"\"").body(new FileSystemResource(p));
    }
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Map<String,Object>> status(ResponseStatusException e){return ResponseEntity.status(e.getStatus()).body(Map.of("message",e.getReason()==null?"Request failed":e.getReason()));}
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class) public ResponseEntity<Map<String,Object>> malformed(Exception e){return ResponseEntity.badRequest().body(Map.of("message","Invalid JSON request body"));}
    @ExceptionHandler(Exception.class) public ResponseEntity<Map<String,Object>> failure(Exception e){return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message","Analysis storage or metadata operation failed"));}
}
