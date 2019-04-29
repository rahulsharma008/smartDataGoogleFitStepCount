package org.kivy.android.kamranzafar.jtar;

public class Octal {
    public static long parseOctal(byte[] header, int offset, int length) {
        long result = 0;
        boolean stillPadding = true;
        int end = offset + length;
        int i = offset;
        while (i < end && header[i] != (byte) 0) {
            if (header[i] == (byte) 32 || header[i] == TarHeader.LF_NORMAL) {
                if (!stillPadding) {
                    if (header[i] == (byte) 32) {
                        break;
                    }
                }
                continue;
            }
            stillPadding = false;
            result = (result << 3) + ((long) (header[i] - 48));
            i++;
        }
        return result;
    }

    public static int getOctalBytes(long value, byte[] buf, int offset, int length) {
        int idx = length - 1;
        buf[offset + idx] = (byte) 0;
        idx--;
        buf[offset + idx] = (byte) 32;
        idx--;
        if (value == 0) {
            buf[offset + idx] = TarHeader.LF_NORMAL;
            idx--;
        } else {
            long val = value;
            while (idx >= 0 && val > 0) {
                buf[offset + idx] = (byte) (((byte) ((int) (7 & val))) + 48);
                val >>= 3;
                idx--;
            }
        }
        while (idx >= 0) {
            buf[offset + idx] = (byte) 32;
            idx--;
        }
        return offset + length;
    }

    public static int getCheckSumOctalBytes(long value, byte[] buf, int offset, int length) {
        getOctalBytes(value, buf, offset, length);
        buf[(offset + length) - 1] = (byte) 32;
        buf[(offset + length) - 2] = (byte) 0;
        return offset + length;
    }

    public static int getLongOctalBytes(long value, byte[] buf, int offset, int length) {
        byte[] temp = new byte[(length + 1)];
        getOctalBytes(value, temp, 0, length + 1);
        System.arraycopy(temp, 0, buf, offset, length);
        return offset + length;
    }
}
