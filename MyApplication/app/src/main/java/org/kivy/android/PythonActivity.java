package org.kivy.android;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.kivy.android.launcher.Project;
import org.kivy.android.libsdl.app.SDLActivity;
import org.kivy.android.renpy.android.AssetExtract;
import org.kivy.android.renpy.android.ResourceManager;

public class PythonActivity extends SDLActivity {

    private static final String TAG = "PythonActivity";
    public static PythonActivity mActivity = null;
    public static ImageView mImageView = null;
    private List<ActivityResultListener> activityResultListeners = null;
    int mLoadingCount = 2;
    private Bundle mMetaData = null;
    private WakeLock mWakeLock = null;
    private List<NewIntentListener> newIntentListeners = null;
    private ResourceManager resourceManager = null;

    public interface ActivityResultListener {
        void onActivityResult(int i, int i2, Intent intent);
    }

    public interface NewIntentListener {
        void onNewIntent(Intent intent);
    }

    private class UnpackFilesTask extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... params) {
            File app_root_file = new File(params[0]);
            Log.v(PythonActivity.TAG, "Ready to unpack");
            PythonActivity.this.unpackData("private", app_root_file);
            return null;
        }

        protected void onPostExecute(String result) {
            PythonActivity.mActivity.finishLoad();
            PythonActivity.mActivity.showLoadingScreen();
            String app_root_dir = PythonActivity.this.getAppRoot();
            if (PythonActivity.this.getIntent() == null || PythonActivity.this.getIntent().getAction() == null || !PythonActivity.this.getIntent().getAction().equals("org.kivy.LAUNCH")) {
                SDLActivity.nativeSetEnv("ANDROID_ENTRYPOINT", "main.pyo");
                SDLActivity.nativeSetEnv("ANDROID_ARGUMENT", app_root_dir);
                SDLActivity.nativeSetEnv("ANDROID_APP_PATH", app_root_dir);
            } else {
                File path = new File(PythonActivity.this.getIntent().getData().getSchemeSpecificPart());
                Project p = Project.scanDirectory(path);
                SDLActivity.nativeSetEnv("ANDROID_ENTRYPOINT", p.dir + "/main.py");
                SDLActivity.nativeSetEnv("ANDROID_ARGUMENT", p.dir);
                SDLActivity.nativeSetEnv("ANDROID_APP_PATH", p.dir);

                if (p != null) {
                    if (p.landscape) {
                        PythonActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    } else {
                        PythonActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    }
                }

                try {
                    FileWriter f = new FileWriter(new File(path, ".launch"));
                    f.write("started");
                    f.close();
                } catch (IOException e) {
                }

            }
            String mFilesDirectory = PythonActivity.mActivity.getFilesDir().getAbsolutePath();
            Log.v(PythonActivity.TAG, "Setting env vars for start.c and Python to use");
            SDLActivity.nativeSetEnv("ANDROID_PRIVATE", mFilesDirectory);
            SDLActivity.nativeSetEnv("ANDROID_UNPACK", app_root_dir);
            SDLActivity.nativeSetEnv("PYTHONHOME", app_root_dir);
            SDLActivity.nativeSetEnv("PYTHONPATH", app_root_dir + ":" + app_root_dir + "/lib");
            SDLActivity.nativeSetEnv("PYTHONOPTIMIZE", "2");
            try {
                Log.v(PythonActivity.TAG, "Access to our meta-data...");
                PythonActivity.mActivity.mMetaData = PythonActivity.mActivity.getPackageManager().getApplicationInfo(PythonActivity.mActivity.getPackageName(), 128).metaData;
                PowerManager pm = (PowerManager) PythonActivity.mActivity.getSystemService("power");
                if (PythonActivity.mActivity.mMetaData.getInt("wakelock") == 1) {
                    PythonActivity.mActivity.mWakeLock = pm.newWakeLock(10, "Screen On");
                    PythonActivity.mActivity.mWakeLock.acquire();
                }
                if (PythonActivity.mActivity.mMetaData.getInt("surface.transparent") != 0) {
                    Log.v(PythonActivity.TAG, "Surface will be transparent.");
                    PythonActivity.getSurface().setZOrderOnTop(true);
                    PythonActivity.getSurface().getHolder().setFormat(-2);
                    return;
                }
                Log.i(PythonActivity.TAG, "Surface will NOT be transparent");
            } catch (NameNotFoundException e2) {
            }
        }

        protected void onPreExecute() {
        }

        protected void onProgressUpdate(Void... values) {
        }
    }

    public String getAppRoot() {
        return getFilesDir().getAbsolutePath() + "/app";
    }

    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "My oncreate running");
        this.resourceManager = new ResourceManager(this);
        Log.v(TAG, "About to do super onCreate");
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Did super onCreate");
        mActivity = this;
        showLoadingScreen();
        new UnpackFilesTask().execute(new String[]{getAppRoot()});
    }

    public void loadLibraries() {
        PythonUtil.loadLibraries(new File(new String(getAppRoot())));
    }

    public void recursiveDelete(File f) {
        if (f.isDirectory()) {
            for (File r : f.listFiles()) {
                recursiveDelete(r);
            }
        }
        f.delete();
    }

    public void toastError(final String msg) {
        final Activity thisActivity = this;
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(thisActivity, msg, 1).show();
            }
        });
        synchronized (this) {
            try {
                wait(1000);
            } catch (InterruptedException e) {
            }
        }
    }

    public void unpackData(String resource, File target) {
        String disk_version;
        FileOutputStream os;
        Log.v(TAG, "UNPACKING!!! " + resource + " " + target.getName());
        String data_version = this.resourceManager.getString(resource + "_version");
        Log.v(TAG, "Data version is " + data_version);
        if (data_version != null) {
            String disk_version_fn = target.getAbsolutePath() + "/" + resource + ".version";
            try {
                byte[] buf = new byte[64];
                InputStream is = new FileInputStream(disk_version_fn);
                String disk_version2 = new String(buf, 0, is.read(buf));
                try {
                    is.close();
                    disk_version = disk_version2;
                } catch (Exception e) {
                    disk_version = disk_version2;
                    disk_version = BuildConfig.FLAVOR;
                    if (data_version.equals(disk_version)) {
                        Log.v(TAG, "Extracting " + resource + " assets.");
                        recursiveDelete(target);
                        target.mkdirs();
                        if (!new AssetExtract(this).extractTar(resource + ".mp3", target.getAbsolutePath())) {
                            toastError("Could not extract " + resource + " data.");
                        }
                        try {
                            new File(target, ".nomedia").createNewFile();
                            os = new FileOutputStream(disk_version_fn);
                            os.write(data_version.getBytes());
                            os.close();
                        } catch (Exception e2) {
                            Log.w("python", e2);
                            return;
                        }
                    }
                }
            } catch (Exception e3) {
                disk_version = BuildConfig.FLAVOR;
                if (data_version.equals(disk_version)) {
                    Log.v(TAG, "Extracting " + resource + " assets.");
                    recursiveDelete(target);
                    target.mkdirs();
                    if (new AssetExtract(this).extractTar(resource + ".mp3", target.getAbsolutePath())) {
                        toastError("Could not extract " + resource + " data.");
                    }
                    new File(target, ".nomedia").createNewFile();
                    os = new FileOutputStream(disk_version_fn);
                    os.write(data_version.getBytes());
                    os.close();
                }
            }
            if (data_version.equals(disk_version)) {
                Log.v(TAG, "Extracting " + resource + " assets.");
                recursiveDelete(target);
                target.mkdirs();
                if (new AssetExtract(this).extractTar(resource + ".mp3", target.getAbsolutePath())) {
                    toastError("Could not extract " + resource + " data.");
                }
                new File(target, ".nomedia").createNewFile();
                os = new FileOutputStream(disk_version_fn);
                os.write(data_version.getBytes());
                os.close();
            }
        }
    }

    public static ViewGroup getLayout() {
        return mLayout;
    }

    public static SurfaceView getSurface() {
        return mSurface;
    }

    public void registerNewIntentListener(NewIntentListener listener) {
        if (this.newIntentListeners == null) {
            this.newIntentListeners = Collections.synchronizedList(new ArrayList());
        }
        this.newIntentListeners.add(listener);
    }

    public void unregisterNewIntentListener(NewIntentListener listener) {
        if (this.newIntentListeners != null) {
            this.newIntentListeners.remove(listener);
        }
    }

    protected void onNewIntent(Intent intent) {
        if (this.newIntentListeners != null) {
            onResume();
            synchronized (this.newIntentListeners) {
                for (NewIntentListener onNewIntent : this.newIntentListeners) {
                    onNewIntent.onNewIntent(intent);
                }
            }
        }
    }

    public void registerActivityResultListener(ActivityResultListener listener) {
        if (this.activityResultListeners == null) {
            this.activityResultListeners = Collections.synchronizedList(new ArrayList());
        }
        this.activityResultListeners.add(listener);
    }

    public void unregisterActivityResultListener(ActivityResultListener listener) {
        if (this.activityResultListeners != null) {
            this.activityResultListeners.remove(listener);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (this.activityResultListeners != null) {
            onResume();
            synchronized (this.activityResultListeners) {
                for (ActivityResultListener onActivityResult : this.activityResultListeners) {
                    onActivityResult.onActivityResult(requestCode, resultCode, intent);
                }
            }
        }
    }

    public static void start_service(String serviceTitle, String serviceDescription, String pythonServiceArgument) {
        Intent serviceIntent = new Intent(mActivity, PythonService.class);
        String argument = mActivity.getFilesDir().getAbsolutePath();
        String filesDirectory = argument;
        String app_root_dir = mActivity.getAppRoot();
        serviceIntent.putExtra("androidPrivate", argument);
        serviceIntent.putExtra("androidArgument", app_root_dir);
        serviceIntent.putExtra("serviceEntrypoint", "service/main.pyo");
        serviceIntent.putExtra("pythonName", "python");
        serviceIntent.putExtra("pythonHome", app_root_dir);
        serviceIntent.putExtra("pythonPath", app_root_dir + ":" + app_root_dir + "/lib");
        serviceIntent.putExtra("serviceTitle", serviceTitle);
        serviceIntent.putExtra("serviceDescription", serviceDescription);
        serviceIntent.putExtra("pythonServiceArgument", pythonServiceArgument);
        mActivity.startService(serviceIntent);
    }

    public static void stop_service() {
        mActivity.stopService(new Intent(mActivity, PythonService.class));
    }

    public void keepActive() {
        if (this.mLoadingCount > 0) {
            this.mLoadingCount--;
            if (this.mLoadingCount == 0) {
                removeLoadingScreen();
            }
        }
    }

    public void removeLoadingScreen() {
        runOnUiThread(new Runnable() {
            public void run() {
                if (PythonActivity.mImageView != null && PythonActivity.mImageView.getParent() != null) {
                    ((ViewGroup) PythonActivity.mImageView.getParent()).removeView(PythonActivity.mImageView);
                    PythonActivity.mImageView = null;
                }
            }
        });
    }

    protected void showLoadingScreen() {
        if (mImageView == null) {
            InputStream is = getResources().openRawResource(this.resourceManager.getIdentifier("presplash", "drawable"));
            Bitmap bitmap = null;
            try {
                bitmap = BitmapFactory.decodeStream(is);
                mImageView = new ImageView(this);
                mImageView.setImageBitmap(bitmap);
                String backgroundColor = this.resourceManager.getString("presplash_color");
                if (backgroundColor != null) {
                    try {
                        mImageView.setBackgroundColor(Color.parseColor(backgroundColor));
                    } catch (IllegalArgumentException e) {
                    }
                }
                mImageView.setLayoutParams(new LayoutParams(-1, -1));
                mImageView.setScaleType(ScaleType.FIT_CENTER);
            } finally {
                try {
                    is.close();
                } catch (IOException e2) {
                }
            }
        }
        if (mLayout == null) {
            setContentView(mImageView);
        } else if (mImageView.getParent() == null) {
            mLayout.addView(mImageView);
        }
    }

    protected void onPause() {
        if (this.mWakeLock != null && this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
        Log.v(TAG, "onPause()");
        super.onPause();
    }

    protected void onResume() {
        if (this.mWakeLock != null) {
            this.mWakeLock.acquire();
        }
        Log.v(TAG, "onResume()");
        super.onResume();
    }
}
