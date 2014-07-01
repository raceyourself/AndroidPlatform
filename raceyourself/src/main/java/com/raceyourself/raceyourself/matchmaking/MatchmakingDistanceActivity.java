package com.raceyourself.raceyourself.matchmaking;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.raceyourself.platform.gpstracker.Helper;
import com.raceyourself.platform.models.User;
import com.raceyourself.platform.models.UserDetail;
import com.raceyourself.raceyourself.R;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

public class MatchmakingDistanceActivity extends Activity implements SeekBar.OnSeekBarChangeListener {

    private int stepSize = 6;

    private int duration;

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matchmaking_distance);
        textView = (TextView)findViewById(R.id.duration);
        SeekBar seekBar = (SeekBar)findViewById(R.id.matchmaking_distance_bar);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setMax(30);

        UserDetail user = UserDetail.get();
        String url = user.getPhotoUri();
        ImageView playerImage = (ImageView)findViewById(R.id.playerProfilePic);
        Picasso.with(this).load(url).into(playerImage);
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
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        progress = ((int)Math.round(progress/stepSize))*stepSize;
        seekBar.setProgress(progress);
        duration = ((progress / stepSize) + 1) * 5;
        if(duration == 0) {
            duration = 5;
        }
        textView.setText(duration + "");
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
