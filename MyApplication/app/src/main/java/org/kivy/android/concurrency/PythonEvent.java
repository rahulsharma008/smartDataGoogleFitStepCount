package org.kivy.android.concurrency;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PythonEvent {
    private final Condition cond = this.lock.newCondition();
    private boolean flag = false;
    private final Lock lock = new ReentrantLock();

    public void set() {
        this.lock.lock();
        try {
            this.flag = true;
            this.cond.signalAll();
        } finally {
            this.lock.unlock();
        }
    }

    public void wait_() throws InterruptedException {
        this.lock.lock();
        while (!this.flag) {
            try {
                this.cond.await();
            } finally {
                this.lock.unlock();
            }
        }
    }

    public void clear() {
        this.lock.lock();
        try {
            this.flag = false;
            this.cond.signalAll();
        } finally {
            this.lock.unlock();
        }
    }
}
