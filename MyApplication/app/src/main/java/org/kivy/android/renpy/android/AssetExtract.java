package org.kivy.android.renpy.android;

import android.app.Activity;
import android.content.res.AssetManager;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarInputStream;

public class AssetExtract {
    private Activity mActivity = null;
    private AssetManager mAssetManager = null;

    public AssetExtract(Activity act) {
        this.mActivity = act;
        this.mAssetManager = act.getAssets();
    }

    public boolean extractTar(String asset, String target) {
        TarInputStream tarInputStream;
        byte[] buf = new byte[1048576];
        try {
            InputStream assetStream = this.mAssetManager.open(asset, 2);
            TarInputStream tis = new TarInputStream(new BufferedInputStream(new GZIPInputStream(new BufferedInputStream(assetStream, 8192)), 8192));
            while (true) {
                try {
                    TarEntry entry = tis.getNextEntry();
                    if (entry == null) {
                        try {
                            break;
                        } catch (IOException e) {
                        }
                    } else {
                        Log.v("python", "extracting " + entry.getName());
                        if (entry.isDirectory()) {
                            try {
                                new File(target + "/" + entry.getName()).mkdirs();
                            } catch (SecurityException e2) {
                            }
                        } else {
                            OutputStream out = null;
                            String path = target + "/" + entry.getName();
                            try {
                                out = new BufferedOutputStream(new FileOutputStream(path), 8192);
                            } catch (FileNotFoundException e3) {
                            } catch (SecurityException e4) {
                            }
                            if (out == null) {
                                Log.e("python", "could not open " + path);
                                tarInputStream = tis;
                                return false;
                            }
                            while (true) {
                                int len = tis.read(buf);
                                if (len == -1) {
                                    break;
                                }
                                try {
                                    out.write(buf, 0, len);
                                } catch (IOException e5) {
                                    Log.e("python", "extracting zip", e5);
                                    tarInputStream = tis;
                                    return false;
                                }
                            }
                            out.flush();
                            out.close();
                        }
                    }
                } catch (IOException e52) {
                    Log.e("python", "extracting tar", e52);
                    tarInputStream = tis;
                    return false;
                }
            }
            tis.close();
            assetStream.close();
            tarInputStream = tis;
            return true;
        } catch (IOException e522) {
            Log.e("python", "opening up extract tar", e522);
            return false;
        }
    }
}
