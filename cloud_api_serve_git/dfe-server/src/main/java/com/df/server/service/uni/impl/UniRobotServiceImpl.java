package com.df.server.service.uni.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.df.framework.config.FileConfig;
import com.df.framework.utils.DateUtils;
import com.df.framework.utils.file.FileUploadUtils;
import com.df.framework.vo.Result;
import com.df.server.entity.uni.UniRobotEntity;
import com.df.server.mapper.uni.UniRobotMapper;
import com.df.server.service.uni.UniRobotService;
import com.df.server.vo.UniRobot.UniRobotVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;


/**
 * 机器人（巡视设备）Service业务层处理
 *
 * @author ruoyi
 * @date 2021-01-20
 */
@Service
public class UniRobotServiceImpl extends ServiceImpl<UniRobotMapper, UniRobotEntity> implements UniRobotService {

    @Autowired
    private FileConfig fileConfig;

    @Override
    public UniRobotEntity getRobotByCode(String subCode, String robotCode) {
        return this.lambdaQuery().eq(UniRobotEntity::getSubCode, subCode).eq(UniRobotEntity::getRobotCode, robotCode).one();
    }

    @Override
    public int countUnRunningRobot(List<String> waiteRobotCodeList) {
        return baseMapper.countUnRunningRobot(waiteRobotCodeList);
    }

    @Override
    public UniRobotVO getDogInfo() {
        return baseMapper.getDogInfo();
    }

    @Override
    public void uploadImage(MultipartFile file) throws IOException {
        UniRobotVO dogInfo = baseMapper.getDogInfo();
        String yyyyMM = DateUtils.parseDateToStr(new Date(), "yyyyMM");
        String profile = fileConfig.getFileSavePath() + File.separator;
        String baseDir = dogInfo.getSubCode() + File.separator + yyyyMM + File.separator;
        String extension = FileUploadUtils.getExtension(file);
        String fileName = DateUtils.getNowDateTimeStrSimple() + "." + extension;
        File desc = FileUploadUtils.getAbsoluteFile(profile + baseDir + fileName);
        file.transferTo(desc);
        baseMapper.updateMap(dogInfo.getRobotCode(), baseDir + fileName);
    }

    @Override
    public void updateRobotOnlineState(int commState, int online) {
        baseMapper.updateRobotOnlineState(commState, online);
    }

}
