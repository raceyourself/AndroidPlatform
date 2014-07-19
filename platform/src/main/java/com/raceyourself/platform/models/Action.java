package com.raceyourself.platform.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roscopeco.ormdroid.Entity;

import lombok.SneakyThrows;

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

    private static ObjectMapper om = null;

	public Action() {
	}
	
	public Action(String json) {
		this.json = json;
	}

    private static ObjectMapper getOm() {
        if (om == null) {
            om = new ObjectMapper();
        }
        return om;
    }

    @SneakyThrows(JsonProcessingException.class)
    public static void queue(Object action) {
        Action entity = new Action(getOm().writeValueAsString(action));
        entity.save();
    }

	@JsonValue
        @JsonRawValue
	public String toJson() {
	    return json;
	}

    public static class ChallengeAction {
        public final String action = "challenge";
        public final int[] challenge_id = new int[2];
        public final String target;

        public ChallengeAction(Challenge challenge, int userId) {
            this.challenge_id[0] = challenge.device_id;
            this.challenge_id[1] = challenge.challenge_id;
            this.target = String.valueOf(userId);
        }

        public ChallengeAction(Challenge challenge, String uid) {
            this.challenge_id[0] = challenge.device_id;
            this.challenge_id[1] = challenge.challenge_id;
            this.target = uid;
        }
    }

    public static class ChallengeAttemptAction {
        public final String action = "challenge_attempt";
        public final int[] challenge_id = new int[2];
        public final int[] track_id = new int[2];
        public final Integer notification_id;

        public ChallengeAttemptAction(Challenge challenge, Track track) {
            this(challenge, track, null);
        }

        public ChallengeAttemptAction(Challenge challenge, Track track, Integer notificationId) {
            this.challenge_id[0] = challenge.device_id;
            this.challenge_id[1] = challenge.challenge_id;
            this.track_id[0] = track.device_id;
            this.track_id[1] = track.track_id;
            this.notification_id = notificationId;
        }
    }

    public static class AcceptChallengeAction {
        public final String action = "accept_challenge";
        public final int[] challenge_id = new int[2];

        public AcceptChallengeAction(Challenge challenge) {
            this.challenge_id[0] = challenge.device_id;
            this.challenge_id[1] = challenge.challenge_id;
        }
    }

    public static class ShareActivityAction {
        public final String action = "share_activity";
        public final int notification_id;

        public ShareActivityAction(int notificationId) {
            this.notification_id = notificationId;
        }
    }
}
