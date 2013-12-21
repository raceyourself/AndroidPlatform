package com.glassfitgames.glassfitplatform.sensors;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.glassfitgames.glassfitplatform.gpstracker.Helper;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.qualcomm.QCARUnityPlayer.QCARPlayerActivity;
import com.unity3d.player.UnityPlayer;

public class GestureHelper extends QCARPlayerActivity {
		
//	@Override
//	public void onBackPressed() {
//		super.onBackPressed();
//		try {
//			Log.e("GestureHelper", "Message Sent - back");
//			UnityPlayer.UnitySendMessage("Scriptholder", "flingDown", "");
//		} catch (UnsatisfiedLinkError err) {
//			Log.e("GestureHelper", "Error sending message:");
//			Log.e("GestureHelper", err.getMessage());
//		}
//	}

	private GestureDetector mGestureDetector = null;

    	
	private static final int KEY_SWIPE_DOWN = 4;

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
	    if (keyCode == KEY_SWIPE_DOWN)
	    {
	    	Log.e("GestureHelper", "Down swipe");
	        return true;
	    }
	    return false;
	}
	
	@Override
	 public void onCreate(Bundle savedInstanceState) {
		 super.onCreate(savedInstanceState);
		 
		 if (Helper.onGlass()) {
		     mGestureDetector = createGestureDetector(this);		 
		 } else {
		     mGestureDetector = null;
		 }
		 
                Intent intent = getIntent();
                if (intent != null) {
                    if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                        try {
                            UnityPlayer.UnitySendMessage("Platform", "OnActionIntent", intent.getData().toString());
                        } catch (UnsatisfiedLinkError e) {
                            Log.i("GlassFitPlatform","Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
                            Log.i("GlassFitPlatform",e.getMessage());
                        }            
                    }
                }
	 }
	
    @Override
    public void onContentChanged() {
        Log.e("GestureHelper", "Content changed");
        super.onContentChanged();
        View view = this.findViewById(android.R.id.content);
        view.setKeepScreenOn(true);
        recurseViews(view);
    }
    
    private void recurseViews(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup)view;
            for (int i = 0; i < vg.getChildCount(); ++i) {
                View foundView = vg.getChildAt(i);
                if (foundView != null) {
                    foundView.setFocusableInTouchMode(true);
                    recurseViews(foundView);
                }
            }
        }
    }
	
	private GestureDetector createGestureDetector(Context context) {
		GestureDetector gestureDetector = new GestureDetector(context);
		
		gestureDetector.setBaseListener(new GestureDetector.BaseListener() {
			
			@Override
			public boolean onGesture(Gesture gesture) {
				if(gesture == Gesture.TAP) {
					Log.i("GestureHelper", "One tap detected, sending message");
					try {
						UnityPlayer.UnitySendMessage("Scriptholder", "IsTap", "");
						Log.i("GestureHelper", "Message Sent: Tap");
					} catch (UnsatisfiedLinkError er) {
						Log.e("GestureHelper",
								"Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
						Log.e("GestureHelper", er.getMessage());
					}
					return true;
				} else if(gesture == Gesture.SWIPE_LEFT) {
					Log.i("GestureHelper", "Swipe left detected, sending message");
					try {
						UnityPlayer.UnitySendMessage("Scriptholder", "FlingLeft", "");
						Log.i("GestureHelper", "Message sent: fling left");
					} catch (UnsatisfiedLinkError er) {
						Log.e("GestureHelper", "Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
						Log.e("GestureHelper", er.getMessage());
					}
					return true;
				} else if(gesture == Gesture.SWIPE_RIGHT) {
					Log.i("GestureHelper", "Swipe right detected, sending message");
					try {
						UnityPlayer.UnitySendMessage("Scriptholder", "FlingRight", "");
						Log.i("GestureHelper", "Message sent: fling right");
					} catch (UnsatisfiedLinkError er) {
						Log.e("GestureHelper", "Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
						Log.e("GestureHelper", er.getMessage());
					}
				} else if(gesture == Gesture.SWIPE_UP) {
					Log.i("GestureHelper", "Up swipe");
					return true;
				} else if(gesture == Gesture.TWO_SWIPE_LEFT) {
					Log.i("GestureHelper", "Two swipe left");
					try {
					UnityPlayer.UnitySendMessage("Scriptholder", "TwoSwipeLeft", "");
					} catch (UnsatisfiedLinkError er) {
						Log.e("GestureHelper", "Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
					}
					return true;
				} else if(gesture == Gesture.TWO_TAP) {
					Log.i("GestureHelper", "Two tap detected, sending message");
					try {
						UnityPlayer.UnitySendMessage("Scriptholder", "TwoTap", "");
						Log.i("GestureHelper", "Message sent: two tap");
					} catch (UnsatisfiedLinkError er) {
						Log.e("GestureHelper", "Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
						Log.e("GestureHelper", er.getMessage());
					}
					return true;
				} else if(gesture == Gesture.THREE_TAP) {
					Log.i("GestureHelper", "Three tap detected, sending message");
					try {
						UnityPlayer.UnitySendMessage("Scriptholder", "ThreeTap", "");
						Log.i("GestureHelper", "Message sent: three tap");
					} catch (UnsatisfiedLinkError er) {
						Log.e("GestureHelper", "Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
						Log.e("GestureHelper", er.getMessage());
					}
				}
				return false;
			}
		});
		gestureDetector.setFingerListener(new GestureDetector.FingerListener() {
            @Override
            public void onFingerCountChanged(int previousCount, int currentCount) {
              // do something on finger count changes
            }
        });
        gestureDetector.setScrollListener(new GestureDetector.ScrollListener() {
            @Override
            public boolean onScroll(float displacement, float delta, float velocity) {
                // do something on scrolling
            	return true;
            }
        });
        return gestureDetector;
	}
	
	@Override
    public boolean onGenericMotionEvent(MotionEvent event) {
		Log.e("GestureHelper", "Gesture processing");
        if (mGestureDetector != null) {
            return mGestureDetector.onMotionEvent(event);
        }
        return false;
    }
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		Log.e("GestureHelper", "On touch called");
		super.onTouchEvent(event);
		return true;
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
	protected void onDestroy() {
		Log.e("GestureHelper", "On destroy has been called");
		super.onDestroy();
	}
	
	@Override
	protected void onStop() {
		Log.e("GestureHelper", "On stop called");
		super.onStop();
	}

	
}
