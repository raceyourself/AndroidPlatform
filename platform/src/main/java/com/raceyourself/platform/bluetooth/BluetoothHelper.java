package com.raceyourself.platform.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by benlister on 14/07/2014.
 */
@Slf4j
public class BluetoothHelper {

    private Context context;

    // BluetoothHelper
    private final static boolean REQUEST_BT = false;
    private BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter(); ;
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

    // Listeners
    private List<BluetoothListener> listeners = new ArrayList<BluetoothListener>();

    public BluetoothHelper() {
        // nothing, for now
    }

    public void registerListener(BluetoothListener bl) {
        listeners.add(bl);
    }

    public void unregisterListener(BluetoothListener bl) {
        listeners.remove(bl);
    }

    /**
     * Start BluetoothHelper server
     */
    public void startBluetoothServer() {
        btState = BluetoothState.SERVER;
        log.info("Starting BluetoothHelper server..");
        bluetoothStartup();
    }

    /**
     * Start BluetoothHelper client
     */
    public void startBluetoothClient() {
        btState = BluetoothState.CLIENT;
        log.info("Starting BluetoothHelper client..");
        bluetoothStartup();
    }

    /**
     * Common (delayable) startup method for BluetoothHelper
     */
    private void bluetoothStartup() {
        if (bt == null) {
            log.error("Can't start bluetooth - BluetoothAdaptor is null");
            return;
        }
        if (!bt.isEnabled()) {
            log.warn("Can't start bluetooth - it's disabled");
            return;
        }

        //if (bt.getName().contains("Display")) Helper.setRemoteDisplay(true); // Act as a remote display
        if (btInitThread != null) return; // Done
        log.info("BluetoothHelper enabled: " + bt.isEnabled());
        if (BluetoothState.SERVER.equals(btState)) {
            btInitThread = new AcceptThread();
            btInitThread.start();
            log.info("BluetoothHelper server started");
        }
        if (BluetoothState.CLIENT.equals(btState)) {
            btInitThread = new ConnectThread();
            btInitThread.start();
            log.info("BluetoothHelper client started");
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

    public String[] getBluetoothPeers() {
        List<String> peers = new LinkedList<String>();
        for (BluetoothThread thread : btThreads) {
            thread.keepalive();
        }
        for (BluetoothThread thread : btThreads) {
            peers.add(thread.getDevice().getName());
        }
        return peers.toArray(new String[peers.size()]);
    }

    /**
     * BluetoothHelper server thread
     */
    private class AcceptThread extends Thread {
        private boolean done = false;
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            log.info("Creating server socket..");
            try {
                tmp = bt.listenUsingRfcommWithServiceRecord(BT_NAME, UUID.fromString(BT_UUID));
            } catch (IOException e) {
                log.error("Error creating server socket", e);
            }
            mmServerSocket = tmp;
            log.info("Created server socket");
        }

        public void run() {
            // Keep listening until exception occurs
            while (!done) {
                try {
                    // Block until we get a socket, pass it to the bluetooth thread.
                    manageConnectedSocket(mmServerSocket.accept());
                } catch (IOException e) {
                    log.error("Error accepting socket", e);
                    break;
                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            done = true;
            try {
                log.info("Closing server socket..");
                mmServerSocket.close();
            } catch (IOException e) {
                log.error("Error closing server socket", e);
            }
        }
    }

    /**
     * BluetoothHelper client thread
     */
    private class ConnectThread extends Thread {
        private boolean done = false;
        private Set<BluetoothDevice> connectedDevices = new HashSet<BluetoothDevice>();
        private UUID uuid = UUID.fromString(BT_UUID);

        public ConnectThread() {
            log.info("Creating client sockets..");
        }

        public void run() {
            while (!done) {
                Set<BluetoothDevice> devices = new HashSet<BluetoothDevice>(bt.getBondedDevices());
                devices.removeAll(connectedDevices);
                for (BluetoothDevice device : devices) {
                    if (done) break;
                    try {
                        log.info("Attempting to open client socket to server " + device.getName() + "/" + device.getAddress());
                        BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
                        socket.connect();
                        manageConnectedSocket(socket);
                        connectedDevices.add(device);
                    } catch (IOException e) {
                        log.info("Error opening client socket - is RaceYourself running on the remote device (as a server)? Will retry in 5s");
                    }
                }
                synchronized(this) {
                    try {
                        this.wait(5000);
                    } catch (InterruptedException e) {
                        log.error("Interrupted while waiting", e);
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
     * Common BluetoothHelper thread
     */
    private class BluetoothThread extends Thread {
        private boolean done = false;
        private final BluetoothSocket socket;
        private InputStream is = null;
        private OutputStream os = null;
        private ConcurrentLinkedQueue<byte[]> msgQueue = new ConcurrentLinkedQueue<byte[]>();
        private long alive = 0L;
        private long keepalive = 0L;

        public BluetoothThread(BluetoothSocket socket) {
            this.socket = socket;
            log.info("Connected to " + socket.getRemoteDevice().getName() + "/" + socket.getRemoteDevice().getAddress());
            for (BluetoothListener bl : listeners) {
                bl.onConnected(socket.getRemoteDevice());
            }
        }

        public void send(byte[] data) {
            msgQueue.add(data);
            log.info("Queue size: " + msgQueue.size());
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
            header.order(ByteOrder.BIG_ENDIAN); // Network byte order
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
                        log.info("Sent " + data.length + "B to "  + socket.getRemoteDevice().getName() + "/" + socket.getRemoteDevice().getAddress());
                        busy = true;
                        alive = System.currentTimeMillis();
                    }
                    /// Read incoming packets
                    if (is.available() > 0) {
                        // New packet
                        if (packetLength < 0) {
                            /// Depacketize
                            // Verify marker
                            if (is.read() != 0xd3 || is.read() != 0x4d || is.read() != 0xb3 || is.read() != 0x3f) {
                                log.error("Received invalid header from " + socket.getRemoteDevice().getName() + "/" + socket.getRemoteDevice().getAddress());
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
                                log.error("Received invalid packet length " + socket.getRemoteDevice().getName() + "/" + socket.getRemoteDevice().getAddress());
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
                            log.error("Received EOF from " + socket.getRemoteDevice().getName() + "/" + socket.getRemoteDevice().getAddress());
                            cancel();
                            if (btInitThread instanceof ConnectThread) {
                                ((ConnectThread)btInitThread).reconnect(socket.getRemoteDevice());
                            }
                            return;
                        }
                        bufferOffset += read;
                        log.info("Received " + read + "B of " + packetLength + "B packet, " + (packetLength - bufferOffset) + "B left, from " + socket.getRemoteDevice().getName() + "/" + socket.getRemoteDevice().getAddress() );
                        if (packetLength == bufferOffset) {
                            if (packetLength > 0) {
                                String message = new String(buffer, 0, packetLength);
                                for (BluetoothListener bl : listeners) {
                                    bl.onMessageReceived(message);
                                }
                            }
                            packetLength = -1;
                        }
                        busy = true;
                        alive = System.currentTimeMillis();
                    }
                    if (!busy) {
                        try {
                            synchronized(this) {
                                this.wait(100);
                            }
                        } catch (InterruptedException e) {
                            log.error("InterruptedException for " + socket.getRemoteDevice().getName() + "/" + socket.getRemoteDevice().getAddress(), e);
                        }
                    }
                }
            } catch (IOException e) {
                log.error("IOException for " + socket.getRemoteDevice().getName() + "/" + socket.getRemoteDevice().getAddress(), e);
                if (!done) {
                    if (btInitThread instanceof ConnectThread) {
                        ((ConnectThread)btInitThread).reconnect(socket.getRemoteDevice());
                    }
                }
            } finally {
                cancel();
            }
            btThreads.remove(this);
        }

        public void keepalive() {
            // Send ping to check if connection is still up if unused
            if (System.currentTimeMillis() - alive > 5000 && System.currentTimeMillis() - keepalive > 5000) {
                keepalive = System.currentTimeMillis();
                if (msgQueue.isEmpty()) send(new byte[0]);
            }
        }

        public BluetoothDevice getDevice() {
            return socket.getRemoteDevice();
        }

        public void cancel() {
            for (BluetoothListener bl : listeners) {
                bl.onDisconnected(socket.getRemoteDevice());
            }
            done = true;
            try {
                if (is != null) is.close();
                if (os != null) os.close();
                socket.close();
            } catch (IOException e) {
                log.error("IOException for " + socket.getRemoteDevice().getName() + "/" + socket.getRemoteDevice().getAddress(), e);
            }
        }
    }
}
