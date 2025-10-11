package com.df.framework.utils;

import com.df.framework.exception.FastException;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * 时间工具类
 *
 * @author lyc
 */
@Slf4j
public class DateUtils {
    public static String T_TIME_SIMPLE_INTERVAL = "mmss";
    public static String T_DATA_SIMPLE_INTERVAL = "yyyyMMddHH";
    public static String T_DATA = "yyyy-MM-dd";
    public static String T_DATA_SIMPLE = "yyyyMMdd";
    public static String T_DATA_TIME = "yyyy-MM-dd HH:mm:ss";
    public static String T_DATA_TIME_SIMPLE = "yyyyMMddHHmmss";
    public static String T_TIME = "HH:mm:ss";
    public static String T_DATA_TIME_MIL = "yyyyMMddHHmmssSSS";
    public static String T_TIME_SIMPLE = "HHmmss";

    //-----------------------------格式化日期-----------------------------

    /**
     * 解析日期为字符串，默认yyyy-MM-dd HH:mm:ss格式
     *
     * @param date
     * @return String
     */
    public static String parseDateToStr(Date date) {
        if (date != null) {
            return new SimpleDateFormat(T_DATA_TIME).format(date);
        } else {
            return "";
        }
    }

    /**
     * 解析日期为字符串，默认yyyy-MM-dd HH:mm:ss格式
     *
     * @param date
     * @return String
     */
    public static String parseDateToStr(LocalDateTime date) {
        if (date != null) {
            return DateTimeFormatter.ofPattern(T_DATA_TIME).format(date);
        } else {
            return "";
        }
    }


    /**
     * 解析日期为字符串，自定义格式
     *
     * @param date
     * @param format
     * @return String
     */
    public static String parseDateToStr(Date date, String format) {
        return new SimpleDateFormat(format).format(date);
    }

    /**
     * 解析字符串为日期，默认yyyy-MM-dd HH:mm:ss格式
     *
     * @return Date() 日期
     */
    public static Date parseDate(String dateStr) {
        try {
            return new SimpleDateFormat(T_DATA_TIME).parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * 解析字符串为日期，默自定义格式
     *
     * @return Date() 日期
     */
    public static Date parseDate(String dateStr, String format) {
        try {
            return new SimpleDateFormat(format).parse(dateStr);
        } catch (ParseException e) {
            log.error("解析{}类型日期出错：{}", T_DATA_TIME, e.getMessage());
            return null;
        }
    }
    //-----------------------------获取当前日期-----------------------------

    /**
     * 获取当前日期
     *
     * @return Date() 当前日期
     */
    public static Date getNowDate() {
        return new Date();
    }

    /**
     * 获取当前日期, 格式为yyyy-MM-dd
     *
     * @return String
     */
    public static String getNowDateStr() {
        String nowDateStr = new SimpleDateFormat(T_DATA).format(new Date());
        return nowDateStr;
    }

    /**
     * 获取当前日期, 格式为yyyyMMdd
     *
     * @return String
     */
    public static String getNowDateStrSimple() {
        String nowDateStr = new SimpleDateFormat(T_DATA_SIMPLE).format(new Date());
        return nowDateStr;
    }

    /**
     * 获取当前日期时间, 格式为yyyy-MM-dd HH:mm:ss
     *
     * @return String
     */
    public static String getNowDateTimeStr() {
        String nowDateTimeStr = new SimpleDateFormat(T_DATA_TIME).format(new Date());
        return nowDateTimeStr;
    }

    /**
     * 获取当前日期时间, 格式为yyyyMMddHHmmssSSS
     *
     * @return String
     */
    public static synchronized String getNowDateTimeStrMil() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
        }
        String nowDateTimeStr = new SimpleDateFormat(T_DATA_TIME_MIL).format(new Date());
        return nowDateTimeStr;
    }


