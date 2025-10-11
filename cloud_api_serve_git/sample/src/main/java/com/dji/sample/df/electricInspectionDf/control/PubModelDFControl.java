package com.dji.sample.df.electricInspectionDf.control;


import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.df.electricInspectionDf.model.PubModelDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubModelDfService;
import com.dji.sdk.common.HttpResultResponse;
import com.dji.sdk.common.PaginationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;

/**
 * 倾斜摄影模型接口
 */
@RestController
@RequestMapping("pub/api/v1/")
public class PubModelDFControl {
    @Autowired
    PubModelDfService importModelService;

    // 提交模型
    @PostMapping("/importModel/workspaces/{workspace_id}/file/upload")
    public HttpResultResponse importModelFile(HttpServletRequest request,
                                              @RequestParam("file") MultipartFile file,
                                              @RequestParam("file_path") String filePath,
                                              @RequestParam("isLastFile") String isLastFile,
                                              @RequestParam("json_name") String jsonName) {
        if (Objects.isNull(file)) {
            return HttpResultResponse.error("提交文件为空");
        }
        CustomClaim customClaim = (CustomClaim) request.getAttribute(TOKEN_CLAIM);
        String workspaceId = customClaim.getWorkspaceId();
        String creator = customClaim.getUsername();
        boolean result =  importModelService.importModelFile(file, workspaceId, filePath, creator);
        System.out.println("Import result: " + result);

        String subCode = "";
        String subName = "";
//        Integer insertResult =  importModelService.saveModelFile(workspaceId, filePath,subCode,subName);
        if(result && "true".equals(isLastFile)) {
            try{
                Integer insertResult =  importModelService.saveModelFile(workspaceId, filePath,subCode,subName,jsonName);
                return HttpResultResponse.success().setMessage("模型信息插入数据库成功");
            }
            catch (Exception e){
                return HttpResultResponse.error("未知错误: " + e.getMessage());
            }
        }
        return HttpResultResponse.success().setMessage("提交模型文件成功");
    }

    // 按模型名称查询
    @GetMapping("/modelName/{modelName}/getModelInfo")
    public HttpResultResponse<?> getWayLineInfo(@PathVariable(name = "modelName") String modelName){
        try {
            // 调用服务查询模型信息
            List<PubModelDfEntity> result = importModelService.queryModelByName(modelName);
            if (result == null || result.isEmpty()) {
                result = new ArrayList<>(); // 创建空的列表
            }

            // 如果 result 不为 null，返回成功响应
            return HttpResultResponse.success(result).setMessage("查询成功！");
        } catch (Exception e) {
            // 捕获异常并返回错误响应
            return HttpResultResponse.error("未知错误: " + e.getMessage());
        }
    }

    //查询所有模型记录
    @GetMapping("/models/all")
    public HttpResultResponse<PaginationData<PubModelDfEntity>> getAllPubModelsDfEntities(@RequestParam(defaultValue = "1") Long page,
                                                                                                       @RequestParam(name = "page_size", defaultValue = "10") Long pageSize) {
        PaginationData<PubModelDfEntity> data = importModelService.getAllPubModelDfEntities(page, pageSize);
        return HttpResultResponse.success(data).setMessage("成功查询到点位信息");
    }

    // 删除模型信息
    @DeleteMapping("workspaces/{workspace_id}/deleteModelById")
    public HttpResultResponse deletePubModelEntityById( @PathVariable(name = "workspace_id") String workspaceId ,@RequestParam("id") Integer id) {
        boolean result = importModelService.deletePubModelDfEntityById(workspaceId, id);
        if (result) {
            return HttpResultResponse.success("删除成功");
        } else {
            return HttpResultResponse.error("删除失败");
        }
    }
}
