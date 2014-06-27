package com.raceyourself.platform.sensors;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class Quaternion {
    
    // public fields for compatibility with C# code through JNI
    private float w;
    private float x;
    private float y;
    private float z;
    
    public Quaternion(float w, float x, float y, float z) {
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    // yaw = z, pitch = x, roll = y
    public static final float EPSILON = 0.000000001f;
    public Quaternion(float yaw, float pitch, float roll) {
        // do the /2 once only
        float y2 = yaw/2.0f;
        float p2 = pitch/2.0f;
        float r2 = roll/2.0f;
        
        // code taken from wikipedia
        // http://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles#cite_note-nasa-rotation-1
        this.w = (float)(Math.cos(p2)*Math.cos(r2)*Math.cos(y2) + Math.sin(p2)*Math.sin(r2)*Math.sin(y2));
        this.x = (float)(Math.sin(p2)*Math.cos(r2)*Math.cos(y2) - Math.cos(p2)*Math.sin(r2)*Math.sin(y2));
        this.y = (float)(Math.cos(p2)*Math.sin(r2)*Math.cos(y2) + Math.sin(p2)*Math.cos(r2)*Math.sin(y2));
        this.z = (float)(Math.cos(p2)*Math.cos(r2)*Math.sin(y2) - Math.sin(p2)*Math.sin(r2)*Math.cos(y2));
        this.normalize();
    }
    
    public Quaternion (float[] rotationVector) {
        this.x = rotationVector[0];
        this.y = rotationVector[1];
        this.z = rotationVector[2];
        
        if (rotationVector.length > 3) {
            this.w = rotationVector[3];
        } else {
            float magnitude = 1.0f - rotationVector[0] * rotationVector[0] - rotationVector[1]
                            * rotationVector[1] - rotationVector[2] * rotationVector[2];
            w = magnitude <= 0.0f ? 0.0f : (float)Math.sqrt(magnitude);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Quaternion)) return false;
        Quaternion q = (Quaternion)o;
        if (q.getW() == w && q.getX() == x && q.getY() == y && q.getZ() == z) {
            return true;
        } else {
            return false;
        }
    }
    
    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public float getW() {
        return w;
    }

    public static Quaternion identity() {
        float w = 1.0f;
        float x = 0.0f;
        float y = 0.0f;
        float z = 0.0f;
        return new Quaternion(w, x, y, z);
    }
    
    public Quaternion multiply(Quaternion q) {
        float w = q.w*this.w - q.x*this.x - q.y*this.y - q.z*this.z;
        float x = q.w*this.x + q.x*this.w - q.y*this.z + q.z*this.y;
        float y = q.w*this.y + q.x*this.z + q.y*this.w - q.z*this.x;
        float z = q.w*this.z - q.x*this.y + q.y*this.x + q.z*this.w;
        Quaternion result = new Quaternion(w, x, y, z);
        return result.normalize();
    }
    
    public Quaternion inverse() {
        return new Quaternion(w, -x, -y, -z);        
    }
    
    public Quaternion normalize() {
        float magnitude = (float)Math.sqrt(w*w + x*x + y*y + z*z);
        w /= magnitude;
        x /= magnitude;
        y /= magnitude;
        z /= magnitude;
        return this;
    }
    
    public static Quaternion quaternionBetween(Vector3D v0, Vector3D v1) {
        
        v0.normalize();
        v1.normalize();
        float dot = (float)v0.dotProduct(v1);
        
        if (dot < -0.999999f) {
            Vector3D axis = v0.orthogonal();
            return new Quaternion(0.0f, (float)axis.getX(), (float)axis.getY(), (float)axis.getZ());
        } else if (dot > 0.999999f) {
            return Quaternion.identity();
        } else {
            Vector3D axis = v0.crossProduct(v1);
            return new Quaternion(1+dot, (float)axis.getX(), (float)axis.getY(), (float)axis.getZ()).normalize();
        }

    }
    
    public Vector3D rotateVector(Vector3D v) {
        
        // pure quaternion representation of the vector (http://en.wikipedia.org/wiki/Quaternion_rotation#Using_quaternion_rotations)
        double length = v.getNorm();
        Quaternion q = new Quaternion(0.0f, (float)v.getX(), (float)v.getY(), (float)v.getZ()).normalize();
        
        // rotate the vector (http://en.wikipedia.org/wiki/Quaternion_rotation#Using_quaternion_rotations)
        Quaternion result = this.multiply(q).multiply(this.inverse());
        
        // convert back to vector
        return new Vector3D(result.getX(), result.getY(), result.getZ()).scalarMultiply(length);
    }
    
    public Quaternion nlerp(Quaternion dest, float blend) {
         float dot = w*dest.w + x*dest.x + y*dest.y + z*dest.z;
         float blendI = 1.0f - blend;
         if(dot < 0.0f) {
             // use minus signs to go the short way round the circle
             return new Quaternion(
                 blendI*this.w - blend*dest.w,
                 blendI*this.x - blend*dest.x,
                 blendI*this.y - blend*dest.y,
                 blendI*this.z - blend*dest.z
                 ).normalize();
         } else {
             // use plus signs to go the short way round the circle
             return new Quaternion(
                 blendI*this.w + blend*dest.w,
                 blendI*this.x + blend*dest.x,
                 blendI*this.y + blend*dest.y,
                 blendI*this.z + blend*dest.z
                 ).normalize();
         }
    }
       
    // yaw = z, pitch = x, roll = y
    public float[] toYpr() {
        float pitch = (float)Math.atan2(2*(w*x + y*z), 1-2*(x*x + y*y));
        float roll = (float)Math.asin(2*(w*y - z*x));
        float yaw = (float)Math.atan2(2*(w*z + x*y), 1-2*(y*y + z*z));
        return new float[] {yaw, pitch, roll};  // yaw, pitch, roll
    }
    
    public float[] toYprLH() {
        float pitch = (float)Math.atan2(2*(w*-x + -y*z), 1-2*(-x*-x + -y*-y));
        float roll = (float)Math.asin(2*(w*-y - z*-x));
        float yaw = (float)Math.atan2(2*(w*z + -x*-y), 1-2*(-y*-y + z*z));
        return new float[] {yaw, pitch, roll};  // yaw, pitch, roll
    }    
    
    public Quaternion flipX() {
        return new Quaternion(w,-x,y,z);
    }
    
    public Quaternion flipY() {
        return new Quaternion(w,x,-y,z);
    }
    
    public Quaternion flipZ() {
        return new Quaternion(w,x,y,-z);
    }
    
    public Quaternion flipW() {
        return new Quaternion(-w,x,y,z);
    }    
    
    public Quaternion swapXY() {
        float t = x;
        x = y;
        y = t;
        return this;
    }    
    
}
