package com.dji.sample.center.utils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public final class StringUtil {
    public static final String SPACE = " ";
    public static final String EMPTY = "";
    private static final int PAD_LIMIT = 8192;

    public static boolean isEmpty(String str) {
        if (str == null || "".equals(str)) {
            return true;
        }

        return false;
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static String getFormatQueryParams(String fields) {
        if (fields == null || "".equals(fields.trim())) {
            return "''";
        }

        String sTemp = fields;
        if (sTemp.startsWith("'")) {
            sTemp = sTemp.substring(1);
        }

        if (sTemp.endsWith("'")) {
            sTemp = sTemp.substring(0, sTemp.length() - 1);
        }

        sTemp = sTemp.replaceAll("'{0,1},'{0,1}", ",");
        sTemp = sTemp.replaceAll("'", "''");
        sTemp = sTemp.replaceAll(",", "','");
        return "'" + sTemp + "'";
    }

    public static boolean isContainSpcifyStr(String str, String[] container) {
        if (isEmpty(str)) {
            return false;
        }

        if (container == null) {
            return false;
        }

        for (int i = 0; i < container.length; i++) {
            if (str.equals(container[i])) {
                return true;
            }
        }

        return false;
    }

    public static String repeat(String str, int repeat) {
        char output2[], ch1, ch0;
        if (str == null) {
            return null;
        }
        if (repeat <= 0) {
            return "";
        }
        int inputLength = str.length();
        if (repeat == 1 || inputLength == 0) {
            return str;
        }
        if (inputLength == 1 && repeat <= 8192) {
            return repeat(str.charAt(0), repeat);
        }

        int outputLength = inputLength * repeat;
        switch (inputLength) {
            case 1:
                return repeat(str.charAt(0), repeat);
            case 2:
                ch0 = str.charAt(0);
                ch1 = str.charAt(1);
                output2 = new char[outputLength];
                for (int i = repeat * 2 - 2; i >= 0; i--, i--) {
                    output2[i] = ch0;
                    output2[i + 1] = ch1;
                }
                return new String(output2);
        }
        StringBuilder buf = new StringBuilder(outputLength);
        for (int i = 0; i < repeat; i++) {
            buf.append(str);
        }
        return buf.toString();
    }

    public static String trimStart(String str, String remove) {
        if (isEmpty(str) || isEmpty(remove)) {
            return str;
        }
        if (str.startsWith(remove)) {
            return str.substring(remove.length());
        }
        return str;
    }

    public static String trimEnd(String str, String remove) {
        if (isEmpty(str) || isEmpty(remove)) {
            return str;
        }
        if (str.endsWith(remove)) {
            return str.substring(0, str.length() - remove.length());
        }
        return str;
    }

    public static String repeat(String str, String separator, int repeat) {
        if (str == null || separator == null) {
            return repeat(str, repeat);
        }

        String result = repeat(str + separator, repeat);
        return trimEnd(result, separator);
    }

    public static String repeat(char ch, int repeat) {
        char[] buf = new char[repeat];
        for (int i = repeat - 1; i >= 0; i--) {
            buf[i] = ch;
        }
        return new String(buf);
    }

    public static String rightPad(String str, int size) {
        return rightPad(str, size, ' ');
    }

    public static String rightPad(String str, int size, char padChar) {
        if (str == null) {
            return null;
        }
        int pads = size - str.length();
        if (pads <= 0) {
            return str;
        }
        if (pads > 8192) {
            return rightPad(str, size, String.valueOf(padChar));
        }
        return str.concat(repeat(padChar, pads));
    }

    public static String rightPad(String str, int size, String padStr) {
        if (str == null) {
            return null;
        }
        if (isEmpty(padStr)) {
            padStr = " ";
        }
        int padLen = padStr.length();
        int strLen = str.length();
        int pads = size - strLen;
        if (pads <= 0) {
            return str;
        }
        if (padLen == 1 && pads <= 8192) {
            return rightPad(str, size, padStr.charAt(0));
        }

        if (pads == padLen)
            return str.concat(padStr);
        if (pads < padLen) {
            return str.concat(padStr.substring(0, pads));
        }
        char[] padding = new char[pads];
        char[] padChars = padStr.toCharArray();
        for (int i = 0; i < pads; i++) {
            padding[i] = padChars[i % padLen];
        }
        return str.concat(new String(padding));
    }

    public static String leftPad(String str, int size) {
        return leftPad(str, size, ' ');
    }

    public static String leftPad(String str, int size, char padChar) {
        if (str == null) {
            return null;
        }
        int pads = size - str.length();
        if (pads <= 0) {
            return str;
        }
        if (pads > 8192) {
            return leftPad(str, size, String.valueOf(padChar));
        }
        return repeat(padChar, pads).concat(str);
    }

    public static String leftPad(String str, int size, String padStr) {
        if (str == null) {
            return null;
        }
        if (isEmpty(padStr)) {
            padStr = " ";
        }
        int padLen = padStr.length();
        int strLen = str.length();
        int pads = size - strLen;
        if (pads <= 0) {
            return str;
        }
        if (padLen == 1 && pads <= 8192) {
            return leftPad(str, size, padStr.charAt(0));
        }

        if (pads == padLen)
            return padStr.concat(str);
        if (pads < padLen) {
            return padStr.substring(0, pads).concat(str);
        }
        char[] padding = new char[pads];
        char[] padChars = padStr.toCharArray();
        for (int i = 0; i < pads; i++) {
            padding[i] = padChars[i % padLen];
        }
        return (new String(padding)).concat(str);
    }

    public static String strFormat(String srcStr, boolean isFillLeft, int count, String fillStr) {
        if (isEmpty(fillStr)) {
            return srcStr;
        }

        if (count <= 0) {
            return srcStr;
        }

        String sResult = "";
        String sTemp = buildFormatStr(count, fillStr);
        if (isFillLeft) {
            sResult = sTemp + srcStr;
        } else {
            sResult = srcStr + sTemp;
        }

        return sResult;
    }

    public static String fill(String srcStr, boolean isFillLeft, int length, String fillStr) {
        if (isEmpty(fillStr)) {
            return srcStr;
        }

        if (length <= 0) {
            return srcStr;
        }

        if (isEmpty(srcStr)) {
            return srcStr;
        }

        int iLen = length - srcStr.length();
        if (iLen <= 0) {
            return srcStr;
        }

        return strFormat(srcStr, isFillLeft, iLen, fillStr);
    }

    private static String buildFormatStr(int count, String fillStr) {
        if (count <= 0 || fillStr == null || "".equals(fillStr)) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < count; i++) {
            sb.append(fillStr);
        }

        return sb.toString();
    }

    public static boolean isIn(String str, String[] scaleStr) {
        if (scaleStr == null) {
            return false;
        }

        if (str == null) {
            return false;
        }

        for (int i = 0; i < scaleStr.length; i++) {
            if (str.equals(scaleStr[i])) {
                return true;
            }
        }

        return false;
    }

    public static boolean isNotIn(String str, String[] scaleStr) {
        return !isIn(str, scaleStr);
    }

    public static String getRandomStr(int pILen, String pStrBas) {
        SecureRandom rand = new SecureRandom();
        StringBuffer sBuffer = new StringBuffer();
        for (int i = 0; i < pILen; i++) {
            int iNumbr = rand.nextInt(pStrBas.length());
            sBuffer.append(pStrBas.charAt(iNumbr));
        }
        return sBuffer.toString();
    }

    public static int ipToInt(String strIp) {
        int[] ip = new int[4];

        int position1 = strIp.indexOf(".");
        int position2 = strIp.indexOf(".", position1 + 1);
        int position3 = strIp.indexOf(".", position2 + 1);

        ip[0] = Integer.parseInt(strIp.substring(0, position1));
        ip[1] = Integer.parseInt(strIp.substring(position1 + 1, position2));
        ip[2] = Integer.parseInt(strIp.substring(position2 + 1, position3));
        ip[3] = Integer.parseInt(strIp.substring(position3 + 1));
        return (ip[0] << 24) + (ip[1] << 16) + (ip[2] << 8) + ip[3];
    }

    public static String ipToStr(int ip) {
        StringBuffer sb = new StringBuffer("");

        sb.append(String.valueOf(ip >>> 24));
        sb.append(".");

        sb.append(String.valueOf((ip & 0xFFFFFF) >>> 16));
        sb.append(".");

        sb.append(String.valueOf((ip & 0xFFFF) >>> 8));
        sb.append(".");

        sb.append(String.valueOf(ip & 0xFF));
        return sb.toString();
    }

    public static long ipToLong(String ip) {
        String[] ipArray = ip.split("\\.");
        List ipNums = new ArrayList();
        for (int i = 0; i < 4; i++) {
            ipNums.add(Long.valueOf(Long.parseLong(ipArray[i].trim())));
        }

        return ((Long) ipNums.get(0)).longValue() * 256L * 256L * 256L
                + ((Long) ipNums.get(1)).longValue() * 256L * 256L + ((Long) ipNums.get(2)).longValue() * 256L
                + ((Long) ipNums.get(3)).longValue();
    }

    public static String longToIp(long ipaddr) {
        long y = ipaddr % 256L;
        long m = (ipaddr - y) / 16777216L;
        long n = (ipaddr - 16777216L * m - y) / 65536L;
        long x = (ipaddr - 16777216L * m - 65536L * n - y) / 256L;
        return m + "." + n + "." + x + "." + y;
    }
}
