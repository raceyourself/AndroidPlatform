package com.raceyourself.raceyourself.shop;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import org.androidannotations.annotations.EActivity;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 22/07/2014.
 */
@Slf4j
@EActivity
public class ShopActivity extends Activity {

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        Toast.makeText(this, "For illustration only - store functionality coming soon.", Toast.LENGTH_LONG).show();
    }
}
