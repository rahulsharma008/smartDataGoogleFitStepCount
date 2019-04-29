package org.kivy.android.kamranzafar.jtar;

import binder24.com.hydrostar.BuildConfig;
import java.io.File;

public class TarHeader {
    public static final int CHKSUMLEN = 8;
    public static final int GIDLEN = 8;
    public static final byte LF_BLK = (byte) 52;
    public static final byte LF_CHR = (byte) 51;
    public static final byte LF_CONTIG = (byte) 55;
    public static final byte LF_DIR = (byte) 53;
    public static final byte LF_FIFO = (byte) 54;
    public static final byte LF_LINK = (byte) 49;
    public static final byte LF_NORMAL = (byte) 48;
    public static final byte LF_OLDNORM = (byte) 0;
    public static final byte LF_SYMLINK = (byte) 50;
    public static final int MODELEN = 8;
    public static final int MODTIMELEN = 12;
    public static final int NAMELEN = 100;
    public static final int SIZELEN = 12;
    public static final int UIDLEN = 8;
    public static final int USTAR_DEVLEN = 8;
    public static final int USTAR_FILENAME_PREFIX = 155;
    public static final int USTAR_GROUP_NAMELEN = 32;
    public static final String USTAR_MAGIC = "ustar";
    public static final int USTAR_MAGICLEN = 8;
    public static final int USTAR_USER_NAMELEN = 32;
    public int checkSum;
    public int devMajor;
    public int devMinor;
    public int groupId;
    public StringBuffer groupName;
    public byte linkFlag;
    public StringBuffer linkName = new StringBuffer();
    public StringBuffer magic = new StringBuffer(USTAR_MAGIC);
    public long modTime;
    public int mode;
    public StringBuffer name = new StringBuffer();
    public StringBuffer namePrefix;
    public long size;
    public int userId;
    public StringBuffer userName;

    public TarHeader() {
        String user = System.getProperty("user.name", BuildConfig.FLAVOR);
        if (user.length() > 31) {
            user = user.substring(0, 31);
        }
        this.userId = 0;
        this.groupId = 0;
        this.userName = new StringBuffer(user);
        this.groupName = new StringBuffer(BuildConfig.FLAVOR);
        this.namePrefix = new StringBuffer();
    }

    public static StringBuffer parseName(byte[] header, int offset, int length) {
        StringBuffer result = new StringBuffer(length);
        int end = offset + length;
        int i = offset;
        while (i < end && header[i] != (byte) 0) {
            result.append((char) header[i]);
            i++;
        }
        return result;
    }

    public static int getNameBytes(StringBuffer name, byte[] buf, int offset, int length) {
        int i = 0;
        while (i < length && i < name.length()) {
            buf[offset + i] = (byte) name.charAt(i);
            i++;
        }
        while (i < length) {
            buf[offset + i] = (byte) 0;
            i++;
        }
        return offset + length;
    }

    public static TarHeader createHeader(String entryName, long size, long modTime, boolean dir) {
        String name = TarUtils.trim(entryName.replace(File.separatorChar, '/'), '/');
        TarHeader header = new TarHeader();
        header.linkName = new StringBuffer(BuildConfig.FLAVOR);
        if (name.length() > 100) {
            header.namePrefix = new StringBuffer(name.substring(0, name.lastIndexOf(47)));
            header.name = new StringBuffer(name.substring(name.lastIndexOf(47) + 1));
        } else {
            header.name = new StringBuffer(name);
        }
        if (dir) {
            header.mode = 16877;
            header.linkFlag = LF_DIR;
            if (header.name.charAt(header.name.length() - 1) != '/') {
                header.name.append("/");
            }
            header.size = 0;
        } else {
            header.mode = 33188;
            header.linkFlag = LF_NORMAL;
            header.size = size;
        }
        header.modTime = modTime;
        header.checkSum = 0;
        header.devMajor = 0;
        header.devMinor = 0;
        return header;
    }
}
