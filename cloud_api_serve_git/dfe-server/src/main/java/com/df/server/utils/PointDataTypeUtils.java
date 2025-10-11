package com.df.server.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * @作者 yangximing
 * @date 2022/8/16 9:49
 **/
@Slf4j
public class PointDataTypeUtils {

    public enum DataType {
        CAMERA("camera"),
        ROBOT("robot"),
        UAV("uav"),
        VOICE("voice"),
        ONLINE("online");

        private String type;

        DataType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    /**
     * 给字符串的左补0或右补0
     *
     * @param str    要处理的字符串
     * @param length 补0后字符串总长度
     * @param type   1-左补0  2-右补0
     * @return
     */
    public static String addZeroForStr(String str, int length, int type) {
        int strLen = str.length();
        if (strLen < length) {
            while (strLen < length) {
                StringBuffer sb = new StringBuffer();
                if (type == 1) {
                    // 左补0
                    sb.append("0").append(str);
                } else if (type == 2) {
                    //右补0
                    sb.append(str).append("0");
                }
                str = sb.toString();
                strLen = str.length();
            }
        }
        return str;
    }

    /**
     * 十进制 转 二进制
     *
     * @param datatype
     * @return
     */
    public static String intToHexString(int datatype) {
        String hexStr = Integer.toBinaryString(datatype);
        return addZeroForStr(hexStr, 8, 1);
    }

    /**
     * 二进制转化为十进制
     *
     * @param hexStr
     * @return
     */
    public static int hexToInt(String hexStr) {
        return Integer.parseInt(hexStr, 2);
    }

    /**
     * 根据二进制字符串 判断数据类型
     *
     * @param hexStr
     * @return
     */
    public static ArrayList<String> judgeHexDataType(String hexStr) {
        ArrayList<String> dataTypes = new ArrayList<>();

        char[] data = hexStr.toCharArray();
        if (data.length < 5) {
            log.error("数据类型错误：" + hexStr);
            return dataTypes;
        }
        if (data[data.length - 1] == '1') {
            dataTypes.add(DataType.CAMERA.getType());
        }
        if (data[data.length - 2] == '1') {
            dataTypes.add(DataType.ROBOT.getType());
        }
        if (data[data.length - 3] == '1') {
            dataTypes.add(DataType.UAV.getType());
        }
        if (data[data.length - 4] == '1') {
            dataTypes.add(DataType.VOICE.getType());
        }
        if (data[data.length - 5] == '1') {
            dataTypes.add(DataType.ONLINE.getType());
        }
        return dataTypes;
    }

    /**
     * 根据整型数据判断数据类型
     *
     * @param datatype
     * @return
     */
    public static ArrayList<String> judgeIntDataType(int datatype) {
        String hexStr = intToHexString(datatype);
        return judgeHexDataType(hexStr);
    }

    /**
     * 根据数据类型数字判断对应整型数据
     *
     * @param list
     * @return
     */
    public static int judgeDataTypeList(List<String> list) {

        char hexArray[] = {'0', '0', '0', '0', '0', '0', '0', '0'};
        for (int i = 0; i < list.size(); i++) {
            String dataType = list.get(i);
            if (dataType.equals(DataType.CAMERA.getType())) {
                hexArray[7] = '1';
            }
            if (dataType.equals(DataType.ROBOT.getType())) {
                hexArray[6] = '1';
            }
            if (dataType.equals(DataType.UAV.getType())) {
                hexArray[5] = '1';
            }
            if (dataType.equals(DataType.VOICE.getType())) {
                hexArray[4] = '1';
            }
            if (dataType.equals(DataType.ONLINE.getType())) {
                hexArray[3] = '1';
            }

        }
        String hexStr = new String(hexArray);


        return hexToInt(hexStr);
    }

    /**
     * 根据数据类型数字判断对应整型数据
     *
     * @param list
     * @return
     */
    public static ArrayList<Integer> judgeDataTypeListEx(List<String> list) {

        ArrayList<Integer> arrayList = new ArrayList<>();
        LinkedHashSet<Integer> set = new LinkedHashSet<>();

        char hexArray[] = {'0', '0', '0', '0', '0', '0', '0', '0'};
        for (int i = 0; i < list.size(); i++) {
            String dataType = list.get(i);
            if (dataType.equals(DataType.CAMERA.getType())) {
                hexArray[7] = '1';
            }
            if (dataType.equals(DataType.ROBOT.getType())) {
                hexArray[6] = '1';
            }
            if (dataType.equals(DataType.UAV.getType())) {
                hexArray[5] = '1';
            }
            if (dataType.equals(DataType.VOICE.getType())) {
                hexArray[4] = '1';
            }
            if (dataType.equals(DataType.ONLINE.getType())) {
                hexArray[3] = '1';
            }
        }


        for (int i = 0; i < 31; i++) {
            // System.out.println(intToHexString(i));

            char dsthexArray[] = {'0', '0', '0', '0', '0', '0', '0', '0'};
            char[] sarry = intToHexString(i).toCharArray();

            for (int j = 0; j < 8; j++) {
                if (hexArray[j] == '1' && sarry[j] == '1') {
                    dsthexArray[j] = '1';
                }
            }
            int dstData = hexToInt(new String(dsthexArray));
            //System.out.println(new String(dsthexArray));

            if (dstData > 0) {
                arrayList.add(dstData);

            }
        }

        for (Integer s : arrayList) {
            set.add(s);
        }
        arrayList.clear();
        for (Integer s : set) {
            // 再将set中的值储存进list中，方便使用
            arrayList.add(s);
        }

        return arrayList;
    }


    public static void main(String[] args) {
        int type = 4;
        String s = intToHexString(type);
        System.out.println(type + "的二进制：" + s);
        ArrayList<String> dataTypes = judgeIntDataType(type);
        System.out.println(Arrays.toString(new ArrayList[]{dataTypes}));
        //结果：
        // 1摄像机 2机器人 3摄像机+机器人 4无人机 5摄像机+无人机 8声纹 16在线监测

        //yxm 测试接口
        List<String> list = new LinkedList<>();
        String d1 = DataType.CAMERA.getType();
        list.add(d1);
        String d2 = DataType.UAV.getType();
        list.add(d2);
        String d3 = DataType.VOICE.getType();
        list.add(d3);
        System.out.println(judgeDataTypeListEx(list));
    }
}
