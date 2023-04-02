package sk.upjs.siete.cviko3;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class RequestServerTask implements Runnable {
    private Interval intervalsToSend;

    public RequestServerTask(Interval intervalsToSend) {
        // ulozim si interval
        this.intervalsToSend = intervalsToSend;
    }

    @Override
    public void run() {
        try(DatagramSocket socket = new DatagramSocket(InfoServer.MISSING_INTERVALS_PORT)) {
            while(true){
                byte [] packetData = new byte[socket.getReceiveBufferSize()];
                DatagramPacket requestPacket = new DatagramPacket(packetData, packetData.length);
                //cakam na packet
                socket.receive(requestPacket);
                // ulozim si co mi prislo
                byte[] data = requestPacket.getData();
                //citanie streamu
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                // ulozenie bais
                ObjectInputStream ois = new ObjectInputStream(bais);
                int count = ois.readInt();
                for (int i = 0; i < count; i++) {
                    //informacie o intervale
                    long min = ois.readLong();
                    long max = ois.readLong();
                    intervalsToSend.addFullSubinterval(min, max);

                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
