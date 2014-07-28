package com.raceyourself.platform.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.roscopeco.ormdroid.Entity;

import java.util.Date;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import static com.roscopeco.ormdroid.Query.and;
import static com.roscopeco.ormdroid.Query.eql;

@Slf4j
public class Mission extends Entity {
    public String id;

    public Date deleted_at = null;

    public MissionLevel getCurrentLevel() {
        MissionLevel current = null;
        for (MissionLevel level : getLevels()) {
            current = level;
            if (!level.isClaimed()) break;
        }
        return current;
    }

    public static Mission get(String mission) {
        return query(Mission.class).where(eql("id", mission)).execute();
    }

    public static List<Mission> getMissions() {
        return query(Mission.class).executeMulti();
    }

    public static int getLevelCount(String mission) {
        Integer levels = (Integer)query(MissionLevel.class).where(eql("mission", mission)).max("level").executeAggregate();
        if (levels == null) return 0;
        return levels;
    }

    public int getLevelCount() {
        return getLevelCount(this.id);
    }

    public static Challenge getLevel(String mission, int level) {
        MissionLevel lvl = query(MissionLevel.class).where(and(eql("mission", mission), eql("level", level))).execute();
        return query(Challenge.class).where(and(eql("device_id", lvl.device_id), eql("challenge_id", lvl.challenge_id))).execute();
    }

    public static List<MissionLevel> getLevels(String mission) {
        return query(MissionLevel.class).where(eql("mission", mission)).orderBy("level").executeMulti();
    }

    public List<MissionLevel> getLevels() {
        return getLevels(this.id);
    }

    public void setLevels(List<MissionLevel> levels) {
        for (MissionLevel level : levels) {
            level.mission = this.id;
            level.save();
        }
        // TODO: Remove orphans
    }

    public void flush() {
        if (deleted_at != null) {
            super.delete();
            return;
        }
    }

    public static class MissionLevel extends Entity {
        @JsonIgnore
        public String id;
        @JsonProperty("mission_id")
        public String mission;
        public int level;

        public int device_id;
        public int challenge_id;

        public MissionLevel() {}

        public MissionLevel(String mission, int level, Challenge challenge) {
            this.mission = mission;
            this.level = level;
            this.device_id = challenge.device_id;
            this.challenge_id = challenge.challenge_id;
        }

        public Challenge getChallenge() {
            return Challenge.get(device_id, challenge_id);
        }

        public void setChallenge(Challenge challenge) {
            this.device_id = challenge.device_id;
            this.challenge_id = challenge.challenge_id;
            challenge.save();
        }

        public boolean isCompleted() {
            Challenge challenge = getChallenge();
            if (challenge == null) throw new Error("Challenge for mission " + mission + " level " + level + " is not in db!");
            return challenge.isCompleted();
        }

        public boolean isClaimed() {
            return query(MissionClaim.class).where(and(eql("mission_id", mission), eql("level", level))).execute() != null;
        }

        public boolean claim() {
            if (isClaimed()) return false;
            MissionClaim claim = new MissionClaim(mission, level);
            claim.save();
            return true;
        }

        @Override
        public int save() {
            if (id == null) {
                id = mission + '-' + level;
            }
            return super.save();
        }
    }
}
