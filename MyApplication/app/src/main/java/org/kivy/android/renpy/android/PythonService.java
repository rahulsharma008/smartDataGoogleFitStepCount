package org.kivy.android.renpy.android;

import android.util.Log;

class PythonService extends org.kivy.android.PythonService {
    PythonService() {
    }

    static {
        Log.w("PythonService", "Accessing org.renpy.android.PythonService is deprecated and will be removed in a future version. Please switch to org.kivy.android.PythonService.");
    }
}
