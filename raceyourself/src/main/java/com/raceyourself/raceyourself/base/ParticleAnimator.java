package com.raceyourself.raceyourself.base;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.Choreographer;
import android.view.View;
import android.widget.RelativeLayout;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class ParticleAnimator implements Choreographer.FrameCallback {
    private Choreographer choreographer;

    private final List<Particle> particles;

    private final Vector2D target;

    private float acceleration;
    private long accelerationDelay;

    @Setter
    private ParticleListener particleListener;

    private Long startTimeNanos = null;
    private Long previousTimeNanos = null;
    private boolean running = false;

    public ParticleAnimator(List<Particle> particles, Vector2D target, float acceleration, long accelerationDelay) {
        this.particles = particles;
        this.target = target;
        this.acceleration = acceleration;
        this.accelerationDelay = accelerationDelay;
    }

    public void start() {
        if (running) return;
        running = true;
        startTimeNanos = null;
        previousTimeNanos = null;
        log.info("started with " + particles.size() + " particles");
        for (Particle particle : particles) particle.sync();
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

        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            final Particle particle = it.next();
            particle.update(delta);

            final Vector2D position = particle.getPosition();
            final Vector2D diff = new Vector2D(target.getX() - position.getX(), target.getY() - position.getY());
            final double mag = diff.getNorm();
            if (mag > 0 && relativeTime >= accelerationDelay) {
                final Vector2D attraction = diff.normalize().scalarMultiply(acceleration / Math.max(mag, 500));
                particle.add(attraction);
            }

            if (diff.getNorm() <= Math.max(25, particle.getVelocity().getNorm() * Math.max(delta, 0.001))) {
                it.remove();
                if (particleListener != null) particleListener.onTargetReached(particle, particles.size());
                log.info("particle removed, left: " + particles.size());
            }
        }
        previousTimeNanos = frameTimeNanos;

        if (!particles.isEmpty()) {
            choreographer.postFrameCallback(this);
        } else {
            running = false;
        }
    }

    @Data
    public static final class Particle {
        private final View view;
        private Vector2D position;
        private Vector2D velocity;

        public Particle(View view, Vector2D velocity) {
            this.view = view;
            this.velocity = velocity;
            sync();
        }

        public void sync() {
            this.position = new Vector2D(view.getX(), view.getY());
        }

        public void update(double delta) {
            Vector2D v = velocity.scalarMultiply(delta);
            position = position.add(v);
            velocity = velocity.scalarMultiply(0.95f);

            view.setX((float)position.getX());
            view.setY((float)position.getY());
        }

        public void add(Vector2D v) {
            this.velocity = this.velocity.add(v);
        }
    }

    public static interface ParticleListener {
        public void onTargetReached(Particle particle, int particlesAlive);
    }
}
