package com.raceyourself.raceyourself.matchmaking;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.raceyourself.raceyourself.base.ChooseDurationActivity;

public class MatchmakingDurationActivity extends ChooseDurationActivity {

    @Override
    public  void onMatchClick(View view) {
        Intent intent = new Intent(this, MatchmakingFindingActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt("duration", getDuration());
        intent.putExtras(bundle);
        startActivity(intent);
    }
}
