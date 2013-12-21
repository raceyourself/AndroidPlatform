package com.glassfitgames.glassfitplatform.utils;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class FileUtils {

    public static File createSdCardFile(Context c, String filename) throws IOException {

        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            Log.i("GlassfitFile", "External storage is mounted as writeable.");
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            Log.w("GlassfitFile", "External storage is mounted as read-only.");
            throw new IOException("External storage is mounted as read-only");
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            // to know is we can neither read nor write
            Log.w("GlassfitFile", "External storage is not available.");
            throw new IOException("External storage is not available.");
        }

        File file = new File(c.getExternalFilesDir(null), filename);
        Log.i("GlassfitFile", "External File dir is: " + c.getExternalFilesDir(null));
        file.getParentFile().mkdirs();
        Log.i("GlassfitFile", "Directories created ok");
        if (!file.exists())
            file.createNewFile();
        Log.i("GlassfitFile", "File ready for writing: " + file.getAbsolutePath());
        return file;

    }

}
