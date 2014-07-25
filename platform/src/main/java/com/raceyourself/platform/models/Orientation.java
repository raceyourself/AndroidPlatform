package com.raceyourself.platform.models;

import java.nio.ByteBuffer;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.raceyourself.platform.sensors.Quaternion;
import com.roscopeco.ormdroid.Entity;

/**
 * Orientation sample.
 * Linked to track using foreign key (device_id, track_id)
 * 
 * Consistency model: Client can add or delete.
 *                    Server can upsert/delete using compound key.
 */
public class Orientation extends Entity {

	// Globally unique compound key (orientation, device)
	public int orientation_id;
    public int device_id;
    // Globally unique foreign key (track, device)
    public int track_id;     
    // Encoded id for local db
    @JsonIgnore
    public long id = 0; 
    
    // Fields
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

    @JsonIgnore
    public boolean dirty = false;
    public Date deleted_at = null;
    
    public Orientation() {

    }

    public Orientation(Track track, float roll, float pitch, float yaw) {
    	this.device_id = track.device_id;
    	this.track_id = track.track_id;
	    this.orientation_id = 0; // Set in save()
    	dirty = true;
    }

    public String toString() {
        return "Roll: " + this.roll + " , Pitch: " + this.pitch + ", Yaw: ";
    }
    
    public float[] getRotationVector() {
        return new float[] {rot_a, rot_b, rot_c};
    }
    
    public void setOrientation(Quaternion orientation) {
        this.rot_a = orientation.getW();
        this.rot_b = orientation.getX();
        this.rot_c = orientation.getY();
        this.rot_d = orientation.getZ();
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

	@Override
	public int save() {
		if (orientation_id == 0) orientation_id = Sequence.getNext("orientation_id");
		if (id == 0) {
			ByteBuffer encodedId = ByteBuffer.allocate(8);
			encodedId.putInt(device_id);
			encodedId.putInt(orientation_id);
			encodedId.flip();
			this.id = encodedId.getLong();
		}
		return super.save();
	}
	
	@Override
	public void delete() {
		deleted_at = new Date();
		save();
	}
	
	public void flush() {
		if (deleted_at != null) {
			super.delete();		
			return;
		}
		if (dirty) {
			dirty = false;
			save();
		}
	}	
}
