package com.dji.sample.center.v2022.runnable;

import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.base.PatrolHostCommandSimple;
import com.dji.sample.center.v2022.command.model.PatrolUavModelItem;
import com.dji.sample.center.v2022.data.CenterProtocolData;
import com.dji.sample.center.v2022.tool.BaseItem;
import com.dji.sample.center.v2022.tool.CenterXmlTool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 巡视上级模型同步指令处理
 *
 * @author lyc
 * @date 2022/4/1
 */
@Slf4j
public class CenterModelSyncRunnable extends CenterMessageBaseRunnable {

    public CenterModelSyncRunnable(CenterProtocolData protocolData) {
        super(protocolData);
    }

    @Override
    public void run() {
        try {
            PatrolHostCommandSimple commandSimple = CenterXmlTool.deserialize(xmlMessage);
            String command = commandSimple.getCommand();
            String subCode = commandSimple.getCode();//xml中的code节点为变电站编码
            com.dji.sample.center.v2022.builder.CenterModelSyncBuilder modelBuilder = com.dji.sample.center.v2022.builder.CenterModelSyncBuilder.getInstance();
            List<BaseItem> items = new ArrayList<>();
            PatrolUavModelItem item = null;
            String file_path = "";
            //巡视系统无人机模型
            if (command.equals("1")) {
                items = modelBuilder.genPatrolUavFile(subCode);
                item = (PatrolUavModelItem) items.get(0);
                file_path = item.getDevice_file_path();
            }

            //上传模型文件到中心端FTPS
            if (item != null && StringUtils.isNotEmpty(file_path)) {

                String []file_paths = file_path.split(";");
                for (int i = 0;i< file_paths.length;i++)
                {
                    modelBuilder.uploadModelFile(file_paths[i]);
                }
            }

            //给中心端发送模型同步响应
            this.sendModelSyncResponse(items);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("【巡视上级模型同步指令】处理异常，巡视上级IP：{}，异常信息：{}", this.centerIp, e.getMessage());
        }
    }

    /**
     * 发送模型同步响应
     */
    private void sendModelSyncResponse(List<BaseItem> items) {
        PatrolHostCommand response = patrolHostSocketClient.getBaseCommand("251", "4", "200");
        response.addItems(items);
        this.patrolHostSocketClient.responseCommand(response, PatrolUavModelItem.class, requestSerialNum);
    }
}
