package com.raceyourself.raceyourself.base.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;

/**
 * Created by Amerigo on 02/07/2014.
 */
public class PictureUtils {
    public static Bitmap getRoundedBmp(Bitmap bitmap, int width) {
        int targetWidth = width;
        int targetHeight = width;
        Bitmap targetBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(targetBitmap);
        Path path = new Path();
        path.addCircle(((float)targetWidth - 1) / 2, ((float)targetHeight - 1) / 2, (Math.min(((float)targetWidth), ((float)targetHeight))/2), Path.Direction.CCW);
        canvas.clipPath(path);
        Bitmap sourceBitmap = bitmap;
        canvas.drawBitmap(sourceBitmap, new Rect(0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight()), new Rect(0, 0, targetWidth, targetHeight), null);
        return targetBitmap;
    }
}
