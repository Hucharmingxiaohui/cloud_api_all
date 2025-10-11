package com.dji.sample.df.kmzDf.control;


import com.dji.sample.df.kmzDf.entity.wayline.Wayline;
import com.dji.sample.df.kmzDf.service.WaylineKmzService;
import com.dji.sample.wayline.service.IWaylineFileService;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.URL;


@RequestMapping("${url.wayline.prefix}${url.wayline.version}/workspaces")
@RestController
public class WaylineKmzControl {
    @Autowired
    private WaylineKmzService waylineKmzService;//kmz服务
    @Autowired
    private IWaylineFileService waylineFileService;

    //创建航点航线

    /**
     * Delete the favorites of this wayline file based on the wayline file id.
     *
     * @param workspaceId 工作空间id
     * @param wayline     航点航线对象
     * @param creator     创建人
     * @return
     */
    @PostMapping("/{workspace_id}/waylines/kmzWayPointRoutePlanning")//创建航点航线
    public HttpResultResponse<?> wayPointRoutePlanning(@PathVariable(name = "workspace_id") String workspaceId,
                                                       @RequestBody Wayline wayline,
                                                       @RequestParam String creator
    ) throws IOException {
        String resultMsg = waylineKmzService.CreateWaypointWaylineKmz(workspaceId, wayline, creator);

        return HttpResultResponse.success().setMessage(resultMsg);
    }

    //对航点飞行文件加锁，只让一个用户调用
    private final Object lock = new Object();
    //获取文件夹路径
    @Value("${singlePointUrl}")
    private String singlePointUrl;

    @GetMapping("/{workspace_id}/waylines/{wayline_id}/getKmzWaypointWayLineInfo")//解析航点航线
    public HttpResultResponse<?> getKmzWaypointWayLineInfo(@PathVariable(name = "workspace_id") String workspaceId,
                                                           @PathVariable(name = "wayline_id") String waylineId) throws Exception {
        //对航点飞行文件加锁，只让一个用户调用
        synchronized (lock) {
            //获取下载路径
            URL url = waylineFileService.getObjectUrl(workspaceId, waylineId);
            Wayline wayline = waylineKmzService.GetKmzWaypointWayline(workspaceId, waylineId, url, singlePointUrl);
            return HttpResultResponse.success(wayline);
        }
    }
}
