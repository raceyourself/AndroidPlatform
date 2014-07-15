package com.raceyourself.platform.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.Query;

import java.util.Date;
import java.util.List;

import lombok.SneakyThrows;

import static com.roscopeco.ormdroid.Query.eql;

/**
 * A notification to be displayed to the user.
 * 
 * Consistency model: Client can mark notifications as read. Client can create synthetic client-
 *                    side-only notifications.
 *                    Server can upsert using server id.
 */
public class Notification extends Entity {

	public int id;
	public boolean read = false;
	@JsonRawValue
	public String message;
    public Date deleted_at;

    @JsonIgnore
    public String type;

	@JsonIgnore
	public boolean dirty = false;

	public Notification() {
	}

    public Notification(ChallengeNotification challenge) throws JsonProcessingException {
        this(new ObjectMapper().writeValueAsString(challenge), "challenge");
    }
    public Notification(String message, String type) {
        this.id = -Sequence.getNext("dummy_id"); // Negative id symbolizes dummy ids
        this.read = false;
        this.message = message;
        this.type = type;
        this.dirty = true;
    }

    public static Notification get(int id) {
        return query(Notification.class).where(Query.eql("id", id)).execute();
    }

    public static List<Notification> getNotifications() {
		return query(Notification.class).executeMulti();
	}

    public static List<Notification> getNotificationsByType(String type) {
        return query(Notification.class).where(eql("type", type)).executeMulti();
    }

    public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		if (this.read != read) dirty = true;
		this.read = read;
        this.save();
	}

	public String getMessage() {
		return message;
	}
	
	public void setMessage(JsonNode node) {
        this.message = node.toString();
        // Extract message type so that we can query on it
        if (node.has("type")) this.type = node.get("type").textValue();
	}

	public void flush() {
        if (id <= 0) {
            // Synthetic notification
            super.delete();
            return;
        }
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
