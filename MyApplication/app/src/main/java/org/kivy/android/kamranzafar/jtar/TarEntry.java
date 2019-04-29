package org.kivy.android.kamranzafar.jtar;

import binder24.com.hydrostar.BuildConfig;
import java.io.File;
import java.util.Date;

public class TarEntry {
    protected File file;
    protected TarHeader header;

    private TarEntry() {
        this.file = null;
        this.header = new TarHeader();
    }

    public TarEntry(File file, String entryName) {
        this();
        this.file = file;
        extractTarHeader(entryName);
    }

    public TarEntry(byte[] headerBuf) {
        this();
        parseTarHeader(headerBuf);
    }

    public TarEntry(TarHeader header) {
        this.file = null;
        this.header = header;
    }

    public boolean equals(TarEntry it) {
        return this.header.name.toString().equals(it.header.name.toString());
    }

    public boolean isDescendent(TarEntry desc) {
        return desc.header.name.toString().startsWith(this.header.name.toString());
    }

    public TarHeader getHeader() {
        return this.header;
    }

    public String getName() {
        String name = this.header.name.toString();
        if (this.header.namePrefix == null || this.header.namePrefix.toString().equals(BuildConfig.FLAVOR)) {
            return name;
        }
        return this.header.namePrefix.toString() + "/" + name;
    }

    public void setName(String name) {
        this.header.name = new StringBuffer(name);
    }

    public int getUserId() {
        return this.header.userId;
    }

    public void setUserId(int userId) {
        this.header.userId = userId;
    }

    public int getGroupId() {
        return this.header.groupId;
    }

    public void setGroupId(int groupId) {
        this.header.groupId = groupId;
    }

    public String getUserName() {
        return this.header.userName.toString();
    }

    public void setUserName(String userName) {
        this.header.userName = new StringBuffer(userName);
    }

    public String getGroupName() {
        return this.header.groupName.toString();
    }

    public void setGroupName(String groupName) {
        this.header.groupName = new StringBuffer(groupName);
    }

    public void setIds(int userId, int groupId) {
        setUserId(userId);
        setGroupId(groupId);
    }

    public void setModTime(long time) {
        this.header.modTime = time / 1000;
    }

    public void setModTime(Date time) {
        this.header.modTime = time.getTime() / 1000;
    }

    public Date getModTime() {
        return new Date(this.header.modTime * 1000);
    }

    public File getFile() {
        return this.file;
    }

    public long getSize() {
        return this.header.size;
    }

    public void setSize(long size) {
        this.header.size = size;
    }

    public boolean isDirectory() {
        if (this.file != null) {
            return this.file.isDirectory();
        }
        if (this.header == null || (this.header.linkFlag != TarHeader.LF_DIR && !this.header.name.toString().endsWith("/"))) {
            return false;
        }
        return true;
    }

    public void extractTarHeader(String entryName) {
        this.header = TarHeader.createHeader(entryName, this.file.length(), this.file.lastModified() / 1000, this.file.isDirectory());
    }

    public long computeCheckSum(byte[] buf) {
        long sum = 0;
        for (byte b : buf) {
            sum += (long) (b & 255);
        }
        return sum;
    }

    public void writeEntryHeader(byte[] outbuf) {
        int offset = Octal.getLongOctalBytes(this.header.modTime, outbuf, Octal.getLongOctalBytes(this.header.size, outbuf, Octal.getOctalBytes((long) this.header.groupId, outbuf, Octal.getOctalBytes((long) this.header.userId, outbuf, Octal.getOctalBytes((long) this.header.mode, outbuf, TarHeader.getNameBytes(this.header.name, outbuf, 0, 100), 8), 8), 8), 12), 12);
        int csOffset = offset;
        int c = 0;
        int offset2 = offset;
        while (c < 8) {
            offset = offset2 + 1;
            outbuf[offset2] = (byte) 32;
            c++;
            offset2 = offset;
        }
        offset = offset2 + 1;
        outbuf[offset2] = this.header.linkFlag;
        offset = TarHeader.getNameBytes(this.header.namePrefix, outbuf, Octal.getOctalBytes((long) this.header.devMinor, outbuf, Octal.getOctalBytes((long) this.header.devMajor, outbuf, TarHeader.getNameBytes(this.header.groupName, outbuf, TarHeader.getNameBytes(this.header.userName, outbuf, TarHeader.getNameBytes(this.header.magic, outbuf, TarHeader.getNameBytes(this.header.linkName, outbuf, offset, 100), 8), 32), 32), 8), 8), TarHeader.USTAR_FILENAME_PREFIX);
        while (offset < outbuf.length) {
            offset2 = offset + 1;
            outbuf[offset] = (byte) 0;
            offset = offset2;
        }
        Octal.getCheckSumOctalBytes(computeCheckSum(outbuf), outbuf, csOffset, 8);
    }

    public void parseTarHeader(byte[] bh) {
        this.header.name = TarHeader.parseName(bh, 0, 100);
        int offset = 0 + 100;
        this.header.mode = (int) Octal.parseOctal(bh, offset, 8);
        offset += 8;
        this.header.userId = (int) Octal.parseOctal(bh, offset, 8);
        offset += 8;
        this.header.groupId = (int) Octal.parseOctal(bh, offset, 8);
        offset += 8;
        this.header.size = Octal.parseOctal(bh, offset, 12);
        offset += 12;
        this.header.modTime = Octal.parseOctal(bh, offset, 12);
        offset += 12;
        this.header.checkSum = (int) Octal.parseOctal(bh, offset, 8);
        offset += 8;
        int offset2 = offset + 1;
        this.header.linkFlag = bh[offset];
        this.header.linkName = TarHeader.parseName(bh, offset2, 100);
        offset = offset2 + 100;
        this.header.magic = TarHeader.parseName(bh, offset, 8);
        offset += 8;
        this.header.userName = TarHeader.parseName(bh, offset, 32);
        offset += 32;
        this.header.groupName = TarHeader.parseName(bh, offset, 32);
        offset += 32;
        this.header.devMajor = (int) Octal.parseOctal(bh, offset, 8);
        offset += 8;
        this.header.devMinor = (int) Octal.parseOctal(bh, offset, 8);
        offset += 8;
        this.header.namePrefix = TarHeader.parseName(bh, offset, TarHeader.USTAR_FILENAME_PREFIX);
    }
}
