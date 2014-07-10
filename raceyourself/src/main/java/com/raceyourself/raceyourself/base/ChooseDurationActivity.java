package com.raceyourself.raceyourself.base;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.matchmaking.MatchmakingFindingActivity;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Used for multiple purposes - choosing duration for quickmatch and for setting a challenge.
 *
 * Created by Duncan on 08/07/2014.
 */
@Slf4j
public abstract class ChooseDurationActivity extends BaseActivity implements SeekBar.OnSeekBarChangeListener {

    @Getter(AccessLevel.PROTECTED)
    private int duration;

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_duration);
        textView = (TextView)findViewById(R.id.duration);
        SeekBar seekBar = (SeekBar)findViewById(R.id.matchmaking_distance_bar);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setMax(30);

//        Bundle bundle = getIntent().getExtras();

        User user = User.get(AccessToken.get().getUserId());

        ImageView playerImage = (ImageView) findViewById(R.id.playerProfilePic);
        String url = user.getImage();
        Picasso.with(this).load(url).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(playerImage);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.matchmaking_distance, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int stepSize = 6;
        progress = (Math.round(progress/ stepSize))* stepSize;
        seekBar.setProgress(progress);
        duration = ((progress / stepSize) + 1) * 5;
        if(duration == 0) {
            duration = 5;
        }
        textView.setText(duration + "");
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    public abstract void onMatchClick(View view);
}
