package com.raceyourself.platform.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.roscopeco.ormdroid.Entity;

import java.util.Date;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MissionClaim extends Entity {
    @JsonIgnore
    public String id;

    public String mission_id;
    public int level;

    public Date created_at;
    public Date deleted_at;

    @JsonIgnore
    public boolean dirty = false;

    public MissionClaim() {}

    public MissionClaim(String missionId, int level) {
        this.mission_id = missionId;
        this.level = level;
        this.dirty = true;
    }

    @Override
    public int save() {
        if (id == null) {
            id = mission_id + '-' + level;
        }
        return super.save();
    }

    public void flush() {
        if (dirty) {
            super.delete();
            return;
        }
        if (deleted_at != null) {
            super.delete();
            return;
        }
    }
}
