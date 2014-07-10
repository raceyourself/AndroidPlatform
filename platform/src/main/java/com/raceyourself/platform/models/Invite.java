package com.raceyourself.platform.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.roscopeco.ormdroid.Column;
import com.roscopeco.ormdroid.Entity;

import org.apache.commons.lang3.StringUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static com.roscopeco.ormdroid.Query.eql;

public class Invite extends EntityCollection.CollectionEntity {
    @Column(primaryKey = true)
    public String code;     // Read-only
    public Date expires_at; // Read-only
    public Date used_at;    // Read-only
    public String identity_type;
    public String identity_uid;

    @JsonIgnore
    public boolean dirty = false;

    public Invite() {}

    public Invite get(String code) {
        return query(Invite.class).where(eql("code", code)).execute();
    }

    public List<Invite> getAll() {
        return query(Invite.class).executeMulti();
    }

    public List<Invite> getUnused() {
        return query(Invite.class).where("used_at IS NULL").executeMulti();
    }

    public void inviteFriend(Friend friend) {
        this.identity_type = StringUtils.capitalize(friend.provider) + "Identity";
        this.identity_uid = friend.uid;
        this.used_at = new Date();
        Calendar expires = new GregorianCalendar();
        expires.add(Calendar.DATE, 14);
        this.expires_at = expires.getTime();
        this.dirty = false;
        this.save();
    }

    public void inviteEmail(String email) {
        this.identity_type = "EmailIdentity";
        this.identity_uid = email;
        this.used_at = new Date();
        Calendar expires = new GregorianCalendar();
        expires.add(Calendar.DATE, 14);
        this.expires_at = expires.getTime();
        this.dirty = false;
        this.save();
    }

    public void flush() {
        if (dirty) {
            dirty = false;
            save();
        }
    }
}
