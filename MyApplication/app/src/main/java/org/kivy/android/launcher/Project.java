package org.kivy.android.launcher;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import org.kivy.android.BuildConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

public class Project {
    String author = null;
    public String dir = null;
    Bitmap icon = null;
    public boolean landscape = false;
    String title = null;

    static String decode(String s) {
        try {
            return new String(s.getBytes("ISO-8859-1"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }

    public static Project scanDirectory(File dir) {
        if (dir.getAbsolutePath().endsWith(".link")) {
            try {
                FileInputStream in = new FileInputStream(new File(dir, "android.txt"));
                Properties p = new Properties();
                p.load(in);
                in.close();
                String directory = p.getProperty("directory", null);
                if (directory == null) {
                    return null;
                }
                dir = new File(directory);
            } catch (Exception e) {
                Log.i("Project", "Couldn't open link file " + dir, e);
            }
        }
        if (!dir.isDirectory()) {
            return null;
        }
        try {
            FileInputStream in = new FileInputStream(new File(dir, "android.txt"));
            Properties p = new Properties();
            p.load(in);
            in.close();
            String title = decode(p.getProperty("title", "Untitled"));
            String author = decode(p.getProperty("author", BuildConfig.FLAVOR));
            boolean landscape = p.getProperty("orientation", "portrait").equals("landscape");
            Project rv = new Project();
            rv.title = title;
            rv.author = author;
            rv.icon = BitmapFactory.decodeFile(new File(dir, "icon.png").getAbsolutePath());
            rv.landscape = landscape;
            rv.dir = dir.getAbsolutePath();
            return rv;
        } catch (Exception e2) {
            Log.i("Project", "Couldn't open android.txt", e2);
            return null;
        }
    }
}
