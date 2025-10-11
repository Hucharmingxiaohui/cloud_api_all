package com.df.framework.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class DateSplitUtils {

    public enum IntervalType {
        DAY,
        HOUR,
        MINUTE,
        SECOND
    }

    /**
     * 时间切割
     *
     * @param startTime    被切割的开始时间
     * @param endTime      被切割的结束时间
     * @param intervalType 要切割的时间单位
     * @param interval     多长时间切割一份
     * @return List<DateSplit>
     */
    public static List<DateSplit> splitDate(LocalDateTime startTime, LocalDateTime endTime, IntervalType intervalType, int interval) {
        if (interval < 0) {
            return null;
        }
        if (endTime.isBefore(startTime)) {
            return null;
        }

        if (intervalType == IntervalType.DAY) {
            return splitByDay(startTime, endTime, interval);
        }
        if (intervalType == IntervalType.HOUR) {
            return splitByHour(startTime, endTime, interval);
        }
        if (intervalType == IntervalType.MINUTE) {
            return splitByMinute(startTime, endTime, interval);
        }
        if (intervalType == IntervalType.SECOND) {
            return splitBySecond(startTime, endTime, interval);
        }
        return null;
    }

    /**
     * 按照小时切割时间区间
     */
    public static List<DateSplit> splitByHour(LocalDateTime startTime, LocalDateTime endTime, int intervalHours) {
        if (endTime.isBefore(startTime)) {
            return null;
        }

        List<DateSplit> dateSplits = new ArrayList<>(256);

        DateSplit param = new DateSplit();
        param.setStartDateTime(startTime);
        param.setEndDateTime(endTime);
        param.setEndDateTime(addHours(startTime, intervalHours));
        while (true) {
            param.setStartDateTime(startTime);
            LocalDateTime tempEndTime = addHours(startTime, intervalHours);
            if (tempEndTime.isAfter(endTime)) {
                tempEndTime = endTime;
            }
            param.setEndDateTime(tempEndTime);

            dateSplits.add(new DateSplit(param.getStartDateTime(), param.getEndDateTime()));

            startTime = addHours(startTime, intervalHours);
            if (startTime.isAfter(endTime)) {
                break;
            }
            if (param.getEndDateTime().isAfter(endTime)) {
                break;
            }
            if (startTime.isEqual(endTime)) {
                break;
            }
        }
        return dateSplits;
    }

    /**
     * 按照秒切割时间区间
     */
    public static List<DateSplit> splitBySecond(LocalDateTime startTime, LocalDateTime endTime, int intervalSeconds) {
        if (endTime.isBefore(startTime)) {
            return null;
        }
        List<DateSplit> dateSplits = new ArrayList<>(256);

        DateSplit param = new DateSplit();
        param.setStartDateTime(startTime);
        param.setEndDateTime(endTime);
        param.setEndDateTime(addSeconds(startTime, intervalSeconds));
        while (true) {
            param.setStartDateTime(startTime);
            LocalDateTime tempEndTime = addSeconds(startTime, intervalSeconds);
            if (tempEndTime.isAfter(endTime)) {
                tempEndTime = endTime;
            }
            param.setEndDateTime(tempEndTime);

            dateSplits.add(new DateSplit(param.getStartDateTime(), param.getEndDateTime()));

            startTime = addSeconds(startTime, intervalSeconds);
            if (startTime.isAfter(endTime)) {
                break;
            }
            if (param.getEndDateTime().isAfter(endTime)) {
                break;
            }
            if (startTime.isEqual(endTime)) {
                break;
            }
        }
        return dateSplits;
    }

    /**
     * 按照天切割时间区间
     */
    public static List<DateSplit> splitByDay(LocalDateTime startTime, LocalDateTime endTime, int intervalDays) {
        if (endTime.isBefore(startTime)) {
            return null;
        }
        List<DateSplit> dateSplits = new ArrayList<>(256);

        DateSplit param = new DateSplit();
        param.setStartDateTime(startTime);
        param.setEndDateTime(endTime);
        param.setEndDateTime(addDays(startTime, intervalDays));
        while (true) {
            param.setStartDateTime(startTime);
            LocalDateTime tempEndTime = addDays(startTime, intervalDays);
            if (tempEndTime.isAfter(endTime)) {
                tempEndTime = endTime;
            }
            param.setEndDateTime(tempEndTime);

            dateSplits.add(new DateSplit(param.getStartDateTime(), param.getEndDateTime()));

            startTime = addDays(startTime, intervalDays);
            if (startTime.isAfter(endTime)) {
                break;
            }
            if (param.getEndDateTime().isAfter(endTime)) {
                break;
            }
            if (startTime.isEqual(endTime)) {
                break;
            }
        }
        return dateSplits;
    }

    /**
     * 按照分钟切割时间区间
     *
     * @param startTime
     * @param endTime
     * @param intervalMinutes
     * @return
     */
    public static List<DateSplit> splitByMinute(LocalDateTime startTime, LocalDateTime endTime, int intervalMinutes) {
        if (endTime.isBefore(startTime)) {
            return null;
        }
        List<DateSplit> dateSplits = new ArrayList<>(256);

        DateSplit param = new DateSplit();
        param.setStartDateTime(startTime);
        param.setEndDateTime(endTime);
        param.setEndDateTime(addMinute(startTime, intervalMinutes));
        while (true) {
            param.setStartDateTime(startTime);
            LocalDateTime tempEndTime = addMinute(startTime, intervalMinutes);
            if (tempEndTime.isAfter(endTime)) {
                tempEndTime = endTime;
            }
            param.setEndDateTime(tempEndTime);

            dateSplits.add(new DateSplit(param.getStartDateTime(), param.getEndDateTime()));

            startTime = addMinute(startTime, intervalMinutes);
            if (startTime.isAfter(endTime)) {
                break;
            }
            if (param.getEndDateTime().isAfter(endTime)) {
                break;
            }
            if (startTime.isEqual(endTime)) {
                break;
            }
            if (startTime.isEqual(endTime)) {
                break;
            }
        }
        return dateSplits;
    }


    private static LocalDateTime addDays(LocalDateTime date, int days) {
        return add(date, ChronoUnit.DAYS, days);
    }

    private static LocalDateTime addHours(LocalDateTime date, int hours) {
        return add(date, ChronoUnit.HOURS, hours);
    }

    public static LocalDateTime addMinute(LocalDateTime date, int minute) {
        return add(date, ChronoUnit.MINUTES, minute);
    }

    private static LocalDateTime addSeconds(LocalDateTime date, int second) {
        return add(date, ChronoUnit.SECONDS, second);
    }

    private static LocalDateTime add(final LocalDateTime date, ChronoUnit unit, final int amount) {
        return date.plus(amount, unit);
    }

    private static String formatDateTime(LocalDateTime date) {
        if (date == null) {
            return "";
        }
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return format.format(date);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DateSplit {
        private LocalDateTime startDateTime;
        private LocalDateTime endDateTime;

        public String getStartDateTimeStr() {
            return formatDateTime(startDateTime);
        }

        public String getEndDateTimeStr() {
            return formatDateTime(endDateTime);
        }
    }


    /*public static void main(String[] args) {
        LocalDateTime startTime = DateUtils.transformDateToLocalDateTime(DateUtils.parseDate("2024-07-20 01:01:36"));
        LocalDateTime endTime = DateUtils.transformDateToLocalDateTime(DateUtils.parseDate("2024-07-27 23:56:27"));
        List<DateSplitUtils.DateSplit> hsplits = DateSplitUtils.splitDate(startTime, endTime, DateSplitUtils.IntervalType.HOUR, 6);
        List<DateSplitUtils.DateSplit> msplits = DateSplitUtils.splitDate(startTime, endTime, IntervalType.MINUTE, 6);
        System.out.println(hsplits.size());
        for (int i = 0; i < hsplits.size(); i++) {
            DateSplit dateSplit = hsplits.get(i);
            System.out.println(dateSplit.getStartDateTimeStr());
            System.out.println(dateSplit.getEndDateTimeStr());
        }
        System.out.println("__________________________________________");
        System.out.println(msplits.size());
        for (int i = 0; i < msplits.size(); i++) {
            DateSplit dateSplit = msplits.get(i);
            System.out.println(dateSplit.getStartDateTimeStr());
            System.out.println(dateSplit.getEndDateTimeStr());
        }
    }*/
}
