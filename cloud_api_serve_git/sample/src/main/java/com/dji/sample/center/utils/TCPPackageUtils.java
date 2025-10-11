package com.dji.sample.center.utils;


import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

/**
 * TCP报文工具类
 * creator: 姜学云 2021-7-12 11:35
 */
@Slf4j
public class TCPPackageUtils {

    public static boolean readAndCheckEndFlag(InputStream inputStream) {
        byte recvEndFlag[] = new byte[2];
        try {
            if (inputStream.read(recvEndFlag, 0, 2) != 2) {
                log.error("校验结束符： 长度不符");
                return false;
            }
        } catch (IOException e) {
            log.error("校验结束符时，异常： {}", e);
            return false;
        }

        //同起始标志符,EB90(小头字节序)
        if (recvEndFlag[0] != (byte) 0xeb || recvEndFlag[1] != (byte) 0x90) {
            log.error("校验结束符： 内容不符");
            return false;
        }

        return true;
    }

    /**
     * 读取报文中的国标xml
     *
     * @author 姜学云
     * @date 2022/1/17 9:16
     **/
    public static String readXmlString(InputStream inputStream, int length) {
        try {
            int errCnt = 0;
            int readLeft = length;
            byte[] readByte = new byte[length];
            int readPos = 0;

            while (errCnt <= 3 && readLeft > 0) {
                int len = inputStream.read(readByte, readPos, readLeft);
                if (len > 0) {
                    readPos += len;
                    readLeft -= len;
                } else {
                    errCnt++;
                }
            }

            return new String(readByte, "utf-8");
        } catch (IOException e) {
            log.error("读取xml报文时，发生异常：{}", e);
            return null;
        }
    }
}
