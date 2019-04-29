package org.kivy.android.jnius;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class NativeInvocationHandler implements InvocationHandler {

    static boolean DEBUG = false;
    private long ptr;

    native Object invoke0(Object obj, Method method, Object[] objArr);

    public NativeInvocationHandler(long ptr) {
        this.ptr = ptr;
    }

    public Object invoke(Object proxy, Method method, Object[] args) {
        if (DEBUG) {
            System.out.print("+ java:invoke(<proxy>, ");
            System.out.print(method);
            System.out.print(", ");
            System.out.print(args);
            System.out.println(")");
            System.out.flush();
        }
        Object ret = invoke0(proxy, method, args);
        if (DEBUG) {
            System.out.print("+ java:invoke returned: ");
            System.out.println(ret);
        }
        return ret;
    }

    public long getPythonObjectPointer() {
        return this.ptr;
    }

}
