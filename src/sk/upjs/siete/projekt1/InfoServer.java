package sk.upjs.siete.cviko3;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InfoServer {
	public static final String FILE_NAME = "C:\\Users\\user\\Downloads\\Afja02_automaty_B.wmv";
	public static final int INFO_SERVER_PORT = 11000;
	public static final int MISSING_INTERVALS_PORT = 8765;

	public static void main(String[] args) {
		// subor ktory chcem posielat
		File file = new File(FILE_NAME);
		String fileName = file.getName();
		long fileSize = file.length();
		//vytvorim si interval
		Interval intervalsToSend = Interval.empty(0, fileSize);
		ExecutorService threadManager = Executors.newCachedThreadPool();
		// vytvorim RST
		RequestServerTask requestServerTask = new RequestServerTask(intervalsToSend);
		// vlakno
		threadManager.execute(requestServerTask);
		FileSenderTask fileSenderTask = new FileSenderTask(intervalsToSend, file);

		byte[] infoData = (fileName + "\n" + fileSize).getBytes();
		try (DatagramSocket infoSocket = new DatagramSocket(INFO_SERVER_PORT)) {

			while (true) {
				byte[] packetData = new byte[infoSocket.getReceiveBufferSize()];
				DatagramPacket requestPacket = new DatagramPacket(packetData, packetData.length);
				infoSocket.receive(requestPacket);
				byte[] requestData = requestPacket.getData();
				String request = new String(requestData).trim();
				if (Client.INFO_REQUEST.equals(request)) {
					DatagramPacket infoPacket = new DatagramPacket(infoData, infoData.length,
							requestPacket.getAddress(), requestPacket.getPort());
					infoSocket.send(infoPacket);
				}
			}
		} catch (IOException e) {
			// TODO: handle exceptionion
			e.printStackTrace();
		}

	}

}
