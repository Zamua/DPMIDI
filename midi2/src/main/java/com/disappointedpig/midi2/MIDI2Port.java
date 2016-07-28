package com.disappointedpig.midi2;

import android.util.Log;

import com.disappointedpig.midi2.events.MIDI2PacketEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;

public class MIDI2Port implements Runnable {
    public final DatagramSocket socket;
    public int port;
    private static final int BUFFER_SIZE = 1536;
    private boolean listening;

    protected MIDI2Port() {
        DatagramSocket socket1;
        try {
            DatagramChannel channel = DatagramChannel.open();
            socket1 = channel.socket();
//            socket1.setBroadcast(true);
            socket1.setReuseAddress(true);
        } catch (IOException e) {
            socket1 = null;
            e.printStackTrace();
        }

        this.socket = socket1;
        this.port = 0;
    }

    protected DatagramSocket getSocket() { return socket; }

    protected int getPort() {
        return port;
    }

    public void close() {
        socket.close();
    }

    public Boolean bind(int port) {
        this.port = port;
        try {
//            Log.d("inport","socket bound? "+(socket.isBound() ? "YES" : "NO"));
//            Log.d("inport","socket closed? "+(socket.isClosed() ? "YES" : "NO"));
            InetSocketAddress a = new InetSocketAddress(port);
//            Log.d("inport","about to bind to socket - "+a.toString());
            socket.bind(a);
//            Log.d("portIn","socket is bound "+(socket.isBound() ? "YES" : "NO"));


        } catch (SocketException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void start() {
        listening = true;
        final Thread thread = new Thread(this);
        // The JVM exits when the only threads running are all daemon threads.
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        listening = false;
    }

    @Override
    public void run() {
        final byte[] buffer = new byte[BUFFER_SIZE];
        final DatagramPacket packet = new DatagramPacket(buffer, BUFFER_SIZE);
        final DatagramSocket socket = getSocket();
        while (listening) {
            try {
                try {
                    socket.receive(packet);
                } catch (SocketException ex) {
                    if (listening) {
                        throw ex;
                    } else {
                        // if we closed the socket while receiving data,
                        // the exception is expected/normal, so we hide it
                        continue;
                    }
                }



                Log.d("MIDI2Port","packet from "+packet.getAddress()+":"+packet.getPort());
                EventBus.getDefault().post(new MIDI2PacketEvent(packet));

//
//                String input = "";
//                for(byte a:packet.getData()) {
//                    input += (String.format("%02x", a) + " ");
//                }
//                Log.e("MIDIPort:in ",input);
//                long a = System.nanoTime();
//                final MIDIPacket midiPacket = converter.convert(packet);
//
//                long b = System.nanoTime();
//                Log.e("MIDIPort", "packet processing time: " + (b - a));

//                final MIDIPacket midiPacket = converter.convert(buffer, packet.getLength(), packet.getAddress(), packet.getPort());
//                dispatcher.dispatchPacket(midiPacket);
            } catch (IOException ex) {
                ex.printStackTrace(); // XXX This may not be a good idea, as this could easily lead to a never ending series of exceptions thrown (due to the non-exited while loop), and because the user of the lib may want to handle this case himself
            }
        }
    }
}