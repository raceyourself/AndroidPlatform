package com.raceyourself.raceyourself.home.sendchallenge;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.home.UserBean;
import com.squareup.picasso.Picasso;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 22/07/2014.
 */
@Slf4j
@EViewGroup(R.layout.fragment_friend_item)
public class FriendView extends RelativeLayout {

    @ViewById(R.id.playerName)
    TextView itemView;
    @ViewById(R.id.playerProfilePic)
    ImageView opponentProfilePic;
    @ViewById(R.id.challengeBtn)
    Button button;
    @ViewById(R.id.raceOutcome)
    TextView subtitle;
    @ViewById(R.id.rankIcon)
    ImageView rankIcon;

    @Setter
    private OnFriendAction onFriendAction;

    private Context context;

    public FriendView(Context context) {
        super(context);
        this.context = context;
    }

    public void bind(@NonNull final UserBean friend) {
        itemView.setText(friend.getName());
        Picasso
            .with(context)
            .load(friend.getProfilePictureUrl())
            .placeholder(R.drawable.default_profile_pic)
            .transform(new PictureUtils.CropCircle())
            .into(opponentProfilePic);

        UserBean.JoinStatus status = friend.getJoinStatus();

        if(status == UserBean.JoinStatus.MEMBER_NOT_YOUR_INVITE ||
                status == UserBean.JoinStatus.MEMBER_YOUR_INVITE) {

            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onFriendAction.sendChallenge(friend);
                }
            });
            button.setText(R.string.challenge_button);
            button.setTextColor(getResources().getColor(R.color.challenge_button_text_color));
            button.setBackgroundResource(R.drawable.challenge_friend_button);

            subtitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_coin_small, 0, 0, 0);
            subtitle.setText("500");
            subtitle.setTextColor(Color.parseColor("#ffecbb1e"));
            if (friend.getRank() != null) {
                rankIcon.setImageDrawable(getResources().getDrawable(friend.getRankDrawable()));
                rankIcon.setVisibility(View.VISIBLE);
            } else {
                rankIcon.setVisibility(View.INVISIBLE);
            }
        }
        else {
            subtitle.setCompoundDrawables(null, null, null, null);
            subtitle.setText("Not a member");
            subtitle.setTextColor(Color.parseColor("#a29f94"));
            rankIcon.setVisibility(View.INVISIBLE);

            if (status == UserBean.JoinStatus.NOT_MEMBER) {
                button.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onFriendAction.invite(friend);
                    }
                });
                button.setText(R.string.invite_button);
                button.setTextColor(getResources().getColor(R.color.invite_button_text_color));
                button.setBackgroundResource(R.drawable.invite_friend_button); // TODO put in correct asset
            }
            else if (status == UserBean.JoinStatus.INVITE_SENT) {
                button.setOnClickListener(null);
                button.setText(R.string.invited_button);
                button.setTextColor(getResources().getColor(R.color.invited_button_text_color));
                button.setBackgroundResource(R.drawable.invited_friend_button); // TODO put in correct asset
            }
            else {
                throw new Error(String.format("Unrecognised UserBean.JoinStatus: %s", status));
            }
        }
    }

    public interface OnFriendAction {
        public void invite(UserBean friend);
        public void sendChallenge(UserBean friend);
    }
}
