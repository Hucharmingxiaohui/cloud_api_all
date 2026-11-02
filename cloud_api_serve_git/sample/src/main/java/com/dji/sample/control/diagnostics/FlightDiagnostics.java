package com.dji.sample.control.diagnostics;

import com.dji.sample.control.model.param.TakeoffToPointParam;
import com.dji.sample.manage.model.dto.DeviceDTO;
import com.dji.sample.manage.service.IDeviceRedisService;
import com.dji.sdk.cloudapi.device.OsdDockDrone;
import com.dji.sdk.common.HttpResultResponse;
import com.dji.sdk.mqtt.services.ServicesReplyData;
import com.dji.sdk.mqtt.services.TopicServicesResponse;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.function.Function;

/** 日志失败不得影响飞行指令，不记录轨迹或持续跟踪设备。 */
@Component
public class FlightDiagnostics {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger("flight.diagnostics");
    private final IDeviceRedisService redis;
    private final Clock clock;
    private final Consumer<String> sink;

    @Autowired
    public FlightDiagnostics(IDeviceRedisService redis) {
        this(redis, Clock.systemDefaultZone(), message -> LOG.info("{}", message));
    }

    FlightDiagnostics(IDeviceRedisService redis, Clock clock, Consumer<String> sink) {
        this.redis = redis;
        this.clock = clock;
        this.sink = sink;
    }

    public static final class Session {
        private final String action;
        private final String gateway;

        private Session(String action, String gateway) {
            this.action = action;
            this.gateway = gateway;
        }
    }

    public HttpResultResponse command(String action, String gateway,
                                      Function<Session, HttpResultResponse> command) {
        Session session = new Session(action, gateway);
        try {
            HttpResultResponse result = command.apply(session);
            safe(() -> emit(session, "处理结果，结果码=" + (result == null ? "未知" : result.getCode())));
            return result;
        } catch (RuntimeException failure) {
            safe(() -> emit(session, "处理异常，异常类型=" + failure.getClass().getSimpleName()));
            throw failure;
        }
    }

    public void outbound(Session session, TakeoffToPointParam param) {
        safe(() -> {
            String time = OffsetDateTime.now(clock).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            String heights = param == null
                    ? "设备上报返航高度=" + reportedReturnHeight(session.gateway)
                    : "目标悬停高度=" + height(param.getTargetHeight()) + "（WGS84椭球高，非离地高度）"
                    + "，安全起飞高度=" + height(param.getSecurityTakeoffHeight()) + "（相对机场起飞点）"
                    + "，指点飞行高度=" + height(param.getCommanderFlightHeight()) + "（相对机场起飞点）"
                    + "，设置返航高度=" + height(param.getRthAltitude()) + "（相对机场起飞点）";
            emit(session, "指令时间=" + time + "，机巢=" + clean(session.gateway) + "，" + heights);
        });
    }

    public void reply(Session session, TopicServicesResponse<?> response) {
        safe(() -> {
            Object code = "未知";
            if (response != null && response.getData() instanceof ServicesReplyData) {
                ServicesReplyData<?> data = (ServicesReplyData<?>) response.getData();
                if (data.getResult() != null) code = data.getResult().getCode();
            }
            emit(session, "指令已应答，不代表飞行完成，结果码=" + code);
        });
    }

    private String reportedReturnHeight(String gateway) {
        try {
            return redis.getDeviceOnline(gateway).map(DeviceDTO::getChildDeviceSn)
                    .flatMap(sn -> redis.getDeviceOsd(sn, OsdDockDrone.class))
                    .map(OsdDockDrone::getRthAltitude)
                    .map(value -> height(value) + "（相对机场起飞点）")
                    .orElse("未知（暂无设备上报）");
        } catch (RuntimeException ignored) {
            return "未知（暂无设备上报）";
        }
    }

    private void emit(Session session, String message) {
        sink.accept("[飞行操作] " + ("return_home".equals(session.action) ? "返航" : "一键起飞")
                + "，" + message);
    }

    private static String height(Number value) {
        return value == null ? "未知" : new BigDecimal(value.toString()).stripTrailingZeros().toPlainString() + "米";
    }

    private static String clean(String value) {
        return value == null ? "未知" : value.replaceAll("\\s", "");
    }

    private static void safe(Runnable task) {
        try { task.run(); } catch (RuntimeException ignored) { /* 诊断异常不影响原指令，也不泄露异常消息。 */ }
    }
}
