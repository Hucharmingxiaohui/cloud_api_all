package com.dji.sample.df.windDf.utils;

import net.sourceforge.pinyin4j.PinyinHelper;

public class FileNameUtils {

    /**
     * 将中文文件名转换为拼音首字母
     * 例如：A叶片-迎风面-1.jpg → AYP-YFM-1.jpg
     */
    public static String convertChineseToPinyinInitials(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return fileName;
        }

        // 分离文件名和扩展名
        int dotIndex = fileName.lastIndexOf('.');
        String nameWithoutExt = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        String extension = dotIndex > 0 ? fileName.substring(dotIndex) : "";

        // 转换文件名主体
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < nameWithoutExt.length(); i++) {
            char ch = nameWithoutExt.charAt(i);

            if (isChinese(ch)) {
                // 获取拼音首字母
                String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(ch);
                if (pinyinArray != null && pinyinArray.length > 0) {
                    // 取第一个拼音的首字母（大写）
                    result.append(Character.toUpperCase(pinyinArray[0].charAt(0)));
                } else {
                    // 无法转换的中文字符，保留原字符
                    result.append(ch);
                }
            } else if (ch == ' ') {
                // 空格替换为下划线
                result.append('_');
            } else {
                // 其他非中文字符保留原样
                result.append(ch);
            }
        }

        // 拼接扩展名后返回
        return result.append(extension).toString();
    }

    /**
     * 判断字符是否为中文
     */
    private static boolean isChinese(char ch) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(ch);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }

    /**
     * 更优雅的版本：保留分隔符，按单词分组转换
     * 例如：A叶片-迎风面-1.jpg → AYP-YFM-1.jpg
     */
    public static String convertFileNameWithSeparators(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return fileName;
        }

        int dotIndex = fileName.lastIndexOf('.');
        String nameWithoutExt = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        String extension = dotIndex > 0 ? fileName.substring(dotIndex) : "";

        StringBuilder result = new StringBuilder();
        StringBuilder currentWord = new StringBuilder();

        for (int i = 0; i < nameWithoutExt.length(); i++) {
            char ch = nameWithoutExt.charAt(i);

            if (isChinese(ch)) {
                // 中文转换为拼音首字母
                String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(ch);
                if (pinyinArray != null && pinyinArray.length > 0) {
                    currentWord.append(Character.toUpperCase(pinyinArray[0].charAt(0)));
                } else {
                    currentWord.append(ch);
                }
            } else if (Character.isLetterOrDigit(ch)) {
                // 字母或数字直接保留
                currentWord.append(ch);
            } else {
                // 分隔符（-、_、空格等）
                if (currentWord.length() > 0) {
                    result.append(currentWord);
                    currentWord.setLength(0); // 清空
                }
                result.append(ch);
            }
        }

        // 处理最后一个单词
        if (currentWord.length() > 0) {
            result.append(currentWord);
        }

        return result.append(extension).toString();
    }
}
