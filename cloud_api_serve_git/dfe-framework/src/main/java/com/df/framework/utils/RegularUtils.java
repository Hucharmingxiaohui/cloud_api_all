package com.df.framework.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegularUtils {

    private static final Logger log = LoggerFactory.getLogger(RegularUtils.class);


    private static final List<Character> CHARACTERS =
            Arrays.asList('!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '=', '_', '+');

    private static final List<String> SPECIAL_WORD =
            Arrays.asList("password", "root", "guest", "admin", "administrator", "ftp", "pwd", "user", "usr");


    /**
     * 校验必须是数字
     *
     * @param str
     * @return
     */
    public static boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("^-?[0-9]+"); //这个也行
        //Pattern pattern = Pattern.compile("^-?\\d+(\\.\\d+)?$");//这个也行
        Matcher isNum = pattern.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }

    /**
     * 校验必须是某区间内
     *
     * @param str
     * @param min
     * @param max
     * @return
     */
    public static boolean isNumericOver(String str, Integer min, Integer max) {
        boolean numeric = isNumeric(str);
        if (!numeric) {
            return false;
        }
        try {
            Long i = Long.parseLong(str);
            if (i < min || i > max) {
                return false;
            }
        } catch (Exception e) {
            log.error("{}", e);
            return false;
        }
        return true;
    }

    /**
     * 是否在list内
     *
     * @param str
     * @param list
     * @return
     */
    public static boolean isNumericInner(String str, List<Integer> list) {
        boolean numeric = isNumeric(str);
        if (!numeric) {
            return false;
        }
        try {
            int i = Integer.parseInt(str);
            if (!list.contains(i)) {
                return false;
            }
        } catch (Exception e) {
            log.error("{}", e);
            return false;
        }
        return true;
    }

    /**
     * 校验密码格式
     *
     * @param pwd
     * @return
     */
    public static boolean checkPwdFormat(String pwd) {

        //8位以上
        boolean flag8 = false;
        if (pwd.length() >= 8) {
            flag8 = true;
        }

        //至少有字母
        boolean matches = pwd.matches(".*[a-zA-Z]+.*");

        //必须包含数字
        boolean digit = false;
        if (hasDigit(pwd)) {
            digit = true;
        }

        //必须包含特殊字符
        boolean contains = false;
        for (Character string : CHARACTERS) {
            if (pwd.indexOf(string) > 0) {
                contains = true;
                break;
            }
        }

        /*//检查特殊单词
        boolean specialWord = true;
        char[] c = new char[pwd.length()];
        for (int i = 0; i < pwd.length(); i++) {
            c = pwd.toCharArray();
        }
        String checkPwd = String.copyValueOf(c).replace(" ", "").replace("  ", "");
        checkPwd = StringUtils.replaceAllBlank(checkPwd);
        for (Character string : CHARACTERS) {
            if ("@".equals(string.toString())) {
                continue;
            }
            checkPwd = checkPwd.toLowerCase(Locale.ROOT).replace(string.toString(), "");
        }
        checkPwd = checkPwd.replaceAll("@", "a");
        checkPwd = checkPwd.replaceAll("0", "o");
        for (String word : SPECIAL_WORD) {
            if (checkPwd.contains(word)) {
                specialWord = false;
                break;
            }
        }*/

        //顺号 123456  abcdefg  超过3位
        /*boolean hasLh = true;
        if (hasLH(pwd, 3)) {
            hasLh = false;
        }*/

        //连续重复字符 超过3位
        /*boolean repeatChar = true;
        if (isRepeatChar(pwd, 3)) {
            repeatChar = false;
        }*/

        //return flag8 && matches && digit && contains && specialWord && hasLh && repeatChar;
        return flag8 && matches && digit && contains;
    }


    /**
     * 判断字符串是否是连续字母或者是连续的数字
     *
     * @param str
     * @param count
     * @return
     */
    private static boolean hasLH(String str, int count) {
        int pre = 0;
        int len = 1;
        for (int i = 0; i < str.length(); i++) {
            String s = str.substring(i, i + 1);
            char c = s.charAt(0);
            if (i == 0) {
                pre = c;
            }
            if (c - 1 == pre) {
                len++;
                if (len >= count) {
                    return true;
                }
            } else {
                len = 1;
            }
            pre = c;
        }
        return false;
    }

    private static boolean isRepeatChar(String str, int i) {
        char[] charArr = str.toCharArray();
        int count = 0;
        int t = charArr[0];
        for (char c : charArr) {
            if (t == c) {
                count++;
                if (count == i) {
                    return true;
                }
            } else {
                t = c;
                count = 0;
            }
        }
        return false;
    }

    // 判断一个字符串是否含有数字
    public static boolean hasDigit(String content) {
        boolean flag = false;
        Pattern p = Pattern.compile(".*\\d+.*");
        Matcher m = p.matcher(content);
        if (m.matches()) {
            flag = true;
        }
        return flag;
    }

    /**
     * 判断是否为合法IP
     *
     * @param ipAddress
     * @return
     */
    public static boolean isBoolIp(String ipAddress) {
        String ip = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
        Pattern pattern = Pattern.compile(ip);
        Matcher matcher = pattern.matcher(ipAddress);
        return matcher.matches();
    }

    /**
     * 判断是否为合法Mac
     *
     * @param mac
     * @return
     */
    public static boolean isBoolMac(String mac) {
        String trueMacAddress = "([A-Fa-f0-9]{2}:){5}[A-Fa-f0-9]{2}";
        if (mac.matches(trueMacAddress)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 判断是否为合法Mac
     *
     * @param mac
     * @return
     */
    public static boolean isBoolMacNew(String mac) {
        String trueMacAddress = "^([0-9a-fA-F]{2})(([/\\s:-][0-9a-fA-F]{2}){5})$";
        if (mac.matches(trueMacAddress)) {
            return true;
        } else {
            return false;
        }
    }
}
