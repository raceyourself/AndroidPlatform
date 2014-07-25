package com.raceyourself.raceyourself.shop;

import android.app.Activity;
import android.widget.Toast;

import com.raceyourself.raceyourself.R;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 22/07/2014.
 */
@Slf4j
@EActivity(R.layout.activity_shop)
public class ShopActivity extends Activity {

    @AfterViews
    public void afterViews() {
        Toast.makeText(this, "For illustration only - store functionality coming soon.", Toast.LENGTH_LONG).show();
    }
}
