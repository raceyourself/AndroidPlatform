package com.raceyourself.raceyourself.base;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.base.util.StringFormattingUtils;
import com.raceyourself.raceyourself.home.feed.ChallengeDetailBean;
import com.squareup.picasso.Picasso;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.joda.time.Duration;
import org.joda.time.Period;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * For use in any dialog where the user must select the duration for a challenge (this challenge might be for the
 * player to run now, or to be sent to another player).
 *
 * Created by Duncan on 08/07/2014.
 */
@Slf4j
@EViewGroup(R.layout.activity_select_duration)
public abstract class DurationView extends RelativeLayout implements SeekBar.OnSeekBarChangeListener {

    protected static final int MIN_DURATION_MINS = 5;
    protected static final int MAX_DURATION_MINS = 30;
    protected static final int STEP_SIZE_MINS = 5;

    @Getter
    Duration duration;

    @ViewById(R.id.duration)
    protected TextView durationTextView;
    @ViewById(R.id.furthestRunText)
    protected TextView furthestRunTextView;
    @ViewById(R.id.matchmaking_distance_bar)
    protected SeekBar seekBar;
    @Getter
    @ViewById(R.id.playerProfilePic)
    protected ImageView playerProfilePic;
    @ViewById
    protected Button okButton;
    @ViewById
    protected TextView lengthWarning;

    protected Context context;

    @Setter
    private DurationViewListener durationViewListener;

    public DurationView(Context context) {
        super(context);
        this.context = context;
    }

    @AfterViews
    protected void afterViews(){
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setMax(MAX_DURATION_MINS);

        User user = User.get(AccessToken.get().getUserId());
        String url = user.getImage();
        Picasso
            .with(context)
            .load(url)
            .placeholder(R.drawable.default_profile_pic)
            .transform(new PictureUtils.CropCircle())
            .into(playerProfilePic);

        okButton.setText(getButtonTextResId());

        lengthWarning.setVisibility(View.GONE);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) okButton.getLayoutParams();
        params.addRule(RelativeLayout.BELOW, R.id.matchmaking_distance_bar);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int nSteps = 6;
        progress = (Math.round(progress / nSteps))* nSteps;
        seekBar.setProgress(progress);
        int durationMins = ((progress / nSteps) + 1) * MIN_DURATION_MINS;
        if(durationMins == 0) {
            durationMins = MIN_DURATION_MINS;
        }
        duration = Period.minutes(durationMins).toStandardDuration();

        // Number above slider
        durationTextView.setText(String.valueOf(durationMins));

        int resId = getChallengeTextResId();
        String durationStr = StringFormattingUtils.ACTIVITY_PERIOD_FORMAT.print(duration.toPeriod());
        Spanned resolved = Html.fromHtml(String.format(context.getString(resId), durationStr));

        // Challenge title
        furthestRunTextView.setText(resolved);
    }

    protected int getChallengeTextResId() {
        return R.string.duration_description;
    }

    protected abstract int getButtonTextResId();

    @Click(R.id.okButton)
    public void confirmDuration() {
        durationViewListener.onConfirmDuration();
    }

    @Click(R.id.cancelButton)
    public void cancel() {
        durationViewListener.onCancel();
    }

    public interface DurationViewListener extends Cancellable {
        public void onConfirmDuration();
    }
}
