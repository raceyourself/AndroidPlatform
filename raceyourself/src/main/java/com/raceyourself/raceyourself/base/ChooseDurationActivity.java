package com.raceyourself.raceyourself.base;

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
import com.squareup.picasso.Picasso;

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

    protected static final int MIN_DURATION_MINS = 5;
    protected static final int MAX_DURATION_MINS = 30;
    protected static final int STEP_SIZE_MINS = 6;

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
        seekBar.setMax(MAX_DURATION_MINS);

        User user = User.get(AccessToken.get().getUserId());

        ImageView playerImage = (ImageView) findViewById(R.id.playerProfilePic);
        String url = user.getImage();
        Picasso.with(this).load(url).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(playerImage);

        // Non-empty string in XML for ease of layout... but needs to be initialised to empty string.
        TextView warning = (TextView) findViewById(R.id.lengthWarning);
        warning.setText("");
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
        int nSteps = 6;
        progress = (Math.round(progress / nSteps))* nSteps;
        seekBar.setProgress(progress);
        duration = ((progress / nSteps) + 1) * STEP_SIZE_MINS;
        if(duration == 0) {
            duration = MIN_DURATION_MINS;
        }
        textView.setText(duration + "");
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    public abstract void onMatchClick(View view);
}
