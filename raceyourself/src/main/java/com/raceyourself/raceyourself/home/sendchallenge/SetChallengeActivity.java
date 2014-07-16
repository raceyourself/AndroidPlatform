package com.raceyourself.raceyourself.home.sendchallenge;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.Challenge;
import com.raceyourself.platform.models.ChallengeNotification;
import com.raceyourself.platform.models.Notification;
import com.raceyourself.raceyourself.MobileApplication;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.ChooseDurationActivity;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.home.HomeActivity;
import com.raceyourself.raceyourself.home.UserBean;
import com.raceyourself.raceyourself.home.feed.ChallengeFragment;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 08/07/2014.
 */
@Slf4j
public class SetChallengeActivity extends ChooseDurationActivity {
    private UserBean opponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        opponent = (UserBean) extras.getSerializable("opponent");

        Button findBtn = (Button) findViewById(R.id.findBtn);
        findBtn.setText("Send Challenge");

        TextView opponentName = (TextView) findViewById(R.id.opponentName);
        opponentName.setText(opponent.getName());

        ImageView opponentProfileImageView = (ImageView) findViewById(R.id.opponentProfilePic);

        Picasso.with(this).load(opponent.getProfilePictureUrl()).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(opponentProfileImageView);
    }

    @Override
    public void onMatchClick(View view) {
        challengeFriend();
        ((MobileApplication)getApplication()).sendMessage(ChallengeFragment.class.getSimpleName(), ChallengeFragment.MESSAGING_MESSAGE_REFRESH);

        Intent intent = new Intent(this, HomeActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("alert",
                String.format(getString(R.string.challenge_enqueue_notification), opponent.getName()));
        intent.putExtras(bundle);
        startActivity(intent);
        finish();
    }

    @SneakyThrows(JsonProcessingException.class)
    private Challenge challengeFriend() {
        Challenge challenge = new Challenge();
        challenge.type = "duration";
        challenge.duration = getDuration()*60;
        challenge.isPublic = true;
        challenge.start_time = new Date();
        Calendar expiry = new GregorianCalendar();
        expiry.add(Calendar.HOUR, 48);
        challenge.stop_time = expiry.getTime();
        challenge.save();
        log.info(String.format("Created a challenge with id <%d,%d>", challenge.device_id, challenge.challenge_id));
        challenge.challengeUser(opponent.getId());
        log.info(String.format("Challenged user %d with challenge <%d,%d>", opponent.getId(), challenge.device_id, challenge.challenge_id));
        Notification synthetic = new Notification(new ChallengeNotification(AccessToken.get().getUserId(), opponent.getId(), challenge));
        synthetic.save();
        log.info(String.format("Created synthetic notification %d for challenge <%d,%d> to user %d", synthetic.id, challenge.device_id, challenge.challenge_id, opponent.getId()));
        return challenge;
    }
}
