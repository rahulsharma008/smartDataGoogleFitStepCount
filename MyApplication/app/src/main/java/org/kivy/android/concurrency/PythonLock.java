package org.kivy.android.concurrency;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PythonLock {
    private final Lock lock = new ReentrantLock();

    public void acquire() {
        this.lock.lock();
    }

    public void release() {
        this.lock.unlock();
    }
}
