package com.glassfitgames.glassfitplatform.auth;

import com.glassfitgames.glassfitplatform.R;
import com.glassfitgames.glassfitplatform.R.layout;
import com.glassfitgames.glassfitplatform.R.menu;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class AuthenticationActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.authentication, menu);
        return true;
    }

}
