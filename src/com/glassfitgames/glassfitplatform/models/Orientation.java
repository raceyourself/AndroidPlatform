package com.glassfitgames.glassfitplatform.models;

import java.sql.Timestamp;
import java.util.List;

import com.roscopeco.ormdroid.Entity;

import static com.roscopeco.ormdroid.Query.eql;

//demo model, will be replaced soon
public class Orientation extends Entity {
    public int id; // Auto-generated ID
    public int track_id; // Track ID of track entity that 'contains' this
                         // orientation
    public long ts; // date/time observation was taken
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
    public float rot_a; // Rotation vector cos_theta
    public float rot_b; // Rotation vector x*sin_theta
    public float rot_c; // Rotation vector y*sin_theta
    public float rot_d; // Rotation vector z*sin_theta
    public float linacc_x; // Acceleration in real-world x-axis
    public float linacc_y; // Acceleration in real-world y-axis
    public float linacc_z; // Acceleration in real-world z-axis

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
    
    public float[] getRotationVector() {
        return new float[] {rot_a, rot_b, rot_c};
    }
    
    public void setRotationVector(float[] rotationVector) {
        this.rot_b = rotationVector[0];
        this.rot_c = rotationVector[1];
        this.rot_d = rotationVector[2];
    }
    
    public float[] getYawPitchRoll() {
        return new float[] {yaw, pitch, roll};
    }
    
    public void setYawPitchRoll(float[] ypr) {
        this.yaw = ypr[0];
        this.pitch = ypr[1];
        this.roll = ypr[2];
    }    
    
    public long getTimestamp() {
        return ts;
    }
    
    public void setTimestamp(long timestamp) {
        this.ts = timestamp;
    }

    public void setLinearAcceleration(float[] linAccValues) {
        this.linacc_x = linAccValues[0];
        this.linacc_y = linAccValues[1];
        this.linacc_z = linAccValues[2];
    }
    
    public float[] getLinearAcceleration() {
        return new float[] {linacc_x, linacc_y, linacc_z};
    }

    public void setAccelerometer(float[] accValues) {
        this.acc_x = accValues[0];
        this.acc_y = accValues[1];
        this.acc_z = accValues[2];
    }

    public void setGyroscope(float[] gyroValues) {
        this.gyro_x = gyroValues[0];
        this.gyro_y = gyroValues[1];
        this.gyro_z = gyroValues[2];
    }

    public void setMagnetometer(float[] magValues) {
        this.mag_x = magValues[0];
        this.mag_y = magValues[1];
        this.mag_z = magValues[2];
    }
}
