package com.example.irving.smartfridge.util;

import android.util.Log;

import net.bither.util.CompressTools;

import java.io.File;
import java.io.FileOutputStream;

public class ImageUtil {
    private final static String TAG = "ImageUtil";

    public static File convertByteToFile(byte[] imageByte, File cacheDir) {
        File f = null;
        try {
            // create a file to write bitmap data
            f = new File(cacheDir, "photo");
            if(f.exists())
                f.delete();
            f.createNewFile();

            // write the bytes in file
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(imageByte);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "error when create file");
        }
        return f;
    }

}
