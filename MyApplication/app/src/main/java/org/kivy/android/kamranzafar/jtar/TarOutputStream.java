package org.kivy.android.kamranzafar.jtar;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class TarOutputStream extends OutputStream {
    private long bytesWritten;
    private TarEntry currentEntry;
    private long currentFileSize;
    private final OutputStream out;

    public TarOutputStream(OutputStream out) {
        this.out = out;
        this.bytesWritten = 0;
        this.currentFileSize = 0;
    }

    public TarOutputStream(File fout) throws FileNotFoundException {
        this.out = new BufferedOutputStream(new FileOutputStream(fout));
        this.bytesWritten = 0;
        this.currentFileSize = 0;
    }

    public TarOutputStream(File fout, boolean append) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(fout, "rw");
        long fileSize = fout.length();
        if (append && fileSize > 1024) {
            raf.seek(fileSize - 1024);
        }
        this.out = new BufferedOutputStream(new FileOutputStream(raf.getFD()));
    }

    public void close() throws IOException {
        closeCurrentEntry();
        write(new byte[TarConstants.EOF_BLOCK]);
        this.out.close();
    }

    public void write(int b) throws IOException {
        this.out.write(b);
        this.bytesWritten++;
        if (this.currentEntry != null) {
            this.currentFileSize++;
        }
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (this.currentEntry == null || this.currentEntry.isDirectory() || this.currentEntry.getSize() >= this.currentFileSize + ((long) len)) {
            this.out.write(b, off, len);
            this.bytesWritten += (long) len;
            if (this.currentEntry != null) {
                this.currentFileSize += (long) len;
                return;
            }
            return;
        }
        throw new IOException("The current entry[" + this.currentEntry.getName() + "] size[" + this.currentEntry.getSize() + "] is smaller than the bytes[" + (this.currentFileSize + ((long) len)) + "] being written.");
    }

    public void putNextEntry(TarEntry entry) throws IOException {
        closeCurrentEntry();
        byte[] header = new byte[512];
        entry.writeEntryHeader(header);
        write(header);
        this.currentEntry = entry;
    }

    protected void closeCurrentEntry() throws IOException {
        if (this.currentEntry == null) {
            return;
        }
        if (this.currentEntry.getSize() > this.currentFileSize) {
            throw new IOException("The current entry[" + this.currentEntry.getName() + "] of size[" + this.currentEntry.getSize() + "] has not been fully written.");
        }
        this.currentEntry = null;
        this.currentFileSize = 0;
        pad();
    }

    protected void pad() throws IOException {
        if (this.bytesWritten > 0) {
            int extra = (int) (this.bytesWritten % 512);
            if (extra > 0) {
                write(new byte[(512 - extra)]);
            }
        }
    }
}
