package com.raceyourself.raceyourself.matchmaking;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.BaseActivity;
import com.raceyourself.raceyourself.base.ChooseDurationActivity;
import com.raceyourself.raceyourself.utils.PictureUtils;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class MatchmakingDurationActivity extends ChooseDurationActivity {

    public void onMatchClick(View view) {
        Intent intent = new Intent(this, MatchmakingFindingActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt("duration", getDuration());
        intent.putExtras(bundle);
        startActivity(intent);
    }
}
