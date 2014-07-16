package com.raceyourself.raceyourself.home.feed;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import com.raceyourself.platform.models.Position;
import com.raceyourself.platform.models.Track;
import com.raceyourself.platform.utils.UnitConversion;

import java.util.Date;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Amerigo on 04/07/2014.
 */
@Slf4j
@Data
public class TrackSummaryBean implements Parcelable {
    private int trackId;
    private int deviceId;
    private Date raceDate;
    private double distanceRan;
    private float averagePace;
    private float topSpeed;
    private float totalUp;
    private float totalDown;
    private Bitmap weatherIcon;

    public TrackSummaryBean() {}

    private TrackSummaryBean(Parcel in) {
        this.weatherIcon = in.readParcelable(Bitmap.class.getClassLoader());
        this.trackId = in.readInt();
        this.deviceId = in.readInt();
        long date = in.readLong();
        this.raceDate = new Date(date);
        this.distanceRan = in.readDouble();
        this.averagePace = in.readFloat();
        this.topSpeed = in.readFloat();
        this.totalUp = in.readFloat();
        this.totalDown = in.readFloat();
    }

    public TrackSummaryBean(Track track) {
        Double lastAltitude = null;
        double metresClimbed = 0;
        double metresDescended = 0;
        float maxSpeed = 0;
        for (Position position : track.getTrackPositions()) {
            if (lastAltitude == null) {
                lastAltitude = position.getAltitude();
            } else {
                Double alt = position.getAltitude();
                if (alt != null && alt > lastAltitude) metresClimbed += (alt - lastAltitude);
                if (alt != null && alt < lastAltitude) metresDescended -= (alt - lastAltitude);
                lastAltitude = alt;
            }
            if (position.speed > maxSpeed) maxSpeed = position.speed;
        }
        this.setAveragePace(Math.round(UnitConversion.minutesPerMile((float) (1000 * track.distance / track.time))));
        this.setDistanceRan(UnitConversion.miles(track.distance));
        this.setTopSpeed(Math.round(UnitConversion.minutesPerMile(maxSpeed)));
        this.setTotalUp(Math.round((metresClimbed) * 100) / 100);
        this.setTotalDown(Math.round((metresDescended) * 100) / 100);
        this.setDeviceId(track.device_id);
        this.setTrackId(track.track_id);
        this.setRaceDate(track.getRawDate());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(weatherIcon, flags);
        dest.writeInt(trackId);
        dest.writeInt(deviceId);
        dest.writeLong(raceDate.getTime());
        dest.writeDouble(distanceRan);
        dest.writeFloat(averagePace);
        dest.writeFloat(topSpeed);
        dest.writeFloat(totalUp);
        dest.writeFloat(totalDown);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public TrackSummaryBean createFromParcel(Parcel in) {
            return new TrackSummaryBean(in);
        }

        public TrackSummaryBean[] newArray(int size) {
            return new TrackSummaryBean[size];
        }
    };
}
