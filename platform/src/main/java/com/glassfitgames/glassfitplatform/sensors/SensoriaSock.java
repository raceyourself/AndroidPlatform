package com.glassfitgames.glassfitplatform.sensors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.raceyourself.platform.R;

import android.content.Context;
import android.util.Log;

public class SensoriaSock {
    
    private final int INTERVAL = 20; // sample time in milliseconds
    
    private ArrayList<Sample> demoData = new ArrayList<Sample>();
    
    public SensoriaSock(Context c) {
        try {
            loadDemoData(c);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public float[] getPressureSensorValues(long milliseconds) {
        
        // if no data, return zeros
        if (demoData.size() == 0) {
            return new float[] {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        }
        
        // translate time to nearest sample
        int offset = (int) (milliseconds / INTERVAL % demoData.size());
        Sample s = demoData.get(offset);
        return new float[] {
                s.leftInsidePressure,
                s.leftOutsidePressure,
                s.leftHeelPressure,
                s.rightInsidePressure,
                s.rightOutsidePressure,
                s.rightHeelPressure
        };
        
    }
    
    private class Sample {
        
        float leftInsidePressure = 0;
        float leftOutsidePressure = 0;
        float leftHeelPressure = 0;
        float rightInsidePressure = 0;
        float rightOutsidePressure = 0;
        float rightHeelPressure = 0;
        
        protected Sample(String[] sensorValues) {
            leftInsidePressure = Float.valueOf(sensorValues[0])/512.0f;
            leftOutsidePressure = Float.valueOf(sensorValues[1])/512.0f;
            leftHeelPressure = Float.valueOf(sensorValues[2])/512.0f;
            rightInsidePressure = Float.valueOf(sensorValues[3])/512.0f;
            rightOutsidePressure = Float.valueOf(sensorValues[4])/512.0f;
            rightHeelPressure = Float.valueOf(sensorValues[5])/512.0f;
        }
        
    }

    public void loadDemoData(Context c) throws IOException {

        // Read the master game list from CSV file:
        InputStream in = c.getResources().openRawResource(
                R.raw.sensoria_demo_data);
        BufferedReader b = new BufferedReader(new InputStreamReader(in));
        b.readLine(); // read (and discard) headers
        String line = null;
        while ((line = b.readLine()) != null) {
            try {
                String[] fields = line.split(",");
                demoData.add(new Sample(fields));
            } catch (NumberFormatException e) {
                Log.w("glassfitplatform.sensors.SensoriaSock",
                        "Error in sensoria CSV, invalid number format in this line: " + line);
            } catch (IndexOutOfBoundsException e) {
                Log.w("glassfitplatform.sensors.SensoriaSock",
                        "Error in sensoria CSV, not enough fields present in this line: " + line);
            }
        }
        Log.d("glassfitplatform.sensors.SensoriaSock", "Loaded " + demoData.size() + " senosria records from CSV.");
    }
    
}
