package com.raceyourself.platform.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.roscopeco.ormdroid.Entity;

/**
 * An action is an opaque json blob that encapsulates server-side actions spawned from Unity.
 * The platform simply queues and transmits them to the server.
 * 
 * Consistency model: Client can add.
 *                    Removed from client when synced to server.
 */
public class Action extends Entity {

	@JsonIgnore
	public int id;
	public String json;

	public Action() {
	}
	
	public Action(String json) {
		this.json = json;
	}
	
	@JsonValue
        @JsonRawValue
	public String toJson() {
	    return json;
	}

    public static class ChallengeAttemptAction {
        public static final String action = "challenge_attempt";
        public int challenge_id;
        public int[] track_id = new int[2];

        public ChallengeAttemptAction(int challengeId, Track track) {
            this.challenge_id = challengeId;
            this.track_id[0] = track.device_id;
            this.track_id[1] = track.track_id;
        }
    }
}
