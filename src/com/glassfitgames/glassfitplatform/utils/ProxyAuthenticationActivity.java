package com.glassfitgames.glassfitplatform.utils;

import com.glassfitgames.glassfitplatform.auth.AuthenticationActivity;
import com.unity3d.player.UnityPlayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class ProxyAuthenticationActivity extends Activity {

    private static final int API_ACCESS_TOKEN_REQUEST_ID = 0;
    private final String TAG = this.getClass().getCanonicalName();
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate() called");		
        Intent intent = new Intent(getApplicationContext(), AuthenticationActivity.class);
        this.startActivityForResult(intent, API_ACCESS_TOKEN_REQUEST_ID);		
	}

	/**
	 * Capture activity results and forward them to unity.
	 */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) { 
        super.onActivityResult(requestCode, resultCode, data); 
        switch(requestCode) { 
          case (API_ACCESS_TOKEN_REQUEST_ID) : { 
            Log.d(TAG,"AuthenticationActivity returned with result");
            String text;
            if (resultCode == Activity.RESULT_OK) { 
	            String apiAccessToken = data.getStringExtra(AuthenticationActivity.API_ACCESS_TOKEN);
	            Log.d(TAG,"AuthenticationActivity returned with token: " + apiAccessToken);
	            
	            // token direct from authenticationActivity
	            if (apiAccessToken != null) {
	                text = "Succeeded";
	            } else {
	                text = "Failed";
	            }

            } else {
            	text = "Interrupted";
            }
            // Sending the message needs the unity native library installed:
            try {
                UnityPlayer.UnitySendMessage("Platform", "OnAuthentication", text);
            } catch (UnsatisfiedLinkError e) {
                Log.i(TAG,"Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
                Log.i(TAG,e.getMessage());
            }            
            this.finish();
            break; 
          } 
        } 
      }	
	
}
