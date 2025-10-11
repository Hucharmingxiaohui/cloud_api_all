package com.df.framework.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 日历工具类
 *
 * @author lyc
 * @date 2022/3/24
 */
public class CalendarUtils {

    /**
     * 判断当前时间在时间区间内
     *
     * @param nowTime
     * @param startTime
     * @param endTime
     * @return
     */
    public static boolean isBetweenDate(Date nowTime, Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            return false;
        }

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
     * 获取某个月最大天数
     *
     * @param year
     * @param month
     * @return
     */
    public static int getDaysByYearMonth(int year, int month) {
        Calendar a = Calendar.getInstance();
        a.set(Calendar.YEAR, year);
        a.set(Calendar.MONTH, month - 1);
        a.set(Calendar.DATE, 1);
        a.roll(Calendar.DATE, -1);
        return a.get(Calendar.DATE);
    }

    /**
     * 根据日期获取周几， 周一至周日对应1-7
     *
     * @param dates
     * @return
     */
    public static int getDayOfWeek(String dates) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        Date d;
        try {
            d = f.parse(dates);
        } catch (ParseException e) {
            return -1;
        }

        if (d == null) {
            return -1;
        }

        cal.setTime(d);
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (w == 0) {
            w = 7;
        }

        return w;
    }

    /**
     * 根据日期获取周几， 周一至周日对应1-7
     *
     * @param date
     * @return
     */
    public static int getDayOfWeek(Date date) {
        if (date == null) {
            return -1;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (w == 0) {
            w = 7;
        }
        return w;
    }

    /**
     * 根据日期判断是月里几号
     *
     * @param date
     * @return
     */
    public static int getDateOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dateOfMonth = cal.get(Calendar.DATE);
        return dateOfMonth;
    }

    /**
     * 增加小时
     *
     * @param date
     * @param num
     * @return
     */
    public static Date addHour(Date date, int num) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        // 24小时制
        cal.add(Calendar.HOUR, num);
        return cal.getTime();
    }
}
