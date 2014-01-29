package com.glassfitgames.glassfitplatform.sensors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.InputDevice.MotionRange;
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

    // Glass uses a different Gesture Detector to other devices
    private GestureDetector glassGestureDetector = null;
    private NormalGestureDetector normalGestureDetector = null;

    // current touch status, for polling from unity
    private float xPosition = 0;
    private float yPosition = 0;
    private long touchTime = 0;
    private int touchCount = 0;

    // Bluetooth
    private BluetoothAdapter bt = null;
    public static enum BluetoothState {
        UNDEFINED,
        SERVER,
        CLIENT
    };
    private BluetoothState btState = BluetoothState.UNDEFINED;
    private Thread btInitThread = null; // Server accept thread or client connect thread
    private final String BT_NAME = "Glassfit";
    private final String BT_UUID = "cdc0b1dc-335a-4179-8aec-1dcd7ad2d832";
    private ConcurrentLinkedQueue<BluetoothThread> btThreads = new ConcurrentLinkedQueue<BluetoothThread>();

    // Intent results
    private static int REQUEST_ENABLE_BT = 1;
    
    // accessors for polling from unity
    public float getTouchX() {
        return xPosition;
    }

    public float getTouchY() {
        return yPosition;
    }

    public long getTouchTime() {
        return touchTime;
    }
    
    public int getTouchCount() {
        return touchCount;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize a gesture detector
        // Glass uses a different Gesture Detector to other devices
        if (Helper.onGlass()) {
            glassGestureDetector = createGlassGestureDetector(this);
        } else {
            normalGestureDetector = new NormalGestureDetector();
        }

        Intent intent = getIntent();
        if (intent != null) {
            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                sendUnityMessage("OnActionIntent", intent.getData().toString());
            }
        }
        
        bt = BluetoothAdapter.getDefaultAdapter();
        if (bt != null) {
            if (!bt.isEnabled()) {
                Log.i("GestureHelper", "Bluetooth not enabled. Enabling..");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);                
            } else {
                bluetoothStartup();                
            }
        }
    }

    /**
     * Activity result handler.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_ENABLE_BT == requestCode && bt != null) {
            bluetoothStartup();
        }
    }
    
    /**
     * Start Bluetooth server
     */
    public void startBluetoothServer() {
        btState = BluetoothState.SERVER;
        Log.i("GestureHelper", "Starting Bluetooth server..");
        bluetoothStartup();
    }

    /**
     * Start Bluetooth client
     */
    public void startBluetoothClient() {
        btState = BluetoothState.CLIENT;
        Log.i("GestureHelper", "Starting Bluetooth client..");
        bluetoothStartup();
    }
    
    /**
     * Common (delayable) startup method for Bluetooth
     */
    public void bluetoothStartup() {
        if (bt == null || !bt.isEnabled()) return; // Delayed
        if (btInitThread != null) return; // Done
        Log.i("GestureHelper", "Bluetooth enabled: " + bt.isEnabled());
        if (BluetoothState.SERVER.equals(btState)) {
            btInitThread = new AcceptThread();
            btInitThread.start();
            Log.i("GestureHelper", "Bluetooth server started");            
        }
        if (BluetoothState.CLIENT.equals(btState)) {
            btInitThread = new ConnectThread();
            btInitThread.start();
            Log.i("GestureHelper", "Bluetooth client started");
        }
    }
    
    public void broadcast(String data) {
        broadcast(data.getBytes());
    }
    
    public void broadcast(byte[] data) {
        for (BluetoothThread btThread : btThreads) {
            btThread.send(data);
        }        
    }
    
    /**
     * Whenever the content changes, we need to tell the new view to keep the
     * screen on and make sure all child components listen for touch input
     */
    @Override
    public void onContentChanged() {
        // Log.v("GestureHelper", "Content changed");
        super.onContentChanged();
        View view = this.findViewById(android.R.id.content);
        view.setKeepScreenOn(true);
        recurseViews(view);
    }

    /**
     * Loop through all child views and tell them to listen for touch input
     * 
     * @param view
     */
    private void recurseViews(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); ++i) {
                View foundView = vg.getChildAt(i);
                if (foundView != null) {
                    foundView.setFocusableInTouchMode(true);
                    recurseViews(foundView);
                }
            }
        }
    }

    /**
     * Listen for (and respond to) all track-pad events. This enables navigation
     * on glass.
     */
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        // Log.v("GestureHelper", "Trackpad event detected");
        processEvent(event);
