package com.raceyourself.raceyourself.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.raceyourself.platform.gpstracker.Helper;
import com.raceyourself.raceyourself.base.ChooseDurationActivity;
import com.raceyourself.raceyourself.matchmaking.MatchmakingFindingActivity;

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

        opponent = (UserBean) savedInstanceState.getSerializable("opponent");
    }

    public void onMatchClick(View view) {
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
