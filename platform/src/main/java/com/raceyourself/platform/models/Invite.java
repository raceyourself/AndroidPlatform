package com.raceyourself.platform.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.roscopeco.ormdroid.Entity;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;

public class Invite extends EntityCollection.CollectionEntity {
    public String code;     // Read-only
    public Date expires_at; // Read-only
    public Date used_at;    // Read-only
    public String identity_type;
    public String identity_uid;

    @JsonIgnore
    public boolean dirty = false;

    public Invite() {}

    public void inviteFriend(Friend friend) {
        this.identity_type = StringUtils.capitalize(friend.provider) + "Identity";
        this.identity_uid = friend.uid;
        this.dirty = false;
    }

    public void inviteEmail(String email) {
        this.identity_type = "EmailIdentity";
        this.identity_uid = email;
        this.dirty = false;
    }

    public void flush() {
        if (dirty) {
            dirty = false;
            save();
        }
    }
}
