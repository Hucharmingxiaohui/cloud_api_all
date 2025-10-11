package com.dji.sample.center.utils;

import java.math.BigDecimal;
import java.math.BigInteger;

public class NumberUtils {
    public static int stringToInt(String str) {
        return toInt(str, 0);
    }

    public static int toInt(String str, int defaultValue) {
        if (str == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    public static Float createFloat(String val) {
        return Float.valueOf(val);
    }

    public static Double createDouble(String val) {
        return Double.valueOf(val);
    }

    public static Integer createInteger(String val) {
        return Integer.decode(val);
    }

    public static byte toByte(String str) {
        return toByte(str, (byte) 0);
    }

    public static byte toByte(String str, byte defaultValue) {
        if (str == null) {
            return defaultValue;
        }
        try {
            return Byte.parseByte(str);
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    public static short toShort(String str) {
        return toShort(str, (short) 0);
    }

    public static short toShort(String str, short defaultValue) {
        if (str == null) {
            return defaultValue;
        }
        try {
            return Short.parseShort(str);
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    public static Long createLong(String str) {
        if (str == null) {
            return null;
        }
        return Long.valueOf(str);
    }

    public static BigInteger createBigInteger(String str) {
        if (str == null) {
            return null;
        }
        return new BigInteger(str);
    }

    public static BigDecimal createBigDecimal(String str) {
        if (str == null) {
            return null;
        }
        if (StringUtil.isEmpty(str)) {
            throw new NumberFormatException("A blank string is not a valid number");
        }
        return new BigDecimal(str);
    }

    public static long min(long[] array) {
        if (array == null)
            throw new IllegalArgumentException("The Array must not be null");
        if (array.length == 0) {
            throw new IllegalArgumentException("Array cannot be empty.");
        }

        long min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) {
                min = array[i];
            }
        }

        return min;
    }

    public static int min(int[] array) {
        if (array == null)
            throw new IllegalArgumentException("The Array must not be null");
        if (array.length == 0) {
            throw new IllegalArgumentException("Array cannot be empty.");
        }

        int min = array[0];
        for (int j = 1; j < array.length; j++) {
            if (array[j] < min) {
                min = array[j];
            }
        }

        return min;
    }

    public static short min(short[] array) {
        if (array == null)
            throw new IllegalArgumentException("The Array must not be null");
        if (array.length == 0) {
            throw new IllegalArgumentException("Array cannot be empty.");
        }

        short min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) {
                min = array[i];
            }
        }

        return min;
    }

    public static double min(double[] array) {
        if (array == null)
            throw new IllegalArgumentException("The Array must not be null");
        if (array.length == 0) {
            throw new IllegalArgumentException("Array cannot be empty.");
        }

        double min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (Double.isNaN(array[i])) {
                return 0;
            }
            if (array[i] < min) {
                min = array[i];
            }
        }

        return min;
    }

    public static boolean isDigits(String str) {
        if (StringUtil.isEmpty(str)) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static byte[] longToBytesBig(long value) {
        byte[] src = new byte[8];
        src[0] = (byte) (int) (value >> 56 & 0xFFL);
        src[1] = (byte) (int) (value >> 48 & 0xFFL);
        src[2] = (byte) (int) (value >> 40 & 0xFFL);
        src[3] = (byte) (int) (value >> 32 & 0xFFL);
        src[4] = (byte) (int) (value >> 24 & 0xFFL);
        src[5] = (byte) (int) (value >> 16 & 0xFFL);
        src[6] = (byte) (int) (value >> 8 & 0xFFL);
        src[7] = (byte) (int) (value & 0xFFL);
        return src;
    }

    public static byte[] longToBytesLittle(long value) {
        byte[] src = new byte[8];
        src[7] = (byte) (int) (value >> 56 & 0xFFL);
        src[6] = (byte) (int) (value >> 48 & 0xFFL);
        src[5] = (byte) (int) (value >> 40 & 0xFFL);
        src[4] = (byte) (int) (value >> 32 & 0xFFL);
        src[3] = (byte) (int) (value >> 24 & 0xFFL);
        src[2] = (byte) (int) (value >> 16 & 0xFFL);
        src[1] = (byte) (int) (value >> 8 & 0xFFL);
        src[0] = (byte) (int) (value & 0xFFL);
        return src;
    }

    public static byte[] intToBytesBig(int value) {
        byte[] src = new byte[4];
        src[0] = (byte) (value >> 24 & 0xFF);
        src[1] = (byte) (value >> 16 & 0xFF);
        src[2] = (byte) (value >> 8 & 0xFF);
        src[3] = (byte) (value & 0xFF);
        return src;
    }

    public static byte[] intToBytesLittle(int value) {
        byte[] src = new byte[4];
        src[3] = (byte) (value >> 24 & 0xFF);
        src[2] = (byte) (value >> 16 & 0xFF);
        src[1] = (byte) (value >> 8 & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }

    public static String getSimpleBinString(int number) {
        StringBuilder sBuilder = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            sBuilder.append(number);
            number >>>= 1;
        }
        int index = sBuilder.reverse().indexOf("1");
        return sBuilder.substring(index);
    }

    public static int ByteToInt(byte b) {
        return b & 0xFF;
    }
}
