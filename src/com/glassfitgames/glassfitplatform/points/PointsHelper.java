package com.glassfitgames.glassfitplatform.points;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;

import com.glassfitgames.glassfitplatform.gpstracker.GPSTracker;
import com.glassfitgames.glassfitplatform.gpstracker.Helper;
import com.glassfitgames.glassfitplatform.models.Position;
import com.glassfitgames.glassfitplatform.models.Track;
import com.glassfitgames.glassfitplatform.models.Transaction;
import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.ORMDroidApplication;

public class PointsHelper {
    
    private PointsHelper pointsHelper = null;
    private GPSTracker gpsTracker = null;
    private Timer timer = new Timer();
    
    // constants to calc points/level/multipliers. May be overriden by values from database in constructor.
    private final long TIME_SINCE_LAST_ACTIVITY;
    private final int BASE_POINTS_PER_METRE = 5;
    private final float BASE_MULTIPLIER_SPEED_THRESH = 0.5f; // m/s
    private final int BASE_MULTIPLIER_LEVELS = 4;
    private final int BASE_MULTIPLIER_PERCENT = 25;
    private final long BASE_MULTIPLIER_TIME_THRESH = 8000;  // ms
    private final long ACTIVITY_COMPLETE_MULTIPLIER_PERCENT = 30;
    private final long ACTIVITY_COMPLETE_MULTIPLIER_LEVELS= 7;
    private final long CHALLENGE_COMPLETE_MULTIPLIER_PERCNET = 100;
    
    private float baseSpeed = 0.0f;
    
                    
    private PointsHelper(Context c) {
        ORMDroidApplication.initialize(c);
        gpsTracker = Helper.getInstance(c).getGPSTracker();
        
        // initialise constants
        TIME_SINCE_LAST_ACTIVITY = System.currentTimeMillis() - Position.getMostRecent().getDeviceTimestamp();
        //TODO: init the other constants from the calibration table
        
        // start checking for points to award!
        timer.scheduleAtFixedRate(task, 0, BASE_MULTIPLIER_TIME_THRESH);
    }
    
    public PointsHelper getInstance(Context c) {
        if (pointsHelper == null) {
            pointsHelper = new PointsHelper(c);
        }
        return pointsHelper;
    }
    
    public void setBaseSpeed(float baseSpeed) {
        this.baseSpeed = baseSpeed;
    }

    public long getPoints() {
        Transaction lastTransaction = Transaction.getLastTransaction();
        long pointsAtLastTransaction = lastTransaction.points_balance;
        return pointsAtLastTransaction + extrapolatePoints();
    }
    
    public void awardPoints(String type, String calc, String source_id, int points_delta) {
        Transaction t = new Transaction(type, calc, source_id, points_delta);
        t.save();
    }
    
    public int extrapolatePoints() {
        if (gpsTracker == null || !gpsTracker.isTracking()) {
            return 0;
        } else {
            long timestampFrom = Transaction.getLastTransaction().ts;
            long timestampTo = System.currentTimeMillis();
            return (int)(gpsTracker.getCurrentSpeed()*(timestampTo-timestampFrom)/1000.0);
        } 
    }
    
    private long lastTimestamp = 0;
    private double lastCumulativeDistance = 0.0;
    
    private TimerTask task = new TimerTask() {
        public void run() {
            if (gpsTracker == null || !gpsTracker.isTracking()) return;
            if (lastTimestamp == 0) {  // 1st loop
                lastTimestamp = System.currentTimeMillis();
                lastCumulativeDistance = gpsTracker.getElapsedDistance();
                return;
            }
            
            // calculate base points
            double awardDistance = gpsTracker.getElapsedDistance() - lastCumulativeDistance;
            int points = (int)awardDistance*BASE_POINTS_PER_METRE;
            String calcString = points + " base";
            
            // apply base multiplier
            if (baseSpeed != 0.0) {
                long awardTime = System.currentTimeMillis() - lastTimestamp;
                float awardSpeed = (float)(awardDistance*1000.0/awardTime);
                float excessSpeed = awardSpeed - baseSpeed;
                
                int level = (int)(excessSpeed / BASE_MULTIPLIER_SPEED_THRESH);
                level = level < 0 ? 0 : level;  // cap between 1 
                level = level > BASE_MULTIPLIER_LEVELS-1 ? BASE_MULTIPLIER_LEVELS-1 : level; // and BASE_MULT_LEVELS-1
                float multiplier = 1 + level*(float)BASE_MULTIPLIER_PERCENT / 100.0f;
                points *= multiplier;
                calcString += " * (1+" + level*BASE_MULTIPLIER_PERCENT + "% base multiplier)";
                // TODO: unity send message with multiplier / bonus points
            }
            
            awardPoints("BASE POINTS", calcString, "PointsHelper.java", extrapolatePoints());
        }
    };
}
