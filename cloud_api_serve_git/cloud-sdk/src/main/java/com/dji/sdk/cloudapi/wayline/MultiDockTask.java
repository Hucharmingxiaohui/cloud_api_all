package com.dji.sdk.cloudapi.wayline;

import com.dji.sdk.common.BaseModel;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public class MultiDockTask extends BaseModel {

    @Valid
    @NotNull
    @Size(min = 2)
    private List<MultiDockTaskDockInfo> dockInfos;

    @NotNull
    private Map<String, Object> wirelessLinkTopo;

    public MultiDockTask() {
    }

    public List<MultiDockTaskDockInfo> getDockInfos() {
        return dockInfos;
    }

    public MultiDockTask setDockInfos(List<MultiDockTaskDockInfo> dockInfos) {
        this.dockInfos = dockInfos;
        return this;
    }

    public Map<String, Object> getWirelessLinkTopo() {
        return wirelessLinkTopo;
    }

    public MultiDockTask setWirelessLinkTopo(Map<String, Object> wirelessLinkTopo) {
        this.wirelessLinkTopo = wirelessLinkTopo;
        return this;
    }
}
