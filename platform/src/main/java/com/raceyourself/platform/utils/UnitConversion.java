package com.raceyourself.platform.utils;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;

/**
 * Created by benlister on 01/07/2014.
 */
public final class UnitConversion {

    private static DisplayMetrics metrics = new DisplayMetrics();
    private static boolean hasMetrics = false;

    public final static double miles(double metres) {
        return metres * 0.00062137119;
    }

    public final static float minutesPerMile(float metresPerSecond) {
        return 26.8224f / metresPerSecond;
    }

    public final static float kilometersPerHour(float metresPerSecond) { return metresPerSecond * 3.6f; }

    public final static float milesPerHour(float metresPerSecond) { return metresPerSecond * 2.23693629f; }

    public final static long minutes(long millis) {
        return millis/60000;
    }

    public final static int pixels(int dp, Activity a) {
        if (!hasMetrics && a != null && a.getWindowManager() != null && a.getWindowManager().getDefaultDisplay() != null) {
            a.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            hasMetrics = true;
        }
        return (int) (dp * (float)metrics.densityDpi / 160.0f);
    }

}
