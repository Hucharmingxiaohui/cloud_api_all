package com.dji.sample.df.cqDockDf.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Exact EUA topic grammar; legacy leading slash is accepted only on inbound messages. */
public final class CqDockMqttTopic {
    private static final Pattern OSD = Pattern.compile("(?:third/|/)([^/]+)/([^/]+)/(drone/osd|dock/osd|push/hms)");
    private static final Pattern PICTURE = Pattern.compile("(?:third/|/)([^/]+)/push/picture");
    private final String unitCode;
    private final String dockSn;
    private final String kind;

    private CqDockMqttTopic(String unitCode, String dockSn, String kind) {
        this.unitCode = unitCode; this.dockSn = dockSn; this.kind = kind;
    }

    public static CqDockMqttTopic parse(String topic) {
        if (topic == null) return null;
        Matcher osd = OSD.matcher(topic);
        if (osd.matches()) return new CqDockMqttTopic(osd.group(1), osd.group(2), osd.group(3));
        Matcher picture = PICTURE.matcher(topic);
        return picture.matches() ? new CqDockMqttTopic(picture.group(1), null, "push/picture") : null;
    }

    public String getUnitCode() { return unitCode; }
    public String getDockSn() { return dockSn; }
    public String getKind() { return kind; }
}
