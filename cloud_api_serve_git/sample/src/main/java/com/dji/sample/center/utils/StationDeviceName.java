package com.dji.sample.center.utils;

public final class StationDeviceName {
    private StationDeviceName() { }

    public static String format(String name, Integer picType) {
        if (name == null || name.isBlank()) return name;
        String suffix = Integer.valueOf(0).equals(picType) ? "-可见光"
                : Integer.valueOf(1).equals(picType) ? "-红外" : "";
        return suffix.isEmpty() || name.endsWith(suffix) ? name : name + suffix;
    }
}
