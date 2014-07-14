package com.raceyourself.raceyourself.base.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;

import com.squareup.picasso.Transformation;

/**
 * Created by Amerigo on 02/07/2014.
 */
public class PictureUtils {
    public static Bitmap getRoundedBmp(Bitmap bitmap, int width) {
        Bitmap targetBitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(targetBitmap);
        Path path = new Path();
        path.addCircle(((float)width - 1) / 2, ((float)width - 1) / 2, (Math.min(((float)width), ((float)width))/2), Path.Direction.CCW);
        canvas.clipPath(path);
        Bitmap sourceBitmap = bitmap;
        canvas.drawBitmap(sourceBitmap, new Rect(0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight()), new Rect(0, 0, width, width), null);
        sourceBitmap.recycle();
        return targetBitmap;
    }

    public static class CropCircle implements Transformation {

        @Override
        public Bitmap transform(Bitmap source) {
            return PictureUtils.getRoundedBmp(source, source.getWidth());
        }

        @Override
        public String key() {
            return null;
        }
    }
}
