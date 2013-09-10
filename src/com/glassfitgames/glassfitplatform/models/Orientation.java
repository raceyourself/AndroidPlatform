package com.glassfitgames.glassfitplatform.models;

import java.sql.Timestamp;
import java.util.List;

import com.roscopeco.ormdroid.Entity;

import static com.roscopeco.ormdroid.Query.eql;

//demo model, will be replaced soon
public class Orientation extends Entity {
    public int orientation_id; // Auto-generated ID
    public int track_id; // Track ID of track entity that 'contains' this
                         // orientation
    public Timestamp ts; // date/time observation was taken
    public float roll; // Roll
    public float pitch; // Pitch
    public float yaw; // Yaw
    public float mag_x; // Magnetometer x-axis
    public float mag_y; // Magnetometer y-axis
    public float mag_z; // Magnetometer z-axis
    public float acc_x; // Accelerometer x-axis
    public float acc_y; // Accelerometer y-axis
    public float acc_z; // Accelerometer z-axis
    public float gyro_x; // Gyroscope x-axis
    public float gyro_y; // Gyroscope y-axis
    public float gyro_z; // Gyroscope z-axis

    public Orientation() {

    }

    public Orientation(int track_id, float roll, float pitch, float yaw) {
        // set local variables from args
    }

    public List<Orientation> getOrientations(int track_id) {
        return query(Orientation.class).where(eql("track_id", track_id))
                .executeMulti();
    }

    public String toString() {
        return "Roll: " + this.roll + " , Pitch: " + this.pitch + ", Yaw: ";
    }
}
