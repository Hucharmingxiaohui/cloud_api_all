package com.dji.sample.df.importKmzNoValiDf.control;

import com.df.server.dto.JobPlan.JobPlanItemPointDTO;
import com.df.server.mapper.uni.UniPointMapper;
import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.df.importKmzNoValiDf.service.ImportKmzNoValiService;
import com.dji.sample.wayline.model.entity.WaylineFileEntity;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;

@RestController
@RequestMapping("importNoValiKmz")
public class ImportKmzNoValiControl {
    @Autowired
    ImportKmzNoValiService importKmzNoValiService;
    @Autowired
    UniPointMapper uniPointMapper;
//    @RequestParam Integer waylinePos
    @PostMapping("/workspaces/{workspace_id}/waylines/file/upload")
    public HttpResultResponse importKmzFile(HttpServletRequest request, MultipartFile file) {
        if (Objects.isNull(file)) {
            return HttpResultResponse.error("No file received.");
        }
        CustomClaim customClaim = (CustomClaim)request.getAttribute(TOKEN_CLAIM);
        String workspaceId = customClaim.getWorkspaceId();
        String creator = customClaim.getUsername();
        importKmzNoValiService.importKmzFile(file, workspaceId, creator, null);
        String fileName = file.getOriginalFilename();
        if (fileName != null && fileName.endsWith(".kmz")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }
        WaylineFileEntity entity = importKmzNoValiService.getWaylineByFileName(fileName);
        if(entity!=null){
            return HttpResultResponse.success(entity).setMessage("导入外部航线到数据库成功");
        }else{
            return HttpResultResponse.error("导入外部航线失败");
        }
    }

    @PostMapping("/workspaces/choseWaylinePos")
    public HttpResultResponse choseWaylinePos() {
        List<JobPlanItemPointDTO> jobPlanItemPointDTOS = uniPointMapper.choseWaylinePos();
        List<Integer> list = new ArrayList<>();
        for (JobPlanItemPointDTO jobPlanItemPointDTO : jobPlanItemPointDTOS) {
            list.add(jobPlanItemPointDTO.getWaylinePos());
        }
        return HttpResultResponse.success(list).setMessage("查询成功");
    }

}
