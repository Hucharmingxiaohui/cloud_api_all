package com.dji.sample.df.thirdKmzDf.control;


import com.dji.sample.df.electricInspectionDf.model.PubWaylinePointDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubWaylinePointDfService;
import com.dji.sample.df.thirdKmzDf.entity.thirdPoints.ThirdPointEntity;
import com.dji.sample.df.thirdKmzDf.entity.thirdPoints.thirdActions.ThirdAction;
import com.dji.sample.df.thirdKmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.Action;
import com.dji.sample.df.thirdKmzDf.entity.wayline.Folder.Placemark.ActionGroup.ActionGroup;
import com.dji.sample.df.thirdKmzDf.entity.wayline.Folder.Placemark.Placemark;
import com.dji.sample.df.thirdKmzDf.entity.wayline.Wayline;
import com.dji.sample.df.thirdKmzDf.service.WaylineKmzThirdService;
import com.dji.sample.wayline.service.IWaylineFileService;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

//针对福建项目修改
@RequestMapping("${url.wayline.prefix}${url.wayline.version}/thirdKmz")
@RestController
public class WaylineKmzthirdControl {
    @Autowired
    private WaylineKmzThirdService waylineKmzService;//kmz服务
    @Autowired
    private IWaylineFileService waylineFileService;
    //查询点位台帐
    @Autowired
    private PubWaylinePointDfService pubWaylinePointDfService;

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
            //进一步提取航线信息
            //存储结果
            List<ThirdPointEntity> pointsList=new ArrayList<ThirdPointEntity>();
            List<Placemark> placeMarks=wayline.getFolder().getPlaceMarks();


            for(int i = 0;i<placeMarks.size();i++){
                //存储动作序列
                String point_sequence= "";
                Placemark placemark=placeMarks.get(i);//整个航点信息
                ThirdPointEntity thirdPointEntity=new ThirdPointEntity();//单个航点信息
                thirdPointEntity.setId(placemark.getIndex());//航点位置
                //存储动作位置
                point_sequence=point_sequence+placemark.getIndex()+"_";
                ActionGroup actionGroup=placemark.getActionGroup();
                if(actionGroup!=null){
                    List<Action> actionList= actionGroup.getActionList();
                    if(actionList!=null){
                        List<ThirdAction> thirdActionList = new ArrayList<ThirdAction>();
                        for(int j=0;j<actionList.size();j++){
                            Action action = actionList.get(j);
                            //存储动作名称
                            //判断是否是拍照
                            if(action.getActionActuatorFunc().equals("takePhoto")){
                                if(j!=0){
                                    point_sequence=point_sequence+","+ i + "_"+ j+"_"+"takePhoto";
                                }else{
                                    point_sequence=point_sequence+j+"_"+"takePhoto";
                                }
                            }
                            //判断是否是定向拍照
                            if(action.getActionActuatorFunc().equals("orientedShoot")){
                                if(j!=0){
                                    point_sequence=point_sequence+","+ i+ "_"+ j+"_"+"orientedShoot";
                                }else{
                                    point_sequence=point_sequence+j+"_"+"orientedShoot";
                                }
                            }
                            //判断是否是全景拍照
                            if(action.getActionActuatorFunc().equals("panoShot")){
                                if(j!=0){
                                    point_sequence=point_sequence+","+ "i"+ "_"+ j+"_"+"panoShot";
                                }else{
                                    point_sequence=point_sequence+j+"_"+"panoShot";
                                }
                            }
                            ThirdAction thirdAction= new ThirdAction();
                            thirdAction.setActionActuatorFuncParam(action.getActionActuatorFunc());
                            thirdActionList.add(thirdAction);//存储到列表里
                        }
                        if(!point_sequence.matches("\\d+_")){//改为是否是数字_，下划线结构判断
                            thirdPointEntity.setPhoto_sequence(point_sequence);
                        }

                        thirdPointEntity.setThirdActionsList(thirdActionList);//存储一个航点所有的动作名称
                    }
                }
                //判断是否有航点序列然后进行台长帐点位信息查询
                if(thirdPointEntity.getPhoto_sequence()!=null){
                    PubWaylinePointDfEntity pubWaylinePointDfEntity= pubWaylinePointDfService.getPointByWaylineIdAndSequence(waylineId,String.valueOf(thirdPointEntity.getId()));
                    if(pubWaylinePointDfEntity!=null){
                        //查询到点位台长就存入
                        thirdPointEntity.setPubWaylinePointDfEntity(pubWaylinePointDfEntity);
                    }
                }
                pointsList.add(thirdPointEntity);
            }
//            System.out.println(pointsList);


            return HttpResultResponse.success(pointsList);
        }
    }
}
