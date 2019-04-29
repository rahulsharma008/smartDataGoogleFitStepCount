package org.kivy.android.libsdl.app;

/* compiled from: SDLActivity */
class SDLMain implements Runnable {
    SDLMain() {
    }

    public void run() {
        SDLActivity.nativeInit(SDLActivity.mSingleton.getArguments());
    }
}
