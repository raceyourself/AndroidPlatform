package com.raceyourself.platform.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.roscopeco.ormdroid.Entity;

import java.util.List;

import static com.roscopeco.ormdroid.Query.and;
import static com.roscopeco.ormdroid.Query.eql;

public class Mission extends Entity {
    public String id;

    public static Mission get(String mission) {
        return query(Mission.class).where(eql("id", mission)).execute();
    }

    public static int getLevelCount(String mission) {
        return (Integer)query(MissionLevel.class).where(eql("mission", mission)).max("level").executeAggregate();
    }

    public int getLevelCount() {
        return getLevelCount(this.id);
    }

    public static Challenge getLevel(String mission, int level) {
        MissionLevel lvl = query(MissionLevel.class).where(and(eql("mission", mission), eql("level", level))).execute();
        return query(Challenge.class).where(and(eql("device_id", lvl.device_id), eql("challenge_id", lvl.challenge_id))).execute();
    }

    public static List<MissionLevel> getLevels(String mission) {
        return query(MissionLevel.class).where(eql("mission", mission)).executeMulti();
    }

    public List<MissionLevel> getLevels() {
        return getLevels(this.id);
    }

    public void setLevels(List<MissionLevel> levels) {
        for (MissionLevel level : levels) {
            level.mission = this.id;
            level.save();
        }
        // TODO: Remove orphanssy
    }

    public static class MissionLevel extends Entity {
        @JsonIgnore
        public String id;
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

        @Override
        public int save() {
            if (id == null) {
                id = mission + '-' + level;
            }
            return super.save();
        }
    }
}
