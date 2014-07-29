package com.raceyourself.platform.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raceyourself.platform.gpstracker.Helper;
import com.raceyourself.platform.utils.Utils;
import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.ORMDroidApplication;

import lombok.SneakyThrows;

/**
 * An event is an opaque json blob that encapsulates client-side events spawned from Unity.
 * The platform simply queues and transmits them to the server.
 * 
 * Consistency model: Client can add.
 *                    Removed from client when synced to server.
 */
public class Event extends Entity {

	@JsonIgnore
	public int id;
	public long ts;
    @JsonRawValue
	public String data;
	public int version;
	public int device_id;
	public int session_id;

    private static ObjectMapper om = null;

    private static ObjectMapper getOm() {
        if (om == null) {
            om = new ObjectMapper();
        }
        return om;
    }

    @SneakyThrows(JsonProcessingException.class)
    public static void log(Object event) {
        Event entity = new Event(getOm().writeValueAsString(event));
        entity.save();
    }

	public Event() {
	}
	
	public Event(String json) {
	    this.ts = System.currentTimeMillis();
	    this.version = Utils.PLATFORM_VERSION;
	    Device self = Device.self();
	    if (self != null) this.device_id = Device.self().getId();
	    else this.device_id = 0;
	    this.session_id = Helper.getInstance(ORMDroidApplication.getInstance().getApplicationContext()).sessionId;
        this.data = json;
	}

    public static class EventEvent {
        public final String event_type = "event";
        public final String event_name;

        public EventEvent(String name) {
            this.event_name = name;
        }
    }

    public static class ChallengeEvent extends EventEvent {
        public int[] challenge_id = new int[2];

        public ChallengeEvent(String eventName, int[] challengeId) {
            this(eventName, challengeId[0], challengeId[1]);
        }

        public ChallengeEvent(String eventName, int deviceId, int challengeId) {
            super(eventName);
            challenge_id[0] = deviceId;
            challenge_id[1] = challengeId;
        }
    }

    /// Use this method to record screen transitions so we can understand how users interact with the app
    public static class ScreenEvent {
        public String event_type = "screen";
        public final String screen_name;

        public ScreenEvent(String name) {
            this.screen_name = name;
        }
    }
}
