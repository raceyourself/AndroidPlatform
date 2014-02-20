package com.glassfitgames.glassfitplatform.networking;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GlassFitServerClient {
    public final static long KEEPALIVE_INTERVAL = 1000;
    private Ping ping = null;
    private final Random random = new Random();
    
    private final Socket socket;
    private ConcurrentLinkedQueue<Timestamped<byte[]>> msgQueue = new ConcurrentLinkedQueue<Timestamped<byte[]>>();
    
    private boolean running = true;
    
    public GlassFitServerClient(byte[] token, String host) throws UnknownHostException, IOException {
        this(token, host, 9090);
    }
    public GlassFitServerClient(byte[] token, String host, int port) throws UnknownHostException, IOException {
        System.out.println("** Connecting");
        socket = new Socket();
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        socket.setSoTimeout(15000); // 15s read timeout
        socket.setPerformancePreferences(1, 2, 0);
        socket.connect(new InetSocketAddress(host, port), 15000); // 15s connection timeout

        System.out.println("** Authenticating");
        ByteBuffer ob = PacketBuffer.allocateMessageBuffer(token.length);
        ob.put(token);
        ob.flip();
        socket.getOutputStream().write(ob.array(), ob.arrayOffset(), ob.limit());
        System.out.println("** Waiting for response");
        byte[] packet = PacketBuffer.read(socket.getInputStream());
        if (packet == null) {
            disconnect();
            shutdown();
            throw new IOException("Server did not respond with a valid packet");
        }
        handle(packet);
    }
    
    public void loop() throws IOException {
        try {
            ByteBuffer ob = ByteBuffer.allocate(4096);
            OutputStream os = socket.getOutputStream();
            long alive = System.currentTimeMillis();
            while (running) {
                long cycle = System.currentTimeMillis();
                boolean busy = false;
                Timestamped<byte[]> data = msgQueue.peek();
                if (data == null && ping == null && System.currentTimeMillis() - alive > KEEPALIVE_INTERVAL) {
                    ping();
                }
                ob.clear();
                while ((data = msgQueue.poll()) != null) {
                    PacketBuffer.write(ob, data.getValue());
                    System.out.println("** Sending " + data.getValue().length + " q-delay: " + (System.currentTimeMillis() - data.getTimestamp()) + "ms");
                }
                if (ob.position() > 0) {
                    ob.flip();
                    long start = System.currentTimeMillis();
                    os.write(ob.array(), ob.arrayOffset(), ob.limit());
                    os.flush();
                    System.out.println("** Sent " + ob.limit() + " w-delay: " + (System.currentTimeMillis() - start) + "ms");
                    busy = true;
                }
                try {
                    long start = System.currentTimeMillis();
                    byte[] packet = PacketBuffer.read(socket);
                    if (packet == null) {
                        disconnect();
                        shutdown();
                        throw new IOException("Server did not respond with a valid packet");
                    }
                    System.out.println("** Read " + packet.length + " r-delay: " + (System.currentTimeMillis() - start) + "ms");
                    handle(packet);            
                    busy = true;
                } catch (SocketTimeoutException e) {
                    // Nothing to read or read timeout (real read timeout will corrupt stream and cause a disconnect)
                }
                if (!busy) {
                    try {
                        long start = System.currentTimeMillis();
                        synchronized(this) {
                            this.wait(100);
                        }
                        if (System.currentTimeMillis() - start > 250) System.err.println("*** Overslept " + (System.currentTimeMillis() - start - 100) + "ms, possible thread contention");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    alive = System.currentTimeMillis();
                }
                cycle = System.currentTimeMillis() - cycle;
                if (cycle > 500) System.err.println("Cycle took " + cycle + "ms");
            }
        } catch (Throwable t) {
            t.printStackTrace();
            running = false;
        }
        if (!socket.isClosed()) disconnect();
    }
    
    private void handle(byte[] data) {
        System.out.println("** Received " + data.length);
        if (data.length == 0) {
            onPing();
            return;
        }                
        ByteBuffer packet = ByteBuffer.wrap(data);                
        byte command = packet.get();
        
        switch (command) {
        case (byte)0x00: {
            // PING->PONG
            int pingId = packet.getInt();
            ByteBuffer message = ByteBuffer.allocate(1 + 4);
            message.put((byte)0xff);
            message.putInt(pingId);
            send(message.array());
            break;
        }
        case (byte)0xff: {
            // PONG
            int pingId = packet.getInt();
            if (ping == null || ping.getId() != pingId) {
                System.err.println("Invalid pong from server");
                break;
            }
            System.out.println(ping.Rtt() + "ms round-trip time to server");
            ping = null;
            break;
        }
        case (byte)0x01: {
            // User->user message
            int fromUid = packet.getInt();
            onUserMessage(fromUid, packet);
            break;
        }
        case (byte)0x02: {
            // User->group message
            int fromUid = packet.getInt();
            int fromGid = packet.getInt();
            onGroupMessage(fromUid, fromGid, packet);
            break;
        }
        case (byte)0x10: {
            // Created a group
            int groupId = packet.getInt();
            onGroupCreated(groupId);
            break;
        }
        default: {
            System.err.println("Unknown command: " + String.format("%02x", command) + " from server");
            disconnect();
            shutdown();
            return;
        }
        }
    }
    
    protected void onUserMessage(int fromUid, ByteBuffer data) {
        String message = new String(data.array(), data.position(), data.remaining());
        System.out.println("<" + fromUid + ">: " + message);        
    }
    
    protected void onGroupMessage(int fromUid, int fromGid, ByteBuffer data) {
        String message = new String(data.array(), data.position(), data.remaining());
        System.out.println("#" + fromGid + " <" + fromUid + ">: " + message);        
    }
    
    protected void onGroupCreated(int groupId) {
        System.out.println("* Created group " + groupId);        
    }
    
    protected void onPing() {        
    }
    
    protected void send(byte[] data) {
        msgQueue.add(new Timestamped<byte[]>(data));
        synchronized(this) {
            this.notify();
        }
    }
    
    public void createGroup() {
        ByteBuffer command = ByteBuffer.allocate(1);
        command.put((byte)0x10);
        send(command.array());
        System.out.println("* Creating group..");
    }
    
    public void joinGroup(int groupId) {
        ByteBuffer command = ByteBuffer.allocate(1 + 4);
        command.put((byte)0x11);
        command.putInt(groupId);
        send(command.array());
        System.out.println("* Joining group " + groupId);
    }
    
    public void leaveGroup(int groupId) {
        ByteBuffer command = ByteBuffer.allocate(1 + 4);
        command.put((byte)0x12);
        command.putInt(groupId);
        send(command.array());
        System.out.println("* Leaving group " + groupId);
    }
    
    public void messageUser(int userId, byte[] data) {
        ByteBuffer command = ByteBuffer.allocate(1 + 4 + data.length);
        command.put((byte)0x01);
        command.putInt(userId);
        command.put(data);
        send(command.array());
        System.out.println(userId + ", " + new String(data));
    }
    
    public void messageGroup(int groupId, byte[] data) {
        ByteBuffer command = ByteBuffer.allocate(1 + 4 + data.length);
        command.put((byte)0x02);
        command.putInt(groupId);
        command.put(data);
        send(command.array());        
        System.out.println("#" + groupId + ", " + new String(data));
    }
    
    public void ping() {
        ping = new Ping(random.nextInt());
        ByteBuffer message = ByteBuffer.allocate(1 + 4);
        message.put((byte)0x00);
        message.putInt(ping.getId());
        send(message.array());        
    }
    
    public void shutdown() {
        running = false;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public void disconnect() {
        try {
            socket.close();
        } catch (Exception e) {}
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("USAGE: client <access token>");
            System.exit(1);
        }
        System.out.println("Starting client");
        final GlassFitServerClient client = new GlassFitServerClient(args[0].getBytes(), "socket.raceyourself.com");
        System.out.println("Started client");
        Thread looper = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    client.loop();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
        });
        looper.start();
        System.out.println("Started looper");
        
        try {
            System.out.println("Sleeping");
//            client.createGroup();
//            System.out.println("Created group");
//            client.joinGroup(1);
//            System.out.println("Joined group");
//            client.messageGroup(1, "boo!".getBytes());
//            System.out.println("Messaged group");
//            client.leaveGroup(1);
//            System.out.println("left group");
            Thread.sleep(90000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }    
    
    private static class Timestamped<T> {
        private final long timestamp;
        private final T value;
        
        public Timestamped(T value) {
            timestamp = System.currentTimeMillis();
            this.value = value;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        public T getValue() {
            return value;
        }
    }
}
