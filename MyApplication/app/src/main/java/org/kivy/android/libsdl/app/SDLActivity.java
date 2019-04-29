package org.kivy.android.libsdl.app;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnKeyListener;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsoluteLayout;
import android.widget.AbsoluteLayout.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import binder24.com.hydrostar.BuildConfig;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;

public class SDLActivity extends Activity {
    static final int COMMAND_CHANGE_TITLE = 1;
    static final int COMMAND_SET_KEEP_SCREEN_ON = 5;
    static final int COMMAND_TEXTEDIT_HIDE = 3;
    static final int COMMAND_UNUSED = 2;
    protected static final int COMMAND_USER = 32768;
    private static final String TAG = "SDL";
    protected static AudioTrack mAudioTrack;
    public static boolean mBrokenLibraries;
    public static boolean mExitCalledFromJava;
    public static boolean mHasFocus;
    public static boolean mIsPaused;
    public static boolean mIsSurfaceReady;
    protected static SDLJoystickHandler mJoystickHandler;
    protected static ViewGroup mLayout;
    protected static Thread mSDLThread;
    public static boolean mSeparateMouseAndTouch;
    protected static SDLActivity mSingleton;
    protected static SDLSurface mSurface;
    protected static View mTextEdit;
    Handler commandHandler = new SDLCommandHandler();
    protected int dialogs = 0;
    private Object expansionFile;
    private Method expansionFileMethod;
    protected final int[] messageboxSelection = new int[COMMAND_CHANGE_TITLE];

    protected static class SDLCommandHandler extends Handler {
        protected SDLCommandHandler() {
        }

        public void handleMessage(Message msg) {
            Context context = SDLActivity.getContext();
            if (context == null) {
                Log.e(SDLActivity.TAG, "error handling message, getContext() returned null");
                return;
            }
            switch (msg.arg1) {
                case SDLActivity.COMMAND_CHANGE_TITLE /*1*/:
                    if (context instanceof Activity) {
                        ((Activity) context).setTitle((String) msg.obj);
                        return;
                    } else {
                        Log.e(SDLActivity.TAG, "error handling message, getContext() returned no Activity");
                        return;
                    }
                case SDLActivity.COMMAND_TEXTEDIT_HIDE /*3*/:
                    if (SDLActivity.mTextEdit != null) {
                        SDLActivity.mTextEdit.setVisibility(8);
                        ((InputMethodManager) context.getSystemService("input_method")).hideSoftInputFromWindow(SDLActivity.mTextEdit.getWindowToken(), 0);
                        return;
                    }
                    return;
                case SDLActivity.COMMAND_SET_KEEP_SCREEN_ON /*5*/:
                    Window window = ((Activity) context).getWindow();
                    if (window == null) {
                        return;
                    }
                    if (!(msg.obj instanceof Integer) || ((Integer) msg.obj).intValue() == 0) {
                        window.clearFlags(128);
                        return;
                    } else {
                        window.addFlags(128);
                        return;
                    }
                default:
                    if ((context instanceof SDLActivity) && !((SDLActivity) context).onUnhandledMessage(msg.arg1, msg.obj)) {
                        Log.e(SDLActivity.TAG, "error handling message, command is " + msg.arg1);
                        return;
                    }
                    return;
            }
        }
    }

    static class ShowTextInputTask implements Runnable {
        static final int HEIGHT_PADDING = 15;
        public int h;
        public int w;
        public int x;
        public int y;

        public ShowTextInputTask(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public void run() {
            LayoutParams params = new LayoutParams(this.w, this.h + HEIGHT_PADDING, this.x, this.y);
            if (SDLActivity.mTextEdit == null) {
                SDLActivity.mTextEdit = new DummyEdit(SDLActivity.getContext());
                SDLActivity.mLayout.addView(SDLActivity.mTextEdit, params);
            } else {
                SDLActivity.mTextEdit.setLayoutParams(params);
            }
            SDLActivity.mTextEdit.setVisibility(0);
            SDLActivity.mTextEdit.requestFocus();
            ((InputMethodManager) SDLActivity.getContext().getSystemService("input_method")).showSoftInput(SDLActivity.mTextEdit, 0);
        }
    }

    public static native int nativeAddJoystick(int i, String str, int i2, int i3, int i4, int i5, int i6);

    public static native void nativeFlipBuffers();

    public static native String nativeGetHint(String str);

    public static native int nativeInit(Object obj);

    public static native void nativeLowMemory();

    public static native void nativePause();

    public static native void nativeQuit();

    public static native int nativeRemoveJoystick(int i);

    public static native void nativeResume();

