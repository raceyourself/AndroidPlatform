package com.raceyourself.raceyourself.base;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.Choreographer;
import android.view.View;
import android.widget.RelativeLayout;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class PhysicsAnimator implements Choreographer.FrameCallback {
    private Choreographer choreographer;

    private final View view;
    private Vector2D position;
    private Vector2D velocity;

    private final Vector2D target;

    private float acceleration;
    private long accelerationDelay;


    private Long startTimeNanos = null;
    private Long previousTimeNanos = null;
    private boolean running = false;

    public PhysicsAnimator(View view, Vector2D target, float acceleration, long accelerationDelay, Vector2D startVelocity) {
        this.view = view;
        this.target = target;
        this.acceleration = acceleration;
        this.accelerationDelay = accelerationDelay;
        this.velocity = startVelocity;
    }

    public void start() {
        if (running) return;
        running = true;
        startTimeNanos = null;
        previousTimeNanos = null;
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)view.getLayoutParams();
        this.position = new Vector2D(params.leftMargin, params.topMargin);
        choreographer = Choreographer.getInstance();
        choreographer.postFrameCallback(this);
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!running) return;
        if (startTimeNanos == null) startTimeNanos = frameTimeNanos;
        if (previousTimeNanos == null) previousTimeNanos = frameTimeNanos;
        long relativeTime = (frameTimeNanos - startTimeNanos)/1000/1000;
        double delta = (frameTimeNanos-previousTimeNanos)/1000.0/1000.0/1000.0;

        Vector2D v = velocity.scalarMultiply(delta);
        position = position.add(v);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)view.getLayoutParams();
        params.leftMargin = (int)position.getX();
        params.topMargin = (int)position.getY();
        view.setLayoutParams(params);

        Vector2D diff = new Vector2D(target.getX() - params.leftMargin, target.getY() - params.topMargin);
        double mag = diff.getNorm();
        if (mag > 0 && relativeTime >= accelerationDelay) {
            Vector2D attraction = diff.normalize().scalarMultiply(acceleration/Math.max(mag, 600));
            velocity = velocity.add(attraction);
        }
        velocity = velocity.scalarMultiply(0.97f);

        previousTimeNanos = frameTimeNanos;

        if (diff.getNorm() > velocity.getNorm() * Math.max(delta, 0.001)) {
            choreographer.postFrameCallback(this);
        } else {
            running = false;
        }
    }
}
