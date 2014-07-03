package com.raceyourself.raceyourself.matchmaking;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.raceyourself.raceyourself.R;

public class ChooseFitnessActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_fitness);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.choose_fitness, menu);
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

    public void onFitnessBtn(View view) {
        Intent matchmakingIntent = new Intent(this, MatchmakingDistanceActivity.class);
        Bundle extras = new Bundle();
        switch(view.getId()) {

            case R.id.outOfShape:
                Log.i("ChooseFitnessActivity", "Out of shape chosen");
                extras.putString("fitness", "out of shape");
                break;
            case R.id.averageBtn:
                Log.i("ChooseFitnessActivity", "Average chosen");
                extras.putString("fitness", "average");
                break;
            case R.id.athleticBtn:
                Log.i("ChooseFitnessActivity", "Athletic chosen");
                extras.putString("fitness", "athletic");
                break;
            case R.id.eliteBtn:
                Log.i("ChooseFitnessActivity", "Elite chosen");
                extras.putString("fitness", "elite");
                break;
            default:
                Log.i("ChooseFitnessActivity", "id not found");
                return;
        }
        matchmakingIntent.putExtras(extras);
        startActivity(matchmakingIntent);
    }
}
