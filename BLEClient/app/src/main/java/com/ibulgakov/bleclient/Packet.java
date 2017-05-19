package com.ibulgakov.bleclient;

/**
 * Created by User on 03.05.2017.
 */

public class Packet {
    public static final int OFFSET_SIGNATURE    = 0;
    public static final int OFFSET_TYPE         = 1;
    public static final int OFFSET_SEQUENCE     = 2;
    public static final int OFFSET_DATA         = 3;
    public static final int OFFSET_CONTROL_SUM  = 4;

    public static final byte TYPE_ECG      = 1;
    public static final byte TYPE_POSTURE  = 2;
    public static final byte TYPE_CONFIRM  = 5;

    private byte[] data;

    public Packet(byte[] data) {
        this.data = data;
    }

    public byte type() {
        return data[OFFSET_TYPE];
    }

    public short getShortByIndex(int index) {
        index &= 0x7;
        index = index*2 + OFFSET_DATA;

        byte low = data[index];
        byte high = data[index + 1];

        return (short)(low + high*256);
    }
}
