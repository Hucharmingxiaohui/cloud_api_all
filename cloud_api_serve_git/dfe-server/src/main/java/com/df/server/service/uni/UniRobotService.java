package com.df.server.service.uni;

import com.baomidou.mybatisplus.extension.service.IService;
import com.df.server.entity.uni.UniRobotEntity;
import com.df.server.vo.UniRobot.UniRobotVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 机器人（巡视设备）Service接口
 *
 * @author ruoyi
 * @date 2021-01-20
 */
public interface UniRobotService extends IService<UniRobotEntity> {


    UniRobotEntity getRobotByCode(String subCode, String robotCode);

    int countUnRunningRobot(List<String> waiteRobotCodeList);

    UniRobotVO getDogInfo();

    void uploadImage(MultipartFile file) throws IOException;

    void updateRobotOnlineState(int commState, int online);
}