    public static native void nativeSetEnv(String str, String str2);

    public static native void onNativeAccel(float f, float f2, float f3);

    public static native void onNativeHat(int i, int i2, int i3, int i4);

    public static native void onNativeJoy(int i, int i2, float f);

    public static native void onNativeKeyDown(int i);

    public static native void onNativeKeyUp(int i);

    public static native void onNativeKeyboardFocusLost();

    public static native void onNativeMouse(int i, int i2, float f, float f2);

    public static native int onNativePadDown(int i, int i2);

    public static native int onNativePadUp(int i, int i2);

    public static native void onNativeResize(int i, int i2, int i3, float f);

    public static native void onNativeSurfaceChanged();

    public static native void onNativeSurfaceDestroyed();

    public static native void onNativeTouch(int i, int i2, int i3, float f, float f2, float f3);

    protected String[] getLibraries() {
        String[] strArr = new String[COMMAND_UNUSED];
        strArr[0] = "SDL2";
        strArr[COMMAND_CHANGE_TITLE] = "main";
        return strArr;
    }

    public void loadLibraries() {
        String[] libraries = getLibraries();
        int length = libraries.length;
        for (int i = 0; i < length; i += COMMAND_CHANGE_TITLE) {
            System.loadLibrary(libraries[i]);
        }
    }

    protected String[] getArguments() {
        return new String[0];
    }

