package com.raceyourself.raceyourself.home.feed;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import com.raceyourself.platform.models.Position;
import com.raceyourself.platform.models.Track;
import com.raceyourself.platform.utils.UnitConversion;
import com.raceyourself.raceyourself.game.GameConfiguration;

import org.joda.time.DateTime;

import java.util.Date;
import java.util.List;

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
    private double distanceRan = 0.0;
    private float averagePace = 0.0f;
    private float topSpeed = 0.0f;
    private float totalUp = 0.0f;
    private float totalDown = 0.0f;
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

    // loads the track but truncates based on the target time/dist in gameConfiguration
    // TODO: make this work for DISTANCE_CHALLENEGE type configs
    public TrackSummaryBean(Track track, GameConfiguration gameConfiguration) {

        if (track.getTrackPositions().size() == 0) return;  // default values

        long timeLimit = Long.MAX_VALUE;
        if (gameConfiguration.getGameType() == GameConfiguration.GameType.TIME_CHALLENGE) {
            timeLimit = gameConfiguration.getTargetTime();
        }

        // initialize
        Position lastPosition = null;
        double metresClimbed = 0;
        double metresDescended = 0;
        float maxSpeed = 0;
        double distance = 0.0;
        long trackStartTime = 0l;
        List<Position> positions = track.getTrackPositions();  // calls to getTrackPositions hit the database so are expensive
        trackStartTime = positions.get(0).getDeviceTimestamp();

        // loop through positions
        for (Position position : positions) {
            if (position.getDeviceTimestamp() - trackStartTime > timeLimit) break;  // TODO: interpolate
            if (lastPosition == null) {
                lastPosition = position;
            } else {
                Double alt = position.getAltitude();
                if (alt != null && alt > lastPosition.getAltitude()) metresClimbed += (alt - lastPosition.getAltitude());
                if (alt != null && alt < lastPosition.getAltitude()) metresDescended -= (alt - lastPosition.getAltitude());
                if (position.speed > maxSpeed) maxSpeed = position.speed;
                distance += Position.distanceBetween(lastPosition, position);
                lastPosition = position;
            }
        }
        this.setAveragePace((float) (1000 * distance / (lastPosition.getDeviceTimestamp()-trackStartTime)));
        this.setDistanceRan(distance);
        this.setTopSpeed(maxSpeed);
        this.setTotalUp(Math.round((metresClimbed) * 100) / 100);
        this.setTotalDown(Math.round((metresDescended) * 100) / 100);
        this.setDeviceId(track.device_id);
        this.setTrackId(track.track_id);
        this.setRaceDate(track.getRawDate());

    }

    public TrackSummaryBean(float fixedSpeed, long durationMillis) {
        this.setDistanceRan(fixedSpeed*(durationMillis/1000.0));
        this.setAveragePace(fixedSpeed);
        this.setTopSpeed(fixedSpeed);
        this.setTotalUp(0);
        this.setTotalDown(0);
        this.setDeviceId(-1);
        this.setTrackId(-1);  // TODO - better way of identifying a fixed speed track
        this.setRaceDate(DateTime.now().minusDays(3).toDate());  // date the track 3 days ago to make it seem fresh
    }

    public TrackSummaryBean(Track track) {
        this(track, new GameConfiguration.GameStrategyBuilder(GameConfiguration.GameType.TIME_CHALLENGE).targetTime(Long.MAX_VALUE).build());
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
