package com.raceyourself.raceyourself.matchmaking;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.raceyourself.platform.auth.AuthenticationActivity;
import com.raceyourself.raceyourself.R;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import java.io.IOException;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Amerigo on 25/07/2014.
 */
@Slf4j
@EViewGroup(R.layout.activity_choose_fitness)
public class FitnessView extends RelativeLayout {

    @ViewById(R.id.outOfShape)
    RadioButton outOfShapeButton;
    @ViewById
    RadioButton averageBtn;
    @ViewById
    RadioButton athleticBtn;
    @ViewById
    RadioButton eliteBtn;

    @Getter
    String fitness;

    Context context;

    public FitnessView(Context context) {
        super(context);
        this.context = context;
    }

    public FitnessView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        this.context = context;
    }

    public FitnessView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
//
//    @Click
    public String getFitness() {
        if (outOfShapeButton.isChecked()) {
            fitness = "out of shape";
        } else if (averageBtn.isChecked()) {
            fitness = "average";
        } else if (athleticBtn.isChecked()) {
            fitness = "athletic";
        } else if (eliteBtn.isChecked()) {
            fitness = "elite";
        } else {
            log.error("fitness level not found");
            Toast.makeText(context, "Please select a fitness level to continue", Toast.LENGTH_LONG).show();
            return null;
        }

        updateUserFitness();

        return fitness;
    }

    @Background
    public void updateUserFitness() {
        try {
            AuthenticationActivity.editUser(new AuthenticationActivity.UserDiff().profile("running_fitness", fitness));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
