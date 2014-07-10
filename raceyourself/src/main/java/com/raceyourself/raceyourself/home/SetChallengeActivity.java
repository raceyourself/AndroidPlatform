package com.raceyourself.raceyourself.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.raceyourself.platform.gpstracker.Helper;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.ChooseDurationActivity;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.squareup.picasso.Picasso;

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

        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);

        // TODO 'challenge sent' confirmation
    }

    private void challengeFriend() {
        Helper.queueAction(String.format("{\"action\":\"challenge\", \"target\":%d,\n" +
                "            \"taunt\" : \"Try beating my track!\",\n" +
                "            \"challenge\" : {\n" +
                "                    \"distance\": %d,\n" +
                "                    \"duration\": %d,\n" +
                "                    \"public\": true,\n" +
                "                    \"start_time\": null,\n" +
                "                    \"stop_time\": null,\n" +
                "                    \"type\": \"duration\"\n" +
                "            }}", opponent.getId(), -1, getDuration()));
    }
}
