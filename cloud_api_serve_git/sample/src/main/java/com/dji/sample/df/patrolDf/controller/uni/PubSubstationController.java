package com.dji.sample.df.patrolDf.controller.uni;


import com.df.framework.vo.Result;
import com.df.framework.vo.Tree;
import com.df.server.entity.uni.PubSubstationEntity;
import com.df.server.entity.uni.UniPatrolSystemEntity;
import com.df.server.service.uni.PubSubstationService;
import com.df.server.service.uni.UniPatrolSystemService;
import com.dji.sample.component.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 变电站接口
 * <p>
 * Created by lianyc on 2025-02-11
 */
@RestController
@RequestMapping("/dog/pubSubstation")
@AuthInterceptor.IgnoreAuth
public class PubSubstationController {

    private static final String LOG_TITLE = "";

    @Autowired
    private PubSubstationService pubSubstationService;
    @Autowired
    private UniPatrolSystemService uniPatrolSystemService;

    /**
     * 变电站树
     *
     * @return
     */
    @PostMapping("/tree")
    public Result<List<Tree>> pubSubstationTree(HttpServletRequest request) {
        List<Tree> treeList = pubSubstationService.pubSubstationTree();
        return Result.success(treeList);
    }

    /**
     * 获取当前变电站信息
     *
     * @return
     */
    @PostMapping("/info")
    public Result<PubSubstationEntity> info(HttpServletRequest request) {
        PubSubstationEntity one = pubSubstationService.lambdaQuery().last("limit 1").one();
        return Result.success(one);
    }

    /**
     * 获取当前变电站系统信息
     *
     * @return
     */
    @PostMapping("/sysInfo")
    public Result<UniPatrolSystemEntity> SysInfo(HttpServletRequest request) {
        UniPatrolSystemEntity one = uniPatrolSystemService.lambdaQuery().last("limit 1").one();
        return Result.success(one);
    }

}

