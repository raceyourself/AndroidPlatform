package com.raceyourself.raceyourself.base;

import android.app.Activity;
import android.view.MotionEvent;

import com.stuxbot.lib.uxcam.UXCam;

public class BaseActivity extends Activity {

    @Override
    public void onPause() {
        super.onPause();
//        UXCam.pause();
    }

    @Override
    protected void onStart() {
        super.onStart();
//        UXCam.start(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
//        UXCam.stop();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
//        UXCam.dispatchTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

}
