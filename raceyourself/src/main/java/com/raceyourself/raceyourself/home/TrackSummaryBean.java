package com.raceyourself.raceyourself.home;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Amerigo on 04/07/2014.
 */
@Slf4j
@Data
public class TrackSummaryBean {
    private int trackId;
    private int deviceId;
    private Date raceDate;
    private int distanceRan;
    private float averagePace;
    private float topSpeed;
    private float totalUp;
    private float totalDown;
    private Bitmap weatherIcon;

    public TrackSummaryBean() {}
}
