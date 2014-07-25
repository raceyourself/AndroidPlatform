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


    /// Json-encoded event values. TODO supported:
    //    Launch app (event_name = "launch")
    //    Successfully signed up via facebook (event_name = "signup", provider = "facebook")
    //    Successfully signed up via email (event_name = "signup", provider = "facebook")
    //    Start race (event_name = "start_race", track_id = "xxx")
    //    End race (event_name = "end_race", result = "win/loss", track_id="xxx")
    //    Send challenge (event_name = "send_challenge", challenge_id = "xxx")
    //    Accept challenge (event_name = "accept_challenge", challenge_id = "xxx")
    //    Reject challenge (event_name = "reject_challenge", challenge_id = "xxx")
    //    Invite new user (event_name = "invite", invite_code = "xxx345x", provider = "facebook/email")
    //    Share (event_name = "share", provider = "facebook/twitter/google+")
    //    Rate (event_name = "rate", provider = "Apple store / Android store / Like on facebook")
    public static class EventEvent {
        public final String event_type = "event";
        public final String event_name;
        public long challenge_id;

        public EventEvent(String name) {
            this.event_name = name;
        }

        public EventEvent setChallengeId(long challengeId) {
            this.challenge_id = challengeId;
            return this;
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
