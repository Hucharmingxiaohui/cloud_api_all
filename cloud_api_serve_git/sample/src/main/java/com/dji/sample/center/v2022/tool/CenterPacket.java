package com.dji.sample.center.v2022.tool;


import com.dji.sample.center.utils.NumberUtils;

import java.nio.ByteBuffer;

/**
 * 参考附录H.3
 * 巡视主机-上级系统通信规约
 */
public class CenterPacket {
    private final byte[] start = {-21, -112};

    public long serialNumber = 0L;

    public long responseSerialNumber = 0L;

    private byte sessionType;

    private int dataLength;

    private byte[] data;

    private final byte[] end = {-21, -112};

    public byte[] getStart() {
        return this.start;
    }

    public byte getSessionType() {
        return this.sessionType;
    }

    public void setSessionType(byte sessionType) {
        this.sessionType = sessionType;
    }

    public int getDataLength() {
        return this.dataLength;
    }

    public void setDataLength(int dataLength) {
        this.dataLength = dataLength;
    }

    public byte[] getData() {
        return this.data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public byte[] getEnd() {
        return this.end;
    }

    public byte[] getBytes() {
        int length = 23 + this.dataLength + 2;

        ByteBuffer buffer = ByteBuffer.allocate(length);
        buffer.put(this.start);
        buffer.put(NumberUtils.longToBytesLittle(this.serialNumber));
        buffer.put(NumberUtils.longToBytesLittle(this.responseSerialNumber));
        buffer.put(this.sessionType);
        buffer.put(NumberUtils.intToBytesLittle(this.dataLength));
        buffer.put(this.data);
        buffer.put(this.end);

        return buffer.array();
    }
}


