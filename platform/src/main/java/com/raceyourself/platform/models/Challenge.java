package com.raceyourself.platform.models;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.ORMDroidApplication;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static com.roscopeco.ormdroid.Query.eql;

/**
 * A challenge.
 * 
 * Consistency model: Client can implicitly create using Actions.
 *                    Server can upsert using id.
 */
public class Challenge extends EntityCollection.CollectionEntity {

    public int id;
    public Date start_time;
    public Date stop_time;
    @JsonProperty("public")
    public boolean isPublic;
    public int creator_id;
    public int distance;
    public int duration;
    public String name;
    public String description;
    public int points_awarded;
    public String prize;
    public String type;

    private List<ChallengeAttempt> transientAttempts = new LinkedList<ChallengeAttempt>();
    private List<ChallengeFriend> transientFriends = new LinkedList<ChallengeFriend>();

    public Date deleted_at;

    public Challenge() {
    }

    public static Challenge get(int id) {
        return query(Challenge.class).where(eql("id", id)).execute();
    }
    
    public static List<Challenge> getPersonalChallenges() {
        EntityCollection defaultCollection = EntityCollection.getDefault();        
        return defaultCollection.getItems(Challenge.class);
    }

    public void setAttempts(List<ChallengeAttempt> attempts) {
        for (ChallengeAttempt attempt : attempts) {
            attempt.challenge_id = this.id;
            transientAttempts.add(attempt);
        }
    }

    public void addAttempt(Track track) {
        try {
            Action.ChallengeAttemptAction aa = new Action.ChallengeAttemptAction(this.id, track);
            Action action = new Action(new ObjectMapper().writeValueAsString(aa));
            action.save();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Should never happen", e);
        }
        // Store attempt client-side (will be replaced by server on sync)
        ChallengeAttempt attempt = new ChallengeAttempt(this.id, track);
        attempt.save();
    }

    public List<ChallengeAttempt> getAttempts() {
        return query(ChallengeAttempt.class).where(eql("challenge_id", this.id)).executeMulti();
    }

    public void clearAttempts() {
        List<ChallengeAttempt> attempts = getAttempts();
        for (ChallengeAttempt attempt : attempts) {
            attempt.delete();
        }
    }

    public void setFriends(List<Integer> friends) {
        for (Integer friendId : friends) {
            ChallengeFriend friend = new ChallengeFriend(this.id, friendId);
            transientFriends.add(friend);
        }
    }

    public List<ChallengeFriend> getFriends() {
        return query(ChallengeFriend.class).where(eql("challenge_id", this.id)).executeMulti();
    }

    public void clearFriends() {
        List<ChallengeFriend> friends = getFriends();
        for (ChallengeFriend friend : friends) {
            friend.delete();
        }
    }

    @Override
    public int store() {
        int ret = -1;
        ORMDroidApplication.getInstance().beginTransaction();
        try {
            clearAttempts();
            for (ChallengeAttempt attempt : transientAttempts) {
                // Foreign key may be null if fields deserialized in wrong order, update.
                attempt.challenge_id = this.id;
                attempt.save();
            }
            clearFriends();
            for (ChallengeFriend friend : transientFriends) {
                // Foreign key may be null if fields deserialized in wrong order, update.
                friend.challenge_id = this.id;
                friend.save();
            }
            ret = super.store();
            ORMDroidApplication.getInstance().setTransactionSuccessful();
        } finally {
            transientAttempts.clear();
            transientFriends.clear();
            ORMDroidApplication.getInstance().endTransaction();
        }
        return ret;
    }

    @Override
    public void delete() {
        clearAttempts();
        clearFriends();
        super.delete();
    }

    @Override
    public void erase() {
        delete();
    }

    public static class ChallengeAttempt extends Entity {
        @JsonIgnore
        public int id; // auto-incremented
        @JsonIgnore
        public int challenge_id;

        public int device_id;
        public int track_id;
        public int user_id;

        public ChallengeAttempt() {}

        public ChallengeAttempt(int challengeId, Track track) {
            this.challenge_id = challengeId;
            this.device_id = track.device_id;
            this.track_id = track.track_id;
            this.user_id = track.user_id;
        }
    }

    public static class ChallengeFriend extends Entity {
        @JsonIgnore
        public int id; // auto-incremented
        @JsonIgnore
        public int challenge_id;

        public int friend_id;

        public ChallengeFriend() {}

        public ChallengeFriend(int challengeId, int friendId) {
            this.challenge_id = challengeId;
            this.friend_id = friendId;
        }
    }
}
