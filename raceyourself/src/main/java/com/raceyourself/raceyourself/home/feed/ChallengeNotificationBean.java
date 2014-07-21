package com.raceyourself.raceyourself.home.feed;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.Challenge;
import com.raceyourself.platform.models.ChallengeNotification;
import com.raceyourself.platform.models.Notification;
import com.raceyourself.platform.models.Track;
import com.raceyourself.raceyourself.home.UserBean;

import org.joda.time.DateTime;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 27/06/2014.
 */
@Slf4j
@Data
public class ChallengeNotificationBean implements Comparable<ChallengeNotificationBean>, HomeFeedRowBean, Serializable {
    private int id;
    private UserBean from;
    private UserBean to;

    // as we receive notifications of races between other people, both fromMe and toMe may be false.
    private boolean fromMe;
    private boolean toMe;

    private DateTime expiry;
    private ChallengeBean challenge;
    private boolean read;
    private DateTime deletedAt;

    private boolean complete;

    public ChallengeNotificationBean() {}

    public ChallengeNotificationBean(Notification notification) throws IOException {
        ObjectMapper om = new ObjectMapper();
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.setVisibilityChecker(om.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        ChallengeNotification cNotif = om.readValue(notification.getMessage(), ChallengeNotification.class);

        Challenge challenge = Challenge.get(cNotif.device_id, cNotif.challenge_id);
        if (challenge == null) {
            throw new RuntimeException(String.format("Could not find challenge <%d,%d> in db", cNotif.device_id, cNotif.challenge_id));
        }

        id = notification.id;

        // Each challenge's expiry should be between yesterday and two days from now.
        setExpiry(new DateTime(challenge.stop_time));

        setRead(notification.isRead());

        ChallengeBean chal = new ChallengeBean(challenge);
        chal.setChallengeGoal(challenge.duration);
        chal.setType("duration");
        setChallenge(chal);

        if (notification.deleted_at != null) // unproven theory: DateTime turns null Date into Unix timestamp (1970...)
            setDeletedAt(new DateTime(notification.deleted_at));

        // FIXME somewhere, null deleted_at values are being mapped to the Unix epoch - 1st Jan, 1970...
        if (getDeletedAt().year().get() == 1970)
            setDeletedAt(null);

        if (cNotif.from == AccessToken.get().getUserId())
            fromMe = true;
        if (cNotif.to == AccessToken.get().getUserId())
            toMe = true;

        from = new UserBean();
        from.setName("?");
        from.setId(cNotif.from);

        to = new UserBean();
        to.setName("?");
        to.setId(cNotif.to);

        Boolean racedByFrom = false;
        Boolean racedByTo = false;
        if (challenge != null) {
            for (Challenge.ChallengeAttempt attempt : challenge.getAttempts()) {
                if (attempt.user_id == cNotif.from) {
                    racedByFrom = true;
                } else if (attempt.user_id == cNotif.to) {
                    racedByTo = true;
                }
                if (racedByFrom && racedByTo) {
                    complete = true;
                    break;
                }
            }
        }

//	    from.setShortName(StringFormattingUtils.getForenameAndInitial(from.getName()));
    }

    public static List<ChallengeNotificationBean> from(List<Notification> notifications) {
        List<ChallengeNotificationBean> beans = new ArrayList<ChallengeNotificationBean>(notifications.size());
        for (Notification notification : notifications) {
            try {
                beans.add(new ChallengeNotificationBean(notification));
            } catch (Throwable e) {
                log.error("Notification " + notification.id + " is malformed", e);
            }
        }
        Collections.sort(beans);
        return beans;
    }

    @Override
    public int compareTo(ChallengeNotificationBean another) {
        if (read != another.read)
            return Boolean.valueOf(read).compareTo(another.read);
        if (expiry != null && another.expiry != null)
            return expiry.compareTo(another.expiry);
        return Integer.valueOf(getId()).compareTo(another.getId());
    }

    public boolean isInbox() {
        return !read && toMe && !complete && deletedAt == null;
    }

    public boolean isRunnableNow() {
        return read && toMe && !complete && deletedAt == null;
    }

    public boolean isActivity() {
        return complete && deletedAt == null;
    }

    public UserBean getOpponent() {
        if (fromMe)
            return to;
        if (toMe)
            return from;
        throw new IllegalStateException(
                "This challenge doesn't involve the player. Use getFrom() and getTo() instead.");
    }

    public void setOpponent(UserBean user) {
        if (fromMe)
            to = user;
        else if (toMe)
            from = user;
        else
            throw new IllegalStateException(
                "This challenge doesn't involve the player. Use setFrom() and setTo() instead.");
    }

    // explicit hashCode/equals rather than Lombok generation because we only want to use id field.

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if(o instanceof ChallengeNotificationBean) {
            return getId() == ((ChallengeNotificationBean)o).getId();
        } else
            return false;
    }

    @Override
    public int hashCode() {
        return getId();
    }
}
