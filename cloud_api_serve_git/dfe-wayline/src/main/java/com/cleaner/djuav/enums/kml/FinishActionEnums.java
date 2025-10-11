package com.cleaner.djuav.enums.kml;

/**
 * wpml:finishAction 航线结束动作
 * 注：动作执行过程，若飞行器退出了航线模式且进入失控状态，则会优先执行失控动作。
 */
public enum FinishActionEnums {

    GO_HOME("goHome", "飞行器完成航线任务后，退出航线模式并返航"),
    NO_ACTION("noAction", "飞行器完成航线任务后，退出航线模式"),
    AUTO_LAND("autoLand", "飞行器完成航线任务后，退出航线模式并原地降落"),
    GOTO_FIRST_WAYPOINT("gotoFirstWaypoint", "飞行器完成航线任务后，立即飞向航线起始点，到达后退出航线模式");

    private String value;
    private String description;

    FinishActionEnums(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
