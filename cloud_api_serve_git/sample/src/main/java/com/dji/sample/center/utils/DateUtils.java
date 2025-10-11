package com.dji.sample.center.utils;

import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

/**
 * 时间工具类
 *
 * @author lyc
 */
@Slf4j
public class DateUtils {
    public static String T_DATA = "yyyy-MM-dd";
    public static String T_DATA_SIMPLE = "yyyyMMdd";
    public static String T_DATA_TIME = "yyyy-MM-dd HH:mm:ss";
    public static String T_DATA_TIME_SIMPLE = "yyyyMMddHHmmss";


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
            return null;
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
        if (date != null) {
            return new SimpleDateFormat(format).format(date);
        } else {
            return null;
        }
    }

    /**
     * 解析字符串为日期，默认yyyy-MM-dd HH:mm:ss格式
     *
     * @return Date() 日期
     */
    // TODO: 2022/11/25  增加 空值判断
    public static Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return new SimpleDateFormat(T_DATA_TIME).parse(dateStr);
        } catch (ParseException e) {
            log.error("解析{}类型日期出错：{}", T_DATA_TIME, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 解析字符串为日期，默自定义格式
     *
     * @return Date() 日期
     */
    // TODO: 2022/11/25  增加 空值判断
    public static Date parseDate(String dateStr, String format) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        try {
            return new SimpleDateFormat(format).parse(dateStr);
        } catch (ParseException e) {
            log.error("解析{}类型日期出错：{}", T_DATA_TIME, e);
            throw new RuntimeException(e);
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
     * 获取当前日期时间, 格式为yyyyMMddHHmmss
     *
     * @return String
     */
    public static String getNowDateTimeStrSimple() {
        String nowDateTimeStr = new SimpleDateFormat(T_DATA_TIME_SIMPLE).format(new Date());
        return nowDateTimeStr;
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
     * 计算两个时间相差天数
     *
     * @param date1
     * @param date2
     * @return 天数
     */
    public static int getDayDiffForStatistic(Date date1, Date date2) {
        int diff = Math.abs((int) (((date2.getTime() - date1.getTime()) / (1000 * 3600 * 24)) + 1));
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
     * @Author: ljh
     * @Description:获取任意天后的时间
     * @DateTime: 18:52 2022/11/23
     * @Params: day 1表示后一天 -1表示前一天
     * @Return
     */
    public static Date getAfterDayTime(Date date, int day){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        calendar.add(Calendar.DATE, day);
        return calendar.getTime();
    }

    /**
     * 获取昨天
     * @return String
     * */
    public static String getYesterday(){
        Calendar cal=Calendar.getInstance();
        cal.add(Calendar.DATE,-1);
        Date time=cal.getTime();
        return new SimpleDateFormat("yyyy-MM-dd").format(time);
    }

    /**
     * 获取本月开始日期
     * @return String
     * **/
    public static String getMonthStart(){
        Calendar cal=Calendar.getInstance();
        cal.add(Calendar.MONTH, 0);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date time=cal.getTime();
        return new SimpleDateFormat("yyyy-MM-dd").format(time)+" 00:00:00";
    }

    /**
     * 获取本月最后一天
     * @return String
     * **/
    public static String getMonthEnd(){
        Calendar cal=Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date time=cal.getTime();
        return new SimpleDateFormat("yyyy-MM-dd").format(time)+" 23:59:59";
    }

    /**
     * 获取本周的第一天
     * @return String
     * **/
    public static String getWeekStart(){
        Calendar cal=Calendar.getInstance();
        cal.add(Calendar.WEEK_OF_MONTH, 0);
        cal.set(Calendar.DAY_OF_WEEK, 2);
        Date time=cal.getTime();
        return new SimpleDateFormat("yyyy-MM-dd").format(time)+" 00:00:00";
    }

    /**
     * 获取本周的最后一天
     * @return String
     * **/
    public static String getWeekEnd(){
        Calendar cal=Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, cal.getActualMaximum(Calendar.DAY_OF_WEEK));
        cal.add(Calendar.DAY_OF_WEEK, 1);
        Date time=cal.getTime();
        return new SimpleDateFormat("yyyy-MM-dd").format(time)+" 23:59:59";
    }
}
