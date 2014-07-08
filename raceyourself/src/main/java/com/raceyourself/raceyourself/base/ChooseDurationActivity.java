package com.raceyourself.raceyourself.base;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 08/07/2014.
 */
@Slf4j
public abstract class ChooseDurationActivity extends BaseActivity implements SeekBar.OnSeekBarChangeListener {

    private int stepSize = 6;

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

        Bundle bundle = getIntent().getExtras();

        User user = User.get(AccessToken.get().getUserId());

        ImageView playerImage = (ImageView) findViewById(R.id.playerProfilePic);
        String url = user.getImage();
        loadImageIntoImageView(playerImage, url);
    }

    protected void loadImageIntoImageView(final ImageView playerImage, String url) {
        Picasso.with(this).load(url).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                log.info("bitmap loaded correctly");
                Bitmap roundedBitmap = PictureUtils.getRoundedBmp(bitmap, bitmap.getWidth());
                playerImage.setImageBitmap(roundedBitmap);
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                log.info("bitmap failed");
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {}
        });
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
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}
}
