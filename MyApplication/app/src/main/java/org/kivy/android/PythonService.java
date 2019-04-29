package org.kivy.android;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

public class PythonService extends Service implements Runnable {
    public static PythonService mService = null;
    private String androidArgument;
    private String androidPrivate;
    private boolean autoRestartService = false;
    private String pythonHome;
    private String pythonName;
    private String pythonPath;
    private String pythonServiceArgument;
    private Thread pythonThread = null;
    private String serviceEntrypoint;
    private Intent startIntent = null;

    public static native void nativeStart(String str, String str2, String str3, String str4, String str5, String str6, String str7);

    public void setAutoRestartService(boolean restart) {
        this.autoRestartService = restart;
    }

    public boolean canDisplayNotification() {
        return true;
    }

    public int startType() {
        return 2;
    }

    public IBinder onBind(Intent arg0) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (this.pythonThread != null) {
            Log.v("python service", "service exists, do not start again");
            return 2;
        }
        this.startIntent = intent;
        Bundle extras = intent.getExtras();
        this.androidPrivate = extras.getString("androidPrivate");
        this.androidArgument = extras.getString("androidArgument");
        this.serviceEntrypoint = extras.getString("serviceEntrypoint");
        this.pythonName = extras.getString("pythonName");
        this.pythonHome = extras.getString("pythonHome");
        this.pythonPath = extras.getString("pythonPath");
        this.pythonServiceArgument = extras.getString("pythonServiceArgument");
        this.pythonThread = new Thread(this);
        this.pythonThread.start();
        if (canDisplayNotification()) {
            doStartForeground(extras);
        }
        return startType();
    }

    protected void doStartForeground(Bundle extras) {
        Notification notification;
        String serviceTitle = extras.getString("serviceTitle");
        String serviceDescription = extras.getString("serviceDescription");
        Context context = getApplicationContext();
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, new Intent(context, PythonActivity.class), 134217728);
        if (VERSION.SDK_INT < 11) {
            notification = new Notification(context.getApplicationInfo().icon, serviceTitle, System.currentTimeMillis());
            try {
                notification.getClass().getMethod("setLatestEventInfo", new Class[]{Context.class, CharSequence.class, CharSequence.class, PendingIntent.class}).invoke(notification, new Object[]{context, serviceTitle, serviceDescription, pIntent});
            } catch (NoSuchMethodException e) {
            } catch (IllegalAccessException e2) {
            } catch (IllegalArgumentException e3) {
            } catch (InvocationTargetException e4) {
            }
        } else {
            Builder builder = new Builder(context);
            builder.setContentTitle(serviceTitle);
            builder.setContentText(serviceDescription);
            builder.setContentIntent(pIntent);
            builder.setSmallIcon(context.getApplicationInfo().icon);
            notification = builder.build();
        }
        startForeground(1, notification);
    }

    public void onDestroy() {
        super.onDestroy();
        this.pythonThread = null;
        if (this.autoRestartService && this.startIntent != null) {
            Log.v("python service", "service restart requested");
            startService(this.startIntent);
        }
        Process.killProcess(Process.myPid());
    }

    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    public void run() {
        PythonUtil.loadLibraries(new File(getFilesDir().getAbsolutePath() + "/app"));
        mService = this;
        nativeStart(this.androidPrivate, this.androidArgument, this.serviceEntrypoint, this.pythonName, this.pythonHome, this.pythonPath, this.pythonServiceArgument);
        stopSelf();
    }
}
