package sk.upjs.siete.cviko3;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Scanner;

public class Client {

	public static final String INFO_REQUEST = "INFO?";
	public static final int FILE_RECEIVE_PORT = 12000;
	public static final int TIMEOUT = 300;
	public static final int MAX_INTERVALS = 74;
	public static final String SERVER_ADDRESS = "localhost";

	public static void main(String[] args) {
		try (DatagramSocket infoSocket = new DatagramSocket()) {
		//infoServer
			//sprava ktoru chcem poslat + port
			byte[] requestData = INFO_REQUEST.getBytes();
			DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length,
					InetAddress.getByName(SERVER_ADDRESS), InfoServer2.INFO_SERVER_PORT);
			// cakam na info o subore zo servera
			infoSocket.send(requestPacket);

			byte[] packetData = new byte[infoSocket.getReceiveBufferSize()];
			DatagramPacket infoPacket = new DatagramPacket(packetData, packetData.length);
			infoSocket.receive(infoPacket); // prijimam info zo server
			//ukladam info od servera
			byte[] infoData = infoPacket.getData();
			String info = new String(infoData).trim();
			// citam informacie zo servera
			Scanner sc = new Scanner(info);
			// informacie ktore mi prisli
			String fileName = sc.nextLine();
			long fileSize = sc.nextLong();
			System.out.println("Subor: " + fileName + "velkost: " + fileSize + " bajtov");
			sc.close();
			//novy subor na zaklade toho co mi prislo
			File fileToSave = new File(fileName);
			RandomAccessFile raf= new RandomAccessFile(fileToSave, "v");
			raf.setLength(fileSize);

			//vytvorim interval
			Interval intervalsIHave = Interval.empty(0, fileSize);
			DatagramSocket dataSocket = new DatagramSocket(FILE_RECEIVE_PORT);

			while(true){
				dataSocket.setSoTimeout(TIMEOUT);
				try {
					packetData = new byte[dataSocket.getReceiveBufferSize()];
					DatagramPacket dataPacket = new DatagramPacket(packetData, packetData.length);
					dataSocket.receive(dataPacket);
					byte[] data = dataPacket.getData(); // ulozim prijate data
					ByteArrayInputStream bais = new ByteArrayInputStream(data);
					ObjectInputStream oos = new ObjectInputStream(bais);
					//informacie o intervale
					long min = oos.readLong();
					long max = oos.readLong();
					if(intervalsIHave.isMissing(max)){
						// zaznamenam si co mi prislo
						intervalsIHave.addFullSubinterval(min, max);
						int dataSize = (int) (max - min) + 1;
						byte[] dataInfo = new byte[dataSize];
						//precitam
						oos.read(dataInfo);
						//zapisem
						raf.write(dataInfo);
					}
					// ukoncenie spojenia po spracovani vsetkych intervalov
					if (intervalsIHave.getEmptySubintervals(MAX_INTERVALS).isEmpty()) {
						raf.close();
						infoSocket.close();
						System.out.println("ukoncujem spojenie");
						break;
					}
				}catch(SocketTimeoutException ste){
					List<Interval> subintervals = intervalsIHave.getEmptySubintervals(MAX_INTERVALS);
					ByteArrayOutputStream baos = new ByteArrayOutputStream(subintervals.size()*16+4);
					ObjectOutputStream oos = new ObjectOutputStream(baos);
					oos.writeInt(subintervals.size());
					for(Interval interval : subintervals){
						oos.writeLong(interval.getMin());
						oos.writeLong(interval.getMax());
					}
					oos.flush();
					byte[] requestIntervalsData = baos.toByteArray();
					DatagramPacket requestIntervalsPacket = new DatagramPacket(requestIntervalsData, requestIntervalsData.length,
							InetAddress.getByName(SERVER_ADDRESS), InfoServer2.MISSING_INTERVALS_PORT);
					infoSocket.send(requestIntervalsPacket);
				}
			}


		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {

			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
