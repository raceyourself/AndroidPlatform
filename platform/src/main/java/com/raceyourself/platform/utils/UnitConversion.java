package com.raceyourself.platform.utils;

/**
 * Created by benlister on 01/07/2014.
 */
public final class UnitConversion {

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

}
