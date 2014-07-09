package com.raceyourself.raceyourself.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.raceyourself.platform.gpstracker.Helper;
import com.raceyourself.platform.models.Challenge;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.ChooseDurationActivity;

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

        ImageView opponentProfileImageView = (ImageView) findViewById(R.id.playerProfilePic);

        loadImageIntoImageView(opponentProfileImageView, opponent.getProfilePictureUrl());
    }

    public void onMatchClick(View view) {
        challengeFriend();

        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);

        // TODO 'challenge sent' confirmation
    }

    private Challenge challengeFriend() {
        Challenge challenge = new Challenge();
        challenge.type = "DurationChallenge";
        challenge.duration = getDuration();
        challenge.isPublic = true;
        challenge.save();
        log.error(String.format("Created a challenge with id <%d,%d>", challenge.device_id, challenge.challenge_id));
        challenge.challengeUser(opponent.getId());
        log.error(String.format("Challenged user %d with challenge <%d,%d>", opponent.getId(), challenge.device_id, challenge.challenge_id));
        return challenge;
    }
}
