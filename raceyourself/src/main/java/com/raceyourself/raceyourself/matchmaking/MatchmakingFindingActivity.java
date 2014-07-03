package com.raceyourself.raceyourself.matchmaking;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.utils.PictureUtils;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class MatchmakingFindingActivity extends Activity {

    TextView matchingText;
    TextView searchingText;
    TextView matrixText;
    TextView foundText;

    ImageView heartIcon;
    ImageView globeIcon;
    ImageView wandIcon;
    ImageView tickIcon;

    Animation translateRightAnim;
    Animation rotationAnim;

    Drawable heartIconDrawable;
    Drawable globeIconDrawable;
    Drawable wandIconDrawable;
    Drawable tickIconDrawable;

    Drawable spinnerIconDrawable;

    Button raceButton;

    int animationCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matchmaking_finding);

        matchingText = (TextView)findViewById(R.id.matchingText);
        searchingText = (TextView)findViewById(R.id.searchingText);
        matrixText = (TextView)findViewById(R.id.matrixText);
        foundText = (TextView)findViewById(R.id.matchedText);

        heartIcon = (ImageView)findViewById(R.id.heartIcon);
        globeIcon = (ImageView)findViewById(R.id.globeIcon);
        wandIcon = (ImageView)findViewById(R.id.wandIcon);
        tickIcon = (ImageView)findViewById(R.id.tickIcon);

        translateRightAnim = AnimationUtils.loadAnimation(this, R.anim.matched_text_anim);
        rotationAnim = AnimationUtils.loadAnimation(this, R.anim.rotating_icon_anim);

        heartIconDrawable = getResources().getDrawable(R.drawable.ic_heart_grey);
        globeIconDrawable = getResources().getDrawable(R.drawable.ic_globe_grey);
        wandIconDrawable = getResources().getDrawable(R.drawable.ic_wand_grey);
        tickIconDrawable = getResources().getDrawable(R.drawable.ic_tick_grey);
        spinnerIconDrawable = getResources().getDrawable(R.drawable.ic_spinner);

        raceButton = (Button)findViewById(R.id.startRaceBtn);

        User user = User.get(AccessToken.get().getUserId());
        String url = user.getImage();
        Log.i("Matchmaking", "url is " + url);

        final ImageView playerImage = (ImageView)findViewById(R.id.playerProfilePic);

        Picasso.with(this).load(url).into(new Target() {

            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                Log.i("Matchmaking", "bitmap loaded correctly");
                Bitmap roundedBitmap = PictureUtils.getRoundedBmp(bitmap, bitmap.getWidth());
                playerImage.setImageBitmap(roundedBitmap);
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                Log.i("Matchmaking", "bitmap failed");
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {}
        });

        translateRightAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                switch(animationCount) {
                    case 0:
                        startImageAnimation(heartIcon);
                        break;

                    case 1:
                        startImageAnimation(globeIcon);
                        break;

                    case 2:
                        startImageAnimation(wandIcon);
                        break;

                    case 3:
                        startImageAnimation(tickIcon);
                        break;
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        rotationAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                switch(animationCount) {
                    case 0:
                        endImageAnimation(heartIcon, heartIconDrawable, searchingText);
                        break;
                    case 1:
                        endImageAnimation(globeIcon, globeIconDrawable, matrixText);
                        break;
                    case 2:
                        endImageAnimation(wandIcon, wandIconDrawable, foundText);
                        break;
                    case 3:
                        tickIcon.setImageDrawable(tickIconDrawable);
                        raceButton.setVisibility(View.VISIBLE);
                        break;
                }
                if(animationCount < 3) {
                    animationCount++;
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        matchingText.startAnimation(translateRightAnim);
    }


    public void startImageAnimation(ImageView imageView) {
        imageView.setImageDrawable(spinnerIconDrawable);
        imageView.setVisibility(View.VISIBLE);
        imageView.startAnimation(rotationAnim);
    }

    public void endImageAnimation(ImageView imageView, Drawable drawable, TextView textView) {
        imageView.setImageDrawable(drawable);
        textView.setVisibility(View.VISIBLE);
        textView.startAnimation(translateRightAnim);
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
