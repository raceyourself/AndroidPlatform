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
public class TrackSummaryBean implements Parcelable {
    private Date raceDate;
    private int distanceRan;
    private float averagePace;
    private float topSpeed;
    private float totalUp;
    private float totalDown;
    private Bitmap weatherIcon;

    @Override
    public int describeContents() {
        return 0;
    }

    private TrackSummaryBean(Parcel in) {
        this.weatherIcon = (Bitmap)in.readParcelable(Bitmap.class.getClassLoader());
        long dateInUnix = in.readLong();
        this.raceDate = new Date(dateInUnix);
        this.distanceRan = in.readInt();
        this.averagePace = in.readFloat();
        this.topSpeed = in.readFloat();
        this.totalUp = in.readFloat();
        this.totalDown = in.readFloat();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(weatherIcon, flags);
        long dateInUnix = raceDate.getTime();
        dest.writeLong(dateInUnix);
        dest.writeInt(distanceRan);
        dest.writeFloat(averagePace);
        dest.writeFloat(topSpeed);
        dest.writeFloat(totalUp);
        dest.writeFloat(totalDown);
    }

    public static final Parcelable.Creator<TrackSummaryBean> CREATOR = new Parcelable.Creator<TrackSummaryBean>() {
        public TrackSummaryBean createFromParcel(Parcel in) {
            return new TrackSummaryBean(in);
        }

        @Override
        public TrackSummaryBean[] newArray(int size) {
            return new TrackSummaryBean[size];
        }
    };
}
