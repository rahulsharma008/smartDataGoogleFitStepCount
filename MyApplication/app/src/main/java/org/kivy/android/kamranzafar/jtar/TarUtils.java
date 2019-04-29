package org.kivy.android.kamranzafar.jtar;

import java.io.File;

public class TarUtils {
    public static long calculateTarSize(File path) {
        return tarSize(path) + 1024;
    }

    private static long tarSize(File dir) {
        if (dir.isFile()) {
            return entrySize(dir.length());
        }
        File[] subFiles = dir.listFiles();
        if (subFiles == null || subFiles.length <= 0) {
            return 512;
        }
        int length = subFiles.length;
        int i = 0;
        long size = 0;
        while (i < length) {
            long size2;
            File file = subFiles[i];
            if (file.isFile()) {
                size2 = size + entrySize(file.length());
            } else {
                size2 = size + tarSize(file);
            }
            i++;
            size = size2;
        }
        return size;
    }

    private static long entrySize(long fileSize) {
        long size = (0 + 512) + fileSize;
        long extra = size % 512;
        if (extra > 0) {
            return size + (512 - extra);
        }
        return size;
    }

    public static String trim(String s, char c) {
        StringBuffer tmp = new StringBuffer(s);
        int i = 0;
        while (i < tmp.length() && tmp.charAt(i) == c) {
            tmp.deleteCharAt(i);
            i++;
        }
        i = tmp.length() - 1;
        while (i >= 0 && tmp.charAt(i) == c) {
            tmp.deleteCharAt(i);
            i--;
        }
        return tmp.toString();
    }
}
