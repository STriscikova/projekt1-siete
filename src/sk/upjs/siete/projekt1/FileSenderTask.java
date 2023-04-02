package sk.upjs.siete.cviko3;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class FileSenderTask implements Runnable {

    public static final int CHUNK_SIZE = 1000; //up to 1400
    public static final String BROADCAST_ADDRESS = "IP";
    private Interval intervalsToSend;
    public File file;

    public FileSenderTask(Interval intervalsToSend, File file) {
        this.intervalsToSend = intervalsToSend;
        this.file = file;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket();
             RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            while(true){
                Interval interval = intervalsToSend.getAndEraseNextFullSubintervalBlocked(CHUNK_SIZE);
                // naciatm subor z disku
                raf.seek(interval.getMin());
                byte[] filePart = new byte[CHUNK_SIZE];
                int actuallyRead = raf.read(filePart);
                // pole B podla dat
                ByteArrayOutputStream baos = new ByteArrayOutputStream(16 + filePart.length);
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                //informacie o intervale
                oos.writeLong(interval.getMin());
                oos.writeLong(interval.getMax());
                //data suboru
                oos.write(filePart);
                //zvysok ide prec
                oos.flush();
                // ulozim info s baos
                byte[] data = baos.toByteArray();
                DatagramPacket dataPacket = new DatagramPacket(data, data.length, InetAddress.getByName(BROADCAST_ADDRESS),
                        Client.FILE_RECEIVE_PORT);
                // posleme packet
                socket.send(dataPacket);
            }

        }catch(SocketException e){
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }
}
