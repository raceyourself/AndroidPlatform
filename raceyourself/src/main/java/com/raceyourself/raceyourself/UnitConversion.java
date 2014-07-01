package com.raceyourself.raceyourself;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by benlister on 01/07/2014.
 */
@Slf4j
public final class UnitConversion {

    public final static double miles(double metres) {
        return metres * 0.00062137119;
    }

    public final static float minutesPerMile(float metresPerSecond) {
        return 26.8224f / metresPerSecond;
    }

}