    public static void initialize() {
        mSingleton = null;
        mSurface = null;
        mTextEdit = null;
        mLayout = null;
        mJoystickHandler = null;
        mSDLThread = null;
        mAudioTrack = null;
        mExitCalledFromJava = false;
        mBrokenLibraries = false;
        mIsPaused = false;
        mIsSurfaceReady = false;
        mHasFocus = true;
    }

    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "Device: " + Build.DEVICE);
        Log.v(TAG, "Model: " + Build.MODEL);
        Log.v(TAG, "onCreate():" + mSingleton);
        super.onCreate(savedInstanceState);
        initialize();
        mSingleton = this;
    }

    protected void finishLoad() {
        String errorMsgBrokenLib = BuildConfig.FLAVOR;
        try {
            loadLibraries();
        } catch (UnsatisfiedLinkError e) {
            System.err.println(e.getMessage());
            mBrokenLibraries = true;
            errorMsgBrokenLib = e.getMessage();
        } catch (Exception e2) {
            System.err.println(e2.getMessage());
            mBrokenLibraries = true;
            errorMsgBrokenLib = e2.getMessage();
        }
        if (mBrokenLibraries) {
            Builder dlgAlert = new Builder(this);
            dlgAlert.setMessage("An error occurred while trying to start the application. Please try again and/or reinstall." + System.getProperty("line.separator") + System.getProperty("line.separator") + "Error: " + errorMsgBrokenLib);
            dlgAlert.setTitle("SDL Error");
            dlgAlert.setPositiveButton("Exit", new OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    SDLActivity.mSingleton.finish();
                }
            });
            dlgAlert.setCancelable(false);
            dlgAlert.create().show();
            return;
        }
        mSurface = new SDLSurface(getApplication());
        if (VERSION.SDK_INT >= 12) {
            mJoystickHandler = new SDLJoystickHandler_API12();
        } else {
            mJoystickHandler = new SDLJoystickHandler();
        }
        mLayout = new AbsoluteLayout(this);
        mLayout.addView(mSurface);
        setContentView(mLayout);
    }

    protected void onPause() {
        Log.v(TAG, "onPause()");
        super.onPause();
        if (!mBrokenLibraries) {
            handlePause();
        }
    }

    protected void onResume() {
        Log.v(TAG, "onResume()");
        super.onResume();
        if (!mBrokenLibraries) {
            handleResume();
        }
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Log.v(TAG, "onWindowFocusChanged(): " + hasFocus);
        if (!mBrokenLibraries) {
            mHasFocus = hasFocus;
            if (hasFocus) {
                handleResume();
            }
        }
    }

    public void onLowMemory() {
        Log.v(TAG, "onLowMemory()");
        super.onLowMemory();
        if (!mBrokenLibraries) {
            nativeLowMemory();
        }
    }

    protected void onDestroy() {
        Log.v(TAG, "onDestroy()");
        if (mBrokenLibraries) {
            super.onDestroy();
            initialize();
            return;
        }
        mExitCalledFromJava = true;
        nativeQuit();
        if (mSDLThread != null) {
            try {
                mSDLThread.join();
            } catch (Exception e) {
                Log.v(TAG, "Problem stopping thread: " + e);
            }
            mSDLThread = null;
        }
        super.onDestroy();
        initialize();
        System.exit(0);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mBrokenLibraries) {
            return false;
        }
        int keyCode = event.getKeyCode();
        if (keyCode == 25 || keyCode == 24 || keyCode == 27 || keyCode == 168 || keyCode == 169) {
            return false;
        }
        return super.dispatchKeyEvent(event);
    }

    public static void handlePause() {
        if (!mIsPaused && mIsSurfaceReady) {
            mIsPaused = true;
            nativePause();
            mSurface.enableSensor(COMMAND_CHANGE_TITLE, false);
        }
    }

    public static void handleResume() {
        if (mIsPaused && mIsSurfaceReady && mHasFocus) {
            mIsPaused = false;
            nativeResume();
            mSurface.handleResume();
        }
    }

    public static void handleNativeExit() {
        mSDLThread = null;
        mSingleton.finish();
    }

    protected boolean onUnhandledMessage(int command, Object param) {
        return false;
    }

    boolean sendCommand(int command, Object data) {
        Message msg = this.commandHandler.obtainMessage();
        msg.arg1 = command;
        msg.obj = data;
        return this.commandHandler.sendMessage(msg);
    }

    public static void flipBuffers() {
        nativeFlipBuffers();
    }

    public static boolean setActivityTitle(String title) {
        return mSingleton.sendCommand(COMMAND_CHANGE_TITLE, title);
    }

    public static boolean sendMessage(int command, int param) {
        return mSingleton.sendCommand(command, Integer.valueOf(param));
    }

    public static Context getContext() {
        return mSingleton;
    }

    public Object getSystemServiceFromUiThread(final String name) {
        final Object lock = new Object();
        final Object[] results = new Object[COMMAND_UNUSED];
        synchronized (lock) {
            runOnUiThread(new Runnable() {
                public void run() {
                    synchronized (lock) {
                        results[0] = SDLActivity.this.getSystemService(name);
                        results[SDLActivity.COMMAND_CHANGE_TITLE] = Boolean.TRUE;
                        lock.notify();
                    }
                }
            });
            if (results[COMMAND_CHANGE_TITLE] == null) {
                try {
                    lock.wait();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return results[0];
    }

    public static boolean showTextInput(int x, int y, int w, int h) {
        return mSingleton.commandHandler.post(new ShowTextInputTask(x, y, w, h));
    }

    public static Surface getNativeSurface() {
        return mSurface.getNativeSurface();
    }

    public static int audioInit(int sampleRate, boolean is16Bit, boolean isStereo, int desiredFrames) {
        String str;
        int channelConfig = isStereo ? COMMAND_TEXTEDIT_HIDE : COMMAND_UNUSED;
        int audioFormat = is16Bit ? COMMAND_UNUSED : COMMAND_TEXTEDIT_HIDE;
        int frameSize = (isStereo ? COMMAND_UNUSED : COMMAND_CHANGE_TITLE) * (is16Bit ? COMMAND_UNUSED : COMMAND_CHANGE_TITLE);
        String str2 = TAG;
        StringBuilder append = new StringBuilder().append("SDL audio: wanted ").append(isStereo ? "stereo" : "mono").append(" ");
        if (is16Bit) {
            str = "16-bit";
        } else {
            str = "8-bit";
        }
        Log.v(str2, append.append(str).append(" ").append(((float) sampleRate) / 1000.0f).append("kHz, ").append(desiredFrames).append(" frames buffer").toString());
        desiredFrames = Math.max(desiredFrames, ((AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat) + frameSize) - 1) / frameSize);
        if (mAudioTrack == null) {
            mAudioTrack = new AudioTrack(COMMAND_TEXTEDIT_HIDE, sampleRate, channelConfig, audioFormat, desiredFrames * frameSize, COMMAND_CHANGE_TITLE);
            if (mAudioTrack.getState() != COMMAND_CHANGE_TITLE) {
                Log.e(TAG, "Failed during initialization of Audio Track");
                mAudioTrack = null;
                return -1;
            }
            mAudioTrack.play();
        }
        Log.v(TAG, "SDL audio: got " + (mAudioTrack.getChannelCount() >= COMMAND_UNUSED ? "stereo" : "mono") + " " + (mAudioTrack.getAudioFormat() == COMMAND_UNUSED ? "16-bit" : "8-bit") + " " + (((float) mAudioTrack.getSampleRate()) / 1000.0f) + "kHz, " + desiredFrames + " frames buffer");
        return 0;
    }

    public static void audioWriteShortBuffer(short[] buffer) {
        int i = 0;
        while (i < buffer.length) {
            int result = mAudioTrack.write(buffer, i, buffer.length - i);
            if (result > 0) {
                i += result;
            } else if (result == 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
            } else {
                Log.w(TAG, "SDL audio: error return from write(short)");
                return;
            }
        }
    }

    public static void audioWriteByteBuffer(byte[] buffer) {
        int i = 0;
        while (i < buffer.length) {
            int result = mAudioTrack.write(buffer, i, buffer.length - i);
            if (result > 0) {
                i += result;
            } else if (result == 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
            } else {
                Log.w(TAG, "SDL audio: error return from write(byte)");
                return;
            }
        }
    }

    public static void audioQuit() {
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack = null;
        }
    }

    public static int[] inputGetInputDeviceIds(int sources) {
        int[] ids = InputDevice.getDeviceIds();
        int[] filtered = new int[ids.length];
        int used = 0;
        for (int i = 0; i < ids.length; i += COMMAND_CHANGE_TITLE) {
            InputDevice device = InputDevice.getDevice(ids[i]);
            if (!(device == null || (device.getSources() & sources) == 0)) {
                int used2 = used + COMMAND_CHANGE_TITLE;
                filtered[used] = device.getId();
                used = used2;
            }
        }
        return Arrays.copyOf(filtered, used);
    }

    public static boolean handleJoystickMotionEvent(MotionEvent event) {
        return mJoystickHandler.handleMotionEvent(event);
    }

    public static void pollInputDevices() {
        if (mSDLThread != null) {
            mJoystickHandler.pollInputDevices();
            mSingleton.keepActive();
        }
    }

    public void keepActive() {
    }

    public InputStream openAPKExtensionInputStream(String fileName) throws IOException {
        InputStream fileStream;
        if (this.expansionFile == null) {
            Integer mainVersion = Integer.valueOf(nativeGetHint("SDL_ANDROID_APK_EXPANSION_MAIN_FILE_VERSION"));
            Integer patchVersion = Integer.valueOf(nativeGetHint("SDL_ANDROID_APK_EXPANSION_PATCH_FILE_VERSION"));
            try {
                Class[] clsArr = new Class[COMMAND_TEXTEDIT_HIDE];
                clsArr[0] = Context.class;
                clsArr[COMMAND_CHANGE_TITLE] = Integer.TYPE;
                clsArr[COMMAND_UNUSED] = Integer.TYPE;
                Method method = Class.forName("com.android.vending.expansion.zipfile.APKExpansionSupport").getMethod("getAPKExpansionZipFile", clsArr);
                Object[] objArr = new Object[COMMAND_TEXTEDIT_HIDE];
                objArr[0] = this;
                objArr[COMMAND_CHANGE_TITLE] = mainVersion;
                objArr[COMMAND_UNUSED] = patchVersion;
                this.expansionFile = method.invoke(null, objArr);
                clsArr = new Class[COMMAND_CHANGE_TITLE];
                clsArr[0] = String.class;
                this.expansionFileMethod = this.expansionFile.getClass().getMethod("getInputStream", clsArr);
            } catch (Exception ex) {
                ex.printStackTrace();
                this.expansionFile = null;
                this.expansionFileMethod = null;
            }
        }
        try {
            method = this.expansionFileMethod;
            Object obj = this.expansionFile;
            objArr = new Object[COMMAND_CHANGE_TITLE];
            objArr[0] = fileName;
            fileStream = (InputStream) method.invoke(obj, objArr);
        } catch (Exception ex2) {
            ex2.printStackTrace();
            fileStream = null;
        }
        if (fileStream != null) {
            return fileStream;
        }
        throw new IOException();
    }

    public int messageboxShowMessageBox(int flags, String title, String message, int[] buttonFlags, int[] buttonIds, String[] buttonTexts, int[] colors) {
        this.messageboxSelection[0] = -1;
        if (buttonFlags.length != buttonIds.length && buttonIds.length != buttonTexts.length) {
            return -1;
        }
        final Bundle args = new Bundle();
        args.putInt("flags", flags);
        args.putString("title", title);
        args.putString("message", message);
        args.putIntArray("buttonFlags", buttonFlags);
        args.putIntArray("buttonIds", buttonIds);
        args.putStringArray("buttonTexts", buttonTexts);
        args.putIntArray("colors", colors);
        runOnUiThread(new Runnable() {
            public void run() {
                SDLActivity sDLActivity = SDLActivity.this;
                SDLActivity sDLActivity2 = SDLActivity.this;
                int i = sDLActivity2.dialogs;
                sDLActivity2.dialogs = i + SDLActivity.COMMAND_CHANGE_TITLE;
                sDLActivity.showDialog(i, args);
            }
        });
        synchronized (this.messageboxSelection) {
            try {
                this.messageboxSelection.wait();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
                return -1;
            }
        }
        return this.messageboxSelection[0];
    }

    protected Dialog onCreateDialog(int ignore, Bundle args) {
        int i;
        int backgroundColor;
        int textColor;
        int buttonBorderColor;
        int buttonBackgroundColor;
        int buttonSelectedColor;
        int[] colors = args.getIntArray("colors");
        if (colors != null) {
            i = -1 + COMMAND_CHANGE_TITLE;
            backgroundColor = colors[i];
            i += COMMAND_CHANGE_TITLE;
            textColor = colors[i];
            i += COMMAND_CHANGE_TITLE;
            buttonBorderColor = colors[i];
            i += COMMAND_CHANGE_TITLE;
            buttonBackgroundColor = colors[i];
            buttonSelectedColor = colors[i + COMMAND_CHANGE_TITLE];
        } else {
            backgroundColor = 0;
            textColor = 0;
            buttonBorderColor = 0;
            buttonBackgroundColor = 0;
            buttonSelectedColor = 0;
        }
        final Dialog dialog = new Dialog(this);
        dialog.setTitle(args.getString("title"));
        dialog.setCancelable(false);
        dialog.setOnDismissListener(new OnDismissListener() {
            public void onDismiss(DialogInterface unused) {
                synchronized (SDLActivity.this.messageboxSelection) {
                    SDLActivity.this.messageboxSelection.notify();
                }
            }
        });
        View textView = new TextView(this);
        textView.setGravity(17);
        textView.setText(args.getString("message"));
        if (textColor != 0) {
            textView.setTextColor(textColor);
        }
        int[] buttonFlags = args.getIntArray("buttonFlags");
        int[] buttonIds = args.getIntArray("buttonIds");
        String[] buttonTexts = args.getStringArray("buttonTexts");
        SparseArray<Button> mapping = new SparseArray();
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(0);
        buttons.setGravity(17);
        i = 0;
        while (i < buttonTexts.length) {
            Button button = new Button(this);
            final int i2 = buttonIds[i];
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    SDLActivity.this.messageboxSelection[0] = i2;
                    dialog.dismiss();
                }
            });
            if (buttonFlags[i] != 0) {
                if ((buttonFlags[i] & COMMAND_CHANGE_TITLE) != 0) {
                    mapping.put(66, button);
                }
                if ((buttonFlags[i] & COMMAND_UNUSED) != 0) {
                    mapping.put(111, button);
                }
            }
            button.setText(buttonTexts[i]);
            if (textColor != 0) {
                button.setTextColor(textColor);
            }
            Drawable drawable;
            if (buttonBorderColor != 0) {
                if (buttonBackgroundColor != 0) {
                    drawable = button.getBackground();
                    if (drawable != null) {
                        button.setBackgroundColor(buttonBackgroundColor);
                    } else {
                        drawable.setColorFilter(buttonBackgroundColor, Mode.MULTIPLY);
                    }
                }
                if (buttonSelectedColor == 0) {
                    buttons.addView(button);
                    i += COMMAND_CHANGE_TITLE;
                } else {
                    buttons.addView(button);
                    i += COMMAND_CHANGE_TITLE;
                }
            } else {
                if (buttonBackgroundColor != 0) {
                    drawable = button.getBackground();
                    if (drawable != null) {
                        drawable.setColorFilter(buttonBackgroundColor, Mode.MULTIPLY);
                    } else {
                        button.setBackgroundColor(buttonBackgroundColor);
                    }
                }
                if (buttonSelectedColor == 0) {
                    buttons.addView(button);
                    i += COMMAND_CHANGE_TITLE;
                } else {
                    buttons.addView(button);
                    i += COMMAND_CHANGE_TITLE;
                }
            }
        }
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(COMMAND_CHANGE_TITLE);
        content.addView(textView);
        content.addView(buttons);
        if (backgroundColor != 0) {
            content.setBackgroundColor(backgroundColor);
        }
        dialog.setContentView(content);
        final SparseArray<Button> sparseArray = mapping;
        dialog.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(DialogInterface d, int keyCode, KeyEvent event) {
                Button button = (Button) sparseArray.get(keyCode);
                if (button == null) {
                    return false;
                }
                if (event.getAction() != SDLActivity.COMMAND_CHANGE_TITLE) {
                    return true;
                }
                button.performClick();
                return true;
            }
        });
        return dialog;
    }
}