//        Log.d("GestureHelper", "Trackpad event:  x:" + xPosition + " y:" + yPosition + " downtime:"
//                + touchTime);
        // return true to prevent event passing onto parent
        return true;
    }

    /**
     * Listen for (and respond to) all touch-screen events. This enables
     * navigation on phones and tablets.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Log.v("GestureHelper", "Touchscreen event detected");
        processEvent(event);
//        Log.d("GestureHelper", "Touch-screen event:  x:" + xPosition + " y:" + yPosition
//                + " downtime:" + touchTime);
        // return true to prevent event passing onto parent
        return true;
    }
    
    /** 
     * Detect back button presses and send to unity.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            sendUnityMessage("BackButton","");
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Stores the event's (x,y) co-ordinates (so the game can poll them) and
     * checks for gestures (which are sent ot the game via a UnityMessage)
     * 
     * @param event
     *            the raw event from the input device
     * @return true if the event has been consumed
     */
    private boolean processEvent(MotionEvent event) {
        // record the co-ordinates
        if (event.getAction() == event.ACTION_CANCEL || event.getAction() == event.ACTION_UP) {
            // no touch, so set position and downtime to null
            touchCount -= event.getPointerCount();
        } else {
            // new touch or continuation of existing touch: update X, Y and
            // downtime
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                touchCount += event.getPointerCount();
            }
            // need to scale raw x/y values to the range 0-1
            float xCoord = event.getX();
            float yCoord = event.getY();

            MotionRange xRange = event.getDevice().getMotionRange(MotionEvent.AXIS_X,
                    event.getSource());
            MotionRange yRange = event.getDevice().getMotionRange(MotionEvent.AXIS_Y,
                    event.getSource());

            xPosition = (xCoord - xRange.getMin()) / (xRange.getMax() - xRange.getMin());
            yPosition = (yCoord - yRange.getMin()) / (yRange.getMax() - yRange.getMin());
            touchTime = event.getEventTime() - event.getDownTime();
        }

        // check for gestures (may send unity message)
        if (glassGestureDetector != null) {
            glassGestureDetector.onMotionEvent(event);
        } else if (normalGestureDetector != null) {
            normalGestureDetector.onMotionEvent(event);
        }

        // return true to prevent event passing onto parent
        return true;
    }

    /**
     * Helper method to send a message to unity
     * 
     * @param message
     *            to send
     */
    private void sendUnityMessage(String message, String detail) {
        try {
            UnityPlayer.UnitySendMessage("Scriptholder", message, detail);
            Log.d("GestureHelper", "Message '" + message + "' sent to Unity.");
        } catch (UnsatisfiedLinkError e) {
            Log.w("GestureHelper",
                    "Failed to send message '"
                            + message
                            + "' to unity, probably because Unity native libraries aren't available (e.g. you are not running this from Unity).");
        }
    }

    /**
     * Helper method to build a new Glass GestureDetector that sends messages to
     * unity when a touchpad gesture is recognised. Note - throws a runtime
     * exception on non-glass devices.
     * 
     * @param context
     * @return the newly created Gesture Detector
     */
    private GestureDetector createGlassGestureDetector(Context context) {

        GestureDetector gestureDetector = new GestureDetector(context);

        gestureDetector.setBaseListener(new GestureDetector.BaseListener() {

            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.TAP) {
                    sendUnityMessage("IsTap", "");
                    return true;
                } else if (gesture == Gesture.SWIPE_LEFT) {
                    sendUnityMessage("FlingLeft", "");
                    return true;
                } else if (gesture == Gesture.SWIPE_RIGHT) {
                    sendUnityMessage("FlingRight", "");
                    return true;
                } else if (gesture == Gesture.SWIPE_UP) {
                    sendUnityMessage("SwipeUp", "");
                    return true;
                } else if (gesture == Gesture.TWO_SWIPE_LEFT) {
                    sendUnityMessage("TwoSwipeLeft", "");
                    return true;
                } else if (gesture == Gesture.TWO_TAP) {
                    sendUnityMessage("TwoTap", "");
                    return true;
                } else if (gesture == Gesture.THREE_TAP) {
                    sendUnityMessage("ThreeTap", "");
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
    
    /**
     * Helper method to build a new Glass GestureDetector that sends messages to
     * unity when a touchpad gesture is recognised. Note - throws a runtime
     * exception on non-glass devices.
     * 
     * @param context
     * @return the newly created Gesture Detector
     */
    private class NormalGestureDetector {
        
        int lastDownPointerID = MotionEvent.INVALID_POINTER_ID;
        float lastDownX = 0;
        float lastDownY = 0;
        
        float swipeDistanceX = 0.0f;
        float swipeDistanceY = 0.0f;
        float swipeSpeedX = 0.0f;
        float swipeSpeedY = 0.0f;
        
        public void onMotionEvent(MotionEvent event) {
            
            if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP) {
                
                // TODO: check for event.ACTION_POINTER_UP to detect a release of a 2nd or subsequent finger
                
                // end of an event - trigger action!
                long pressTime = event.getEventTime() - event.getDownTime(); // in milliseconds
                int numberOfFingers = event.getPointerCount();
                
                // calculate gesture distance and speed in both X and Y directions
                if (event.getPointerId(0) == lastDownPointerID) {
                    // need to scale raw x/y values to the range 0-1
                    MotionRange xRange = event.getDevice().getMotionRange(MotionEvent.AXIS_X,
                            event.getSource());
                    MotionRange yRange = event.getDevice().getMotionRange(MotionEvent.AXIS_Y,
                            event.getSource());

                    swipeDistanceX = (event.getX() - lastDownX) / (xRange.getMax() - xRange.getMin()); // 0-1, 1 means full length of pad
                    swipeDistanceY = (event.getY() - lastDownY) / (yRange.getMax() - yRange.getMin()); // 0-1, 1 means full length of pad
                    swipeSpeedX = swipeDistanceX / pressTime * 1000; // pads per second
                    swipeSpeedY = swipeDistanceY / pressTime * 1000; // pads per second
                } else {
                    swipeDistanceX = 0;
                    swipeDistanceY = 0;
                    swipeSpeedX = 0;
                    swipeSpeedY = 0;
                }
                
                // send any interesting gestures to unity 
                if (Math.abs(swipeDistanceX) < 0.05f && Math.abs(swipeDistanceY) < 0.05f && pressTime < 400) {
                    // we have some kind of tap
                    if (numberOfFingers == 1) {
                        sendUnityMessage("IsTap", "");
                    } else if (numberOfFingers == 2) {
                        sendUnityMessage("TwoTap", "");
                    } else if (numberOfFingers == 3) {
                        sendUnityMessage("ThreeTap", "");
                    }
                } else if (Math.abs(swipeDistanceX) > 0.3f && Math.abs(swipeSpeedX) > 1.5f) {
                    // we have a horizontal swipe
                    if (swipeDistanceX < 0.0f) {
                        sendUnityMessage("FlingLeft", "");
                    } else {
                        sendUnityMessage("FlingRight", "");
                    }
                } else if (Math.abs(swipeDistanceY) > 0.3f && Math.abs(swipeSpeedY) > 1.5f) {
                    // we have a vertical swipe
                    if (swipeDistanceX < 0.0f) {
                        sendUnityMessage("SwipeUp", "");
                    } else {
                        sendUnityMessage("SwipeDown", "");
                    }
                }
                
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {

                if (event.getPointerCount() == 1) {
                    // first finger of new touch - store until it is released
                    lastDownPointerID = event.getPointerId(0);
                    lastDownX = event.getX();
                    lastDownY = event.getY();
                }
                
            }
        }

    }

    /**
     * Bluetooth server thread
     */
    private class AcceptThread extends Thread {
        private boolean done = false;
        private final BluetoothServerSocket mmServerSocket;
     
        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            Log.i("AcceptThread", "Creating server socket..");
            try {
                tmp = bt.listenUsingRfcommWithServiceRecord(BT_NAME, UUID.fromString(BT_UUID));
            } catch (IOException e) { 
                Log.e("AcceptThread", "Error creating server socket", e);                
            }
            mmServerSocket = tmp;
            Log.i("AcceptThread", "Created server socket");
        }
     
        public void run() {
            // Keep listening until exception occurs 
            while (!done) {
                try {
                    // Block until we get a socket, pass it to the bluetooth thread.
                    manageConnectedSocket(mmServerSocket.accept());
                } catch (IOException e) {
                    Log.e("AcceptThread", "Error accepting socket", e);
                    break;
                }
            }
        }
     
        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            done = true;
            try {
                Log.i("AcceptThread", "Closing server socket..");
                mmServerSocket.close();
            } catch (IOException e) { 
                Log.e("AcceptThread", "Error closing server socket", e);
            }
        }
    }
   
    /**
     * Bluetooth client thread
     */
    private class ConnectThread extends Thread {
        private boolean done = false;
        private Set<BluetoothDevice> connectedDevices = new HashSet<BluetoothDevice>();        
        private UUID uuid = UUID.fromString(BT_UUID);

        public ConnectThread() {
            Log.i("ConnectThread", "Creating client sockets..");    
        }
        
        public void run() {
            while (!done) {
                Set<BluetoothDevice> devices = new HashSet<BluetoothDevice>(bt.getBondedDevices());
                devices.removeAll(connectedDevices);
                for (BluetoothDevice device : devices) {
                    if (done) break;
                    try {
                        Log.i("ConnectThread", "Creating client socket for " + device.getName() + "/" + device.getAddress());            
                        BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
                        socket.connect();
                        manageConnectedSocket(socket);
                        connectedDevices.add(device);
                    } catch (IOException e) {
                        Log.e("ConnectThread", "Error creating client socket", e);
                    }
                }
                synchronized(this) {
                    try {
                        this.wait(5000);
                    } catch (InterruptedException e) {
                        Log.e("ConnectThread", "Interrupted while waiting", e);
                    }
                }
            }
        }
        
        public void reconnect(BluetoothDevice device) {
            connectedDevices.remove(device);
            synchronized(this) {
                this.notify();
            }
        }
     
        public void cancel() {
            done = true;
            synchronized(this) {
                this.notify();
            }
        }
    }
   
    public void manageConnectedSocket(BluetoothSocket socket) {
        BluetoothThread btThread = new BluetoothThread(socket);
        btThread.start();
        btThreads.add(btThread);
    }
    
    /** 
     * Common Bluetooth thread
     */
    private class BluetoothThread extends Thread {
        private boolean done = false;
        private final BluetoothSocket socket;
        private InputStream is = null;
        private OutputStream os = null;
        private ConcurrentLinkedQueue<byte[]> msgQueue = new ConcurrentLinkedQueue<byte[]>();

        public BluetoothThread(BluetoothSocket socket) {
            this.socket = socket;
            Log.i("BluetoothThread", "Connected to " + socket.getRemoteDevice().getName() + "/" + socket.getRemoteDevice().getAddress());
            Helper.message("OnBluetoothConnect", "Connected to " + socket.getRemoteDevice().getName());
        }
        
        public void send(byte[] data) {
            msgQueue.add(data);
            Log.i("BluetoothThread", "Queue size: " + msgQueue.size());
            synchronized(this) {
                this.notify();
            }
        }
        
        @Override
        public void run() {
            byte[] buffer = new byte[1024]; 
            int bufferOffset = 0;
            int packetLength = -1;
            ByteBuffer header = ByteBuffer.allocate(8);
            try {
                is = socket.getInputStream();
                os = socket.getOutputStream();
                while (!done) {
                    byte[] data = msgQueue.poll();
                    boolean busy = false;
                    /// Write queued packets
                    if (data != null) {
                        /// Packetize using a simple header
                        header.clear();
                        // Marker
                        header.putInt(0xd34db33f);
                        // Length
                        header.putInt(data.length);
                        header.flip();
                        os.write(header.array(), header.arrayOffset(), header.limit());
                        os.write(data);
                        Log.i("BluetoothThread", "Sent " + data.length + "B to "  + socket.getRemoteDevice().getName() + "/" + socket.getRemoteDevice().getAddress());
                        busy = true;
                    }
                    /// Read incoming packets
                    if (is.available() > 0) {
                        // New packet
                        if (packetLength < 0) {
                            /// Depacketize                           
                            // Verify marker
                            if (is.read() != 0xd3 || is.read() != 0x4d || is.read() != 0xb3 || is.read() != 0x3f) { 
                                Log.e("BluetoothThread", "Received invalid header from " + socket.getRemoteDevice().getName() + "/" + socket.getRemoteDevice().getAddress());
                                cancel(); 
                                if (btInitThread instanceof ConnectThread) {
                                    ((ConnectThread)btInitThread).reconnect(socket.getRemoteDevice());
                                }
                                return; 
                            }  
                            // 32-bit length
                            header.clear();
                            header.put((byte)is.read());
                            header.put((byte)is.read());
                            header.put((byte)is.read());
                            header.put((byte)is.read());
                            header.flip();
                            packetLength = header.getInt();
                            if (packetLength < 0) {
                                Log.e("BluetoothThread", "Received invalid packet length " + socket.getRemoteDevice().getName() + "/" + socket.getRemoteDevice().getAddress());
                                cancel(); 
                                if (btInitThread instanceof ConnectThread) {
                                    ((ConnectThread)btInitThread).reconnect(socket.getRemoteDevice());
                                }
                                return;                                 
                            }
                            if (packetLength > buffer.length) {
                                // Resize buffer
                                buffer = new byte[packetLength];
                            }
                            bufferOffset = 0;
                        }
                        // Packet payload
                        int read = is.read(buffer, bufferOffset, packetLength - bufferOffset);
                        if (read < 0) {
                            Log.e("BluetoothThread", "Received EOF from " + socket.getRemoteDevice().getName() + "/" + socket.getRemoteDevice().getAddress());
                            cancel();
                            if (btInitThread instanceof ConnectThread) {
                                ((ConnectThread)btInitThread).reconnect(socket.getRemoteDevice());
                            }
                            return;
                        }
                        bufferOffset += read;
                        Log.i("BluetoothThread", "Received " + read + "B of " + packetLength + "B packet, " + (packetLength - bufferOffset) + "B left, from " + socket.getRemoteDevice().getName() + "/" + socket.getRemoteDevice().getAddress() );
                        if (packetLength == bufferOffset) {
                            String message = new String(buffer, 0, packetLength);
                            Helper.message("OnBluetoothMessage", message);
                            packetLength = -1;
                        }
                        busy = true;
                    }
                    if (!busy) {
                        try {
                            synchronized(this) {
                                this.wait(100);
                            }
                        } catch (InterruptedException e) {
                            Log.e("BluetoothThread", "InterruptedException for " + socket.getRemoteDevice().getName() + "/" + socket.getRemoteDevice().getAddress(), e);
                        }
                    }
                }
            } catch (IOException e) {
                Log.e("BluetoothThread", "IOException for " + socket.getRemoteDevice().getName() + "/" + socket.getRemoteDevice().getAddress(), e);
                if (!done) {
                    if (btInitThread instanceof ConnectThread) {
                        ((ConnectThread)btInitThread).reconnect(socket.getRemoteDevice());
                    }                    
                }
            } finally {
                cancel();
            }
        }
        
        
        public void cancel() {
            Helper.message("OnBluetoothConnect", "Disconnected from " + socket.getRemoteDevice().getName());
            done = true;
            try {
                if (is != null) is.close();
                if (os != null) os.close();
                socket.close();
            } catch (IOException e) {
                Log.e("BluetoothThread", "IOException for " + socket.getRemoteDevice().getName() + "/" + socket.getRemoteDevice().getAddress(), e);
            }
        }
    }
    
}
