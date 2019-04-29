package org.kivy.android;

import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

public class PythonUtil {
    private static final String TAG = "pythonutil";

    protected static void addLibraryIfExists(ArrayList<String> libsList, String pattern, File libsDir) {
        File[] files = libsDir.listFiles();
        pattern = "lib" + pattern + "\\.so";
        Pattern p = Pattern.compile(pattern);
        for (File file : files) {
            String name = file.getName();
            Log.v(TAG, "Checking pattern " + pattern + " against " + name);
            if (p.matcher(name).matches()) {
                Log.v(TAG, "Pattern " + pattern + " matched file " + name);
                libsList.add(name.substring(3, name.length() - 3));
            }
        }
    }

    protected static ArrayList<String> getLibraries(File filesDir) {
        File libsDir = new File(filesDir.getParentFile().getParentFile().getAbsolutePath() + "/lib/");
        ArrayList<String> libsList = new ArrayList();
        addLibraryIfExists(libsList, "crystax", libsDir);
        addLibraryIfExists(libsList, "sqlite3", libsDir);
        libsList.add("SDL2");
        libsList.add("SDL2_image");
        libsList.add("SDL2_mixer");
        libsList.add("SDL2_ttf");
        addLibraryIfExists(libsList, "ssl.*", libsDir);
        addLibraryIfExists(libsList, "crypto.*", libsDir);
        libsList.add("python2.7");
        libsList.add("python3.5m");
        libsList.add("python3.6m");
        libsList.add("python3.7m");
        libsList.add("main");
        return libsList;
    }

    public static void loadLibraries(File filesDir) {
        String filesDirPath = filesDir.getAbsolutePath();
        boolean foundPython = false;
        Iterator it = getLibraries(filesDir).iterator();
        while (it.hasNext()) {
            String lib = (String) it.next();
            Log.v(TAG, "Loading library: " + lib);
            try {
                System.loadLibrary(lib);
                if (lib.startsWith("python")) {
                    foundPython = true;
                }
            } catch (UnsatisfiedLinkError e) {
                Log.v(TAG, "Library loading error: " + e.getMessage());
                if (lib.startsWith("python3.7") && !foundPython) {
                    throw new RuntimeException("Could not load any libpythonXXX.so");
                } else if (!lib.startsWith("python")) {
                    Log.v(TAG, "An UnsatisfiedLinkError occurred loading " + lib);
                    throw e;
                }
            }
        }
        try {
            System.load(filesDirPath + "/lib/python2.7/lib-dynload/_io.so");
            System.load(filesDirPath + "/lib/python2.7/lib-dynload/unicodedata.so");
        } catch (UnsatisfiedLinkError e2) {
            Log.v(TAG, "Failed to load _io.so or unicodedata.so...but that's okay.");
        }
        try {
            System.load(filesDirPath + "/lib/python2.7/lib-dynload/_ctypes.so");
        } catch (UnsatisfiedLinkError e3) {
            Log.v(TAG, "Unsatisfied linker when loading ctypes");
        }
        Log.v(TAG, "Loaded everything!");
    }
}