    /**
     * 获取当前日期时间, 格式为yyyyMMddHHmmss
     *
     * @return String
     */
    public static synchronized String getNowDateTimeStrSimple() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        String nowDateTimeStr = new SimpleDateFormat(T_DATA_TIME_SIMPLE).format(new Date());
        return nowDateTimeStr;
    }

    /**
     * 获取当前日期时间, 格式为yyyyMMddHHmmss
     *
     * @return String
     */
    public static synchronized String getNanoTimeStr() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
        }
        return System.nanoTime() + "";
    }

    //-----------------------------其他工具方法-----------------------------

    /**
     * 计算两个时间相差天数
     *
     * @param date1
     * @param date2
     * @return 天数
     */
    public static int getDayDiff(Date date1, Date date2) {
        int diff = Math.abs((int) ((date2.getTime() - date1.getTime()) / (1000 * 3600 * 24)));
        return diff;
    }

    /**
     * 计算两个时间相差毫秒数
     *
     * @param endTime
     * @param startTime
     * @return 毫秒数
     */
    public static int getMillisecondDiff(Date endTime, Date startTime) {
        return Math.abs((int) (endTime.getTime() - startTime.getTime()));
    }

    /**
     * 计算两个时间相差秒数
     *
     * @param startTime
     * @param endTime
     * @return
     * @Description 秒数
     */
    public static int getSecondDiff(Date endTime, Date startTime) {
        return (int) ((endTime.getTime() - startTime.getTime()) / (1000));
    }

    /**
     * 计算两个时间相差分钟数
     *
     * @param endTime
     * @param startTime
     * @return
     * @Description 分钟数
     */
    public static int getMinuteDiff(Date endTime, Date startTime) {
        return (int) ((endTime.getTime() - startTime.getTime()) / (1000 * 60));
    }

    /**
     * 计算两个时间差
     *
     * @param endDate
     * @param nowDate
     * @return
     */
    public static String getDateDiffStr(Date endDate, Date nowDate) {
        long nd = 1000 * 24 * 60 * 60;
        long nh = 1000 * 60 * 60;
        long nm = 1000 * 60;
        // long ns = 1000;
        // 获得两个时间的毫秒时间差异
        long diff = endDate.getTime() - nowDate.getTime();
        // 计算差多少天
        long day = diff / nd;
        // 计算差多少小时
        long hour = diff % nd / nh;
        // 计算差多少分钟
        long min = diff % nd % nh / nm;
        // 计算差多少秒//输出结果
        // long sec = diff % nd % nh % nm / ns;
        return day + "天" + hour + "小时" + min + "分钟";
    }

    /**
     * 计算两个时间小时差
     *
     * @param endDate
     * @param nowDate
     * @return
     */
    public static long getDateHoursPoor(Date endDate, Date nowDate) {
        long nd = 1000 * 24 * 60 * 60;
        long nh = 1000 * 60 * 60;
        long nm = 1000 * 60;
        // long ns = 1000;
        // 获得两个时间的毫秒时间差异
        long diff = endDate.getTime() - nowDate.getTime();
        // 计算差多少天
        long day = diff / nd;
        // 计算差多少小时
        long hour = diff % nd / nh;
        return day * 24 + hour;
    }

    /**
     * 将秒数转换为日时分秒，
     *
     * @param second
     * @return
     */
    public static String secondToDateTimeStr(long second) {
        long days = second / 86400;            //转换天数
        second = second % 86400;            //剩余秒数
        long hours = second / 3600;            //转换小时
        second = second % 3600;                //剩余秒数
        long minutes = second / 60;            //转换分钟
        second = second % 60;                //剩余秒数
        if (days > 0) {
            return days + "天" + hours + "小时" + minutes + "分" + second + "秒";
        } else if (hours > 0) {
            return hours + "小时" + minutes + "分" + second + "秒";
        } else if (minutes > 0) {
            return minutes + "分" + second + "秒";
        } else {
            return second + "秒";
        }
    }

    /**
     * 判断两个日期是否是同一天
     *
     * @param date1
     * @param date2
     * @return
     */
    public static Boolean isSameDay(Date date1, Date date2) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        return fmt.format(date1).equals(fmt.format(date2));
    }

    /**
     * 获取前n天 或者 后n天的  零点日期 2021-10-26 00:00:00
     *
     * @param i 前传负值 后传正值
     * @return
     */
    public static Date getBeforeOrAfterStartByFormat(int i) {
        LocalDateTime time = LocalDateTime.of(
                LocalDateTime.now().plusDays(i).toLocalDate(),
                LocalTime.MIN
        );
        return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 获取前n天 或者 后n天的 24点 2021-10-28 23:59:59
     *
     * @param i 前传负值 后传正值
     * @return
     */
    public static Date getBeforeOrAfterEndByFormat(int i) {
        LocalDateTime time = LocalDateTime.of(
                LocalDateTime.now().plusDays(i).toLocalDate(),
                LocalTime.MAX
        );
        return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 获取当天的结束时间
     *
     * @param endTime
     * @return
     */
    public static Date getEnd(Date endTime) {
        try {
            return org.apache.commons.lang3.time.DateUtils.setMilliseconds(
                    org.apache.commons.lang3.time.DateUtils.setSeconds(
                            org.apache.commons.lang3.time.DateUtils.setMinutes(
                                    org.apache.commons.lang3.time.DateUtils.setHours(
                                            endTime, 23), 59), 59), 0);
        } catch (Exception e) {
            throw new FastException("时间错误");
        }
    }

    /**
     * 获取某日期的当天的开始时间
     *
     * @param endTime
     * @return
     */
    public static Date getStart(Date endTime) {
        try {
            return org.apache.commons.lang3.time.DateUtils.setMilliseconds(
                    org.apache.commons.lang3.time.DateUtils.setSeconds(
                            org.apache.commons.lang3.time.DateUtils.setMinutes(
                                    org.apache.commons.lang3.time.DateUtils.setHours(
                                            endTime, 0), 0), 0), 0);
        } catch (Exception e) {
            throw new FastException("时间错误");
        }
    }

    /**
     * @param nowTime   当前时间
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return
     * @author 判断当前时间在时间区间内
     */
    public static boolean isEffectiveDate(Date nowTime, Date startTime, Date endTime) {
        if (nowTime.getTime() == startTime.getTime()
                || nowTime.getTime() == endTime.getTime()) {
            return true;
        }

        Calendar date = Calendar.getInstance();
        date.setTime(nowTime);

        Calendar begin = Calendar.getInstance();
        begin.setTime(startTime);

        Calendar end = Calendar.getInstance();
        end.setTime(endTime);

        if (date.after(begin) && date.before(end)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获取本年的第一天
     *
     * @return String
     **/
    public static String getYear() {
        return new SimpleDateFormat("yyyy").format(new Date());
    }

    /**
     * 获取本年的第一天
     *
     * @return String
     **/
    public static String getYearStart(String year) {
        return year + "-01-01 00:00:00";
    }

    public static String getYearEnd(String year) {
        return year + "-12-31 23:59:59";
    }

    /**
     * 获取本年的最后一天
     *
     * @return String
     **/
    public static String getYearEnd() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, calendar.getActualMaximum(Calendar.MONTH));
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date currYearLast = calendar.getTime();
        return new SimpleDateFormat("yyyy-MM-dd").format(currYearLast) + " 23:59:59";
    }

    /**
     * 获取日期的前一天的最晚时间
     *
     * @param date
     * @return
     */
    public static Date getLastDayEnd(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        //按你的要求设置时间
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 23, 59, 59);
        return cal.getTime();
    }

    /**
     * 获取日期的前一天的最晚时间
     *
     * @param date
     * @return
     */
    public static Date getLastDayMiddle(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        //按你的要求设置时间
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 12, 0, 0);
        return cal.getTime();
    }

    /**
     * 获取日期的后一天的最晚时间
     *
     * @param date
     * @return
     */
    public static Date getAfterDayEnd(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        //按你的要求设置时间
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 23, 59, 59);
        return cal.getTime();
    }

    /**
     * 获取日期的上月最后一天的最晚时间
     *
     * @param date
     * @return
     */
    public static Date getLastMonthLastDayEnd(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        //调到上个月
        cal.add(Calendar.MONTH, -1);
        //得到一个月最最后一天日期(31/30/29/28)
        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        //按你的要求设置时间
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), maxDay, 23, 59, 59);
        return cal.getTime();
    }

    /**
     * 获取日期的去年最后一天的最晚时间
     *
     * @param date
     * @return
     */
    public static Date getLastYearLastDayEnd(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.YEAR, -1);
        //按你的要求设置时间
        cal.set(cal.get(Calendar.YEAR), Calendar.DECEMBER, 31, 23, 59, 59);
        //获取上给月最后一天的日期
        return cal.getTime();
    }

    /**
     * 计算指定年份的所有日期，格式如 yyyy-MM-dd
     *
     * @param year
     * @return java.util.List<java.lang.String>
     * @author Miao Zhanggui
     * @date 2023/1/12 10:31
     */
    public static List<String> getDateListByYear(Integer year) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<String> allDateList = new ArrayList<>();
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = startDate;
        while (year == endDate.getYear()) {
            allDateList.add(endDate.format(formatter));
            endDate = endDate.plusDays(1L);
        }
        return allDateList;
    }

    /**
     * 获取某年所有周的时间跨度
     *
     * @param year
     * @return
     */
    public static Map<String, Map<String, String>> getYearWeekMap(int year) {
        int weeks = getYearWeekCount(year);
        Map<String, Map<String, String>> yearWeekMap = new LinkedHashMap<>();
        for (int i = 1; i <= weeks; i++) {
            Map<String, String> timeMap = getWeekRangeMap(year, i);
            yearWeekMap.put("第" + i + "周", timeMap);
        }
        return yearWeekMap;
    }

    /**
     * 获取某年有多少周
     *
     * @param year
     * @return
     * @throws ParseException
     */
    private static int getYearWeekCount(int year) {
        int week = 52;
        try {
            Map<String, String> timeMap = getWeekRangeMap(year, 53);
            if (!timeMap.isEmpty()) {
                String startTime = timeMap.get("startTime");
                if (startTime.substring(0, 4).equals(year + "")) {
                    //判断年度是否相符，如果相符说明有53个周。
                    week = 53;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return week;
    }

    /**
     * 获取某年某周的时间跨度
     *
     * @param year
     * @param week
     * @return
     */
    private static Map<String, String> getWeekRangeMap(int year, int week) {
        Map<String, String> timeMap = new LinkedHashMap<>();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);//设置星期一为一周开始的第一天
        calendar.setMinimalDaysInFirstWeek(4);//可以不用设置
        int weekYear = calendar.get(Calendar.YEAR);//获得当前的年
        calendar.setWeekDate(weekYear, week, 2);//获得指定年的第几周的开始日期
        Date time = calendar.getTime();
        String startTime = new SimpleDateFormat("yyyy-MM-dd").format(time) + " 00:00:00";
        timeMap.put("startTime", startTime);
        calendar.setWeekDate(weekYear, week, 1);//获得指定年的第几周的结束日期
        time = calendar.getTime();
        String endTime = new SimpleDateFormat("yyyy-MM-dd").format(time) + " 23:59:59";
        timeMap.put("endTime", endTime);
        return timeMap;
    }


    /**
     * 获取某年某月开始日期
     *
     * @return String
     **/
    public static String getMonthStart(int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date time = cal.getTime();
        return new SimpleDateFormat("yyyy-MM-dd").format(time) + " 00:00:00";
    }

    /**
     * 获取某年某月最后一天
     *
     * @return String
     **/
    public static String getMonthEnd(int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date time = cal.getTime();
        return new SimpleDateFormat("yyyy-MM-dd").format(time) + " 23:59:59";
    }

    /**
     * 5、本周第一天（本周开始第一天）2024-05-27 00:00:00
     *
     * @return
     */
    public static Date getWeekStartDate() {
        // 获取当天日期
        LocalDate now = LocalDate.now();
        // 周一
        LocalDate monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        // 本周开始时间
        LocalDateTime weekStart = monday.atStartOfDay();
        ZonedDateTime zdt = weekStart.atZone(ZoneId.systemDefault());
        return Date.from(zdt.toInstant());
    }

    /**
     * 6、本周结束时间（2024-06-02 23:59:59）
     *
     * @return
     */
    public static Date getWeekEndDate() {
        // 获取当天日期
        LocalDate now = LocalDate.now();
        // 周日
        LocalDate sunday = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        // 本周结束时间
        LocalDateTime weekEnd = LocalDateTime.of(sunday, LocalTime.MAX);
        ZonedDateTime zdt = weekEnd.atZone(ZoneId.systemDefault());
        return Date.from(zdt.toInstant());
    }


    /**
     * 8、本月第一天 2024-05-01 00:00:00
     *
     * @return
     */
    public static Date getTimesMonthMorning() {
        Calendar cal = Calendar.getInstance();
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONDAY), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
        return cal.getTime();
    }


    /**
     * 9、本月结束时间 2024-05-31 23:59:59
     *
     * @return
     */
    public static Date getMonthStar() {
        // 获取当天日期
        LocalDate now = LocalDate.now();
        // 本月1号
        LocalDate firstDayOfMonth = now.with(TemporalAdjusters.firstDayOfMonth());
        // 本月1号的开始时间
        LocalDateTime firstDayOfMonthStart = firstDayOfMonth.atStartOfDay();
        ZonedDateTime zdt = firstDayOfMonthStart.atZone(ZoneId.systemDefault());
        return Date.from(zdt.toInstant());
    }

    /**
     * 9、本月结束时间 2024-05-31 23:59:59
     *
     * @return
     */
    public static Date getTimesMonthEnd() {
        // 获取当天日期
        LocalDate now = LocalDate.now();
        // 本月最后一天
        LocalDate lastDayOfMonth = now.with(TemporalAdjusters.lastDayOfMonth());
        // 本月最后一天的最后时间
        LocalDateTime firstDayOfMonthEnd = LocalDateTime.of(lastDayOfMonth, LocalTime.MAX);
        ZonedDateTime zdt = firstDayOfMonthEnd.atZone(ZoneId.systemDefault());
        return Date.from(zdt.toInstant());
    }

    public static List<TimeRange> get24HourTimeRanges(Date date) {
        List<TimeRange> timeRanges = new ArrayList<>();

        LocalDateTime localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());

        for (int i = 0; i < 24; i++) {
            // 使用模运算确保小时数在0到23之间
            int hour = i % 24;
            LocalDateTime startTime = localDateTime.withHour(hour).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime endTime = localDateTime.withHour(hour).withMinute(59).withSecond(59).withNano(0);

            timeRanges.add(new TimeRange(startTime, endTime));
        }

        return timeRanges;
    }

    public static LocalDateTime transformDateToLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    public static Date transformLocalDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static class TimeRange {
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        public TimeRange(LocalDateTime startTime, LocalDateTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        @Override
        public String toString() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return startTime.format(formatter) + " - " + endTime.format(formatter);
        }
    }

    public static void main(String[] args) {
      /*  String str = "2022-12-31 23:59:59";
        System.out.println(parseDateToStr(getLastDayEnd(parseDate(str))));
        System.out.println(parseDateToStr(getAfterDayEnd(parseDate(str))));
        System.out.println(parseDateToStr(getLastMonthLastDayEnd(parseDate(str))));
        System.out.println(parseDateToStr(getLastYearLastDayEnd(parseDate(str))));*/

        /*for (int i = 1; i < 13; i++) {
            System.out.println(getMonthStart(2025, i));
            System.out.println(getMonthEnd(2025, i));
        }*/
        // 获取当天日期
        LocalDate now = LocalDate.now();

        // 当天开始时间
        LocalDateTime todayStart = now.atStartOfDay();
        // 当天结束时间
        LocalDateTime todayEnd = LocalDateTime.of(now, LocalTime.MAX);

        // 周一
        LocalDate monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        // 周日
        LocalDate sunday = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        // 本周开始时间
        LocalDateTime weekStart = monday.atStartOfDay();
        // 本周结束时间
        LocalDateTime weekEnd = LocalDateTime.of(sunday, LocalTime.MAX);

        // 本月1号
        LocalDate firstDayOfMonth = now.with(TemporalAdjusters.firstDayOfMonth());
        // 本月最后一天
        LocalDate lastDayOfMonth = now.with(TemporalAdjusters.lastDayOfMonth());

        // 本月1号的开始时间
        LocalDateTime firstDayOfMonthStart = firstDayOfMonth.atStartOfDay();
        // 本月最后一天的最后时间
        LocalDateTime firstDayOfMonthEnd = LocalDateTime.of(lastDayOfMonth, LocalTime.MAX);

        // 今年第一天
        LocalDate beginTime = LocalDate.now().with(TemporalAdjusters.firstDayOfYear());
        // 今年最后一天
        LocalDate endTiime = LocalDate.now().with(TemporalAdjusters.lastDayOfYear());

        //获取前一天日期
        LocalDate yesterday2 = LocalDate.now().minusDays(1);

        DateTimeFormatter pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println("当天开始时间 = " + todayStart.format(pattern));
        System.out.println("当天结束时间 = " + todayEnd.format(pattern));
        System.out.println("本周开始时间 = " + weekStart.format(pattern));
        System.out.println("本周结束时间 = " + weekEnd.format(pattern));
        System.out.println("本月开始时间 = " + firstDayOfMonthStart.format(pattern));
        System.out.println("本月结束时间 = " + firstDayOfMonthEnd.format(pattern));


        Date inputDate = new Date(); // 你可以传入任意的日期
        List<TimeRange> timeRanges = get24HourTimeRanges(inputDate);
        for (TimeRange timeRange : timeRanges) {
            // LocalDateTime转Date
            Date start = Date.from(timeRange.getStartTime().atZone(ZoneId.systemDefault()).toInstant());
            Date end = Date.from(timeRange.getEndTime().atZone(ZoneId.systemDefault()).toInstant());

            System.out.println(start);
            System.out.println(end);
        }
        /*LocalDateTime now1 = LocalDateTime.now();
        LocalDateTime localDateTime = now1.plusMinutes(321);
        List<DateSplitUtils.DateSplit> splits = DateSplitUtils.splitDate(now1, localDateTime, DateSplitUtils.IntervalType.MINUTE, 30);
        if (splits != null) {
            for (DateSplitUtils.DateSplit split : splits) {
                System.out.println("切割后的时间区间:  " + split.getStartDateTime() + " ------------>" + split.getEndDateTime());
            }
        }*/

    }


}
