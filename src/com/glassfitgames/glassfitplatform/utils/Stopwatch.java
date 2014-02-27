package com.glassfitgames.glassfitplatform.utils;

public class Stopwatch {
    private boolean running = false;
    private long elapsedMillis = 0;
    private long lastResumeMillis;

    public Stopwatch() {
    } 
    
    public void start() {
        if (running) {
            return;
        } else {
            running = true;
            lastResumeMillis = System.currentTimeMillis();
        }
    }
    
    public void stop() {
        if (!running) {
            return;
        } else {
            elapsedMillis = elapsedTimeMillis();
            running = false;
        }
    }
    
    public void reset() {
        elapsedMillis = 0;
        lastResumeMillis = System.currentTimeMillis();
    }

    // return time (in seconds) since this object was created
    public long elapsedTimeMillis() {
        if (running) {
            return elapsedMillis + (System.currentTimeMillis() - lastResumeMillis);
        } else {
            return elapsedMillis;
        }            
    } 
    
    // return time (in seconds) since this object was created
    public void setTo(long elapsedTimeInMillis) {
        elapsedMillis = elapsedTimeInMillis;
        lastResumeMillis = System.currentTimeMillis();         
    } 
}
