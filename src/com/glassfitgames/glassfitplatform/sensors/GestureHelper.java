package com.glassfitgames.glassfitplatform.sensors;

import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.qualcomm.QCARUnityPlayer.QCARPlayerActivity;
import com.unity3d.player.UnityPlayer;

public class GestureHelper extends QCARPlayerActivity implements GestureDetector.OnGestureListener {
	private GestureDetector gestureDetector;

	@Override
	 public void onCreate(Bundle savedInstanceState) {
		 super.onCreate(savedInstanceState);
		 gestureDetector = new GestureDetector(this, this);
	 }
	
	@Override
    protected void onResume() {
        super.onResume();
    }
	
	@Override
    protected void onPause() {
        super.onPause();
	}

	 @Override
	 public boolean onGenericMotionEvent(MotionEvent event) {
		gestureDetector.onTouchEvent(event);
		return true;
	}

	/**
	 * This function is for the GestureDetector for Google Glass 
	 * It is called when the TrackPad is pressed
	 */
	@Override
	public boolean onDown(MotionEvent e) {
		
		return true;
	}

	/**
	 * This function is for the GestureDetector for Google Glass 
	 * It is called when a fast swipe occurs on the trackpad
	 */
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		if(velocityX > 3000) {
			 try {
		            UnityPlayer.UnitySendMessage("Scriptholder", "flingRight", null);
		        } catch (UnsatisfiedLinkError e) {
		            Log.i("GPSTracker","Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
		            Log.i("GPSTracker",e.getMessage());
		        }
		}
			
		if(velocityX < -3000) {
			 try {
		            UnityPlayer.UnitySendMessage("Scriptholder", "flingLeft", null);
		        } catch (UnsatisfiedLinkError e) {
		            Log.i("GPSTracker","Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
		            Log.i("GPSTracker",e.getMessage());
		        }
		}
		
		if(velocityY < -3000) {
			try {
	            UnityPlayer.UnitySendMessage("Scriptholder", "flingDown", null);
	        } catch (UnsatisfiedLinkError e) {
	            Log.i("GPSTracker","Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
	            Log.i("GPSTracker",e.getMessage());
	        }
		}
		
		if(velocityY > 3000) {
			try {
	            UnityPlayer.UnitySendMessage("Scriptholder", "flingUp", null);
	        } catch (UnsatisfiedLinkError e) {
	            Log.i("GPSTracker","Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
	            Log.i("GPSTracker",e.getMessage());
	        }
		}
		return true;
	}

	/**
	 * This function is for the GestureDetector for Google Glass 
	 * It is called when a long press occurs on the trackpad
	 */
	@Override
	public void onLongPress(MotionEvent e) {
		
	}

	/**
	 * This function is for the GestureDetector for Google Glass 
	 * It is called when a slow scroll occurs on the trackpad
	 */
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		
		return true;
	}

	/**
	 * This function is for the GestureDetector for Google Glass 
	 * It is called when a down MotionEvent occurs and has not 
	 * moved or released
	 */
	@Override
	public void onShowPress(MotionEvent e) {
		
	}

	/**
	 * This function is for the GestureDetector for Google Glass 
	 * It is called when the TrackPad is tapped
	 */
	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		 try {
	            UnityPlayer.UnitySendMessage("Scriptholder", "isTap", null);
	        } catch (UnsatisfiedLinkError er) {
	            Log.i("GPSTracker","Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
	            Log.i("GPSTracker",er.getMessage());
	        }
		return true;
	}
	  
}
