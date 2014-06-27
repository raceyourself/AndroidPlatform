package com.raceyourself.platform.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class EnhancedPosition extends Position {
	// Globally unique compound key (orientation, device)
	public int position_id;
    public int device_id;
    // Globally unique foreign key (track, device)
    public int track_id; 
    // Encoded id for local db
    @JsonIgnore
    public long id = 0; 

	// Compass azimuth (degrees)
	public float azimuth = 0.0f;
	// Real-world yaw (degrees) as reported by UI
	public float yaw = 0.0f;
	
	public EnhancedPosition() {	}
	public EnhancedPosition(Position p) {
		super(p);
	}

	public EnhancedPosition(Position p, float azimuthDegrees, float yawDegrees)
	{
		super(p);
		this.azimuth = azimuthDegrees;
		this.yaw = yawDegrees; 
	}

	public float getAzimuth() { return azimuth; }
	public float getYaw() { return yaw; }
	
}
