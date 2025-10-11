package com.df.framework.utils;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 获取两个日期之间的所有周四 并计算每个周四在当月是第几周
 *
 * @author lzw
 * @Date 2019年3月6日
 */
public class DateWeekUtil {

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        /*String week = "星期一";
        List<Date> test = getWeekly("2024-01-01 00:00:00", "2024-12-31 23:59:59", week);
        System.err.println(test);

        System.out.println(currentDataToWeek(new Date()));

        Iterator<Calendar> iterator = DateUtils.iterator(new Date(), DateUtils.RANGE_WEEK_RELATIVE);
        while (iterator.hasNext()) {
            Date time = iterator.next().getTime();
            System.out.println(com.df.framework.utils.DateUtils.parseDateToStr(time));
        }
*/
        /*List<Date> dates = DateWeekUtil.findDates(
                com.df.framework.utils.DateUtils.getYearStart("2024" + ""),
                com.df.framework.utils.DateUtils.getYearEnd("2024" + ""));
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (Date date : dates) {
            System.out.println(format.format(date));
        }*/

        System.out.println(DateWeekUtil.getWeekly(
                DateUtils.getYearStart(2024 + ""),
                DateUtils.getYearEnd(2024 + ""),
                "星期一"));

    }

    /**
     * 根据开始时间和结束时间计算之间的星期
     *
     * @param beginDate
     * @param endDate
     * @return
     * @throws Exception
     */
    public static List<Date> getWeekly(String beginDate, String endDate, String week) {
        //获取俩个日期之间的日期
        List<Date> list = null;
        try {
            list = findDates(beginDate, endDate);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        List<Date> weekDateList = new ArrayList<>();
        //遍历
        for (Date date : list) {
            //判断当前是星期几
            boolean thursday = weekdayOrNot(date, week);
            if (thursday) {
                weekDateList.add(date);
            }

        }
        return weekDateList;
    }

    public static List<Date> findDates(String dBegin, String dEnd) throws ParseException {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calBegin = Calendar.getInstance();
        try {
            calBegin.setTime(format.parse(dBegin));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        Calendar calEnd = Calendar.getInstance();
        try {
            calEnd.setTime(format.parse(dEnd));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        List<Date> Datelist = new ArrayList<>();

        Datelist.add(calBegin.getTime());
        while (format.parse(dEnd).after(calBegin.getTime())) {
            calBegin.add(Calendar.DAY_OF_MONTH, 1);
            if (format.parse(dEnd).after(calBegin.getTime())) {
                Datelist.add(calBegin.getTime());
            } else {
                break;
            }

        }
        return Datelist;
    }

    private static boolean weekdayOrNot(Date date, String week) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE", Locale.SIMPLIFIED_CHINESE);
        String currSun = simpleDateFormat.format(date);
        //判断当前是星期几
        if (currSun.equals(week)) {
            return true;
        }
        return false;
    }


    /**
     * 日期转星期
     *
     * @return
     */
    public static String currentDataToWeek(Date datet) {
        String[] weekDays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        // 获得一个日历
        Calendar cal = Calendar.getInstance();
        cal.setTime(datet);
        // 指示一个星期中的某天。
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (w < 0) {
            w = 0;
        }
        return weekDays[w];
    }
    /*private static String getWeek(Date date) throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String format = sdf.format(date);
        String substring = format.substring(0, 7);
        int number = calendar.get(Calendar.WEEK_OF_MONTH);
        String week = substring + "-0" + number;
        return week;
    }*/
}
