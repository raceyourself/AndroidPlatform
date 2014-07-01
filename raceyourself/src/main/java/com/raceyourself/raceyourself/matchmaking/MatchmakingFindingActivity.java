package com.raceyourself.raceyourself.matchmaking;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.raceyourself.raceyourself.R;

public class MatchmakingFindingActivity extends Activity {

    TextView matching;
    Animation anim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matchmaking_finding);
        matching = (TextView)findViewById(R.id.matchingText);
        anim = AnimationUtils.loadAnimation(this, R.anim.matched_text_anim);

        matching.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                matching.startAnimation(anim);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.matchmaking_finding, menu);
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
}
