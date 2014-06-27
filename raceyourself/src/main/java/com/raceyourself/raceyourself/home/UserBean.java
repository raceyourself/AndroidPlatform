package com.raceyourself.raceyourself.home;

import android.content.Context;
import android.graphics.Bitmap;

import com.raceyourself.raceyourself.R;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Intentionally cut down from C# and platform equivalent, to de-couple from our DB implementation.
 * Just what we need to display on screen.
 *
 * Created by Duncan on 27/06/2014.
 */
@Slf4j
@Data
public class UserBean {
    private int id;
    private Bitmap profilePicture;
    private String name;
    private JoinStatus joinStatus;

    public enum JoinStatus {
        MEMBER_YOUR_INVITE(R.string.ry_invite_accepted),
        MEMBER_NOT_YOUR_INVITE(R.string.ry_member),
        INVITE_SENT(R.string.ry_invite_sent),
        NOT_MEMBER(R.string.not_ry_member);

        private final int stringId;

        JoinStatus(int stringId) {
            this.stringId = stringId;
        }

        public String getStatusText(Context context) {
            return context.getString(stringId);
        }
    }
}
