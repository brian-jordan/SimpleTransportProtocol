import java.io.*;
import java.net.*;

public class Sender {
	
	static DatagramSocket senderSocket;
	static DatagramPacket request;
	static byte[] recvBuffer;
	static int senderSequenceNumber;
	static int senderACKNumber;

	public static void main(String[] args) throws Exception {
	
		// Check for correct number of arguments
		if (args.length != 14) {
			System.out.println("Incorrect number of arguments");
			return;
			}
		
		// Define input arguments
		String receiver_host_ipString = args[0];
		InetAddress receiver_host_ip = InetAddress.getByName(receiver_host_ipString);

		int receiver_port = Integer.parseInt(args[1]);
		String fileName_s = args[2];
		int MWS = Integer.parseInt(args[3]);
		int MSS = Integer.parseInt(args[4]);
		int gamma = Integer.parseInt(args[5]);
		
		// Set Arguments used by PLD Module based on inputs
		double pDrop = Double.parseDouble(args[6]);
		double pDuplicate = Double.parseDouble(args[7]);
		double pCorrupt = Double.parseDouble(args[8]);
		double pOrder = Double.parseDouble(args[9]);
		int maxOrder = Integer.parseInt(args[10]);
		double pDelay = Double.parseDouble(args[11]);
		int maxDelay = Integer.parseInt(args[12]);
		int seed = Integer.parseInt(args[13]);
		
		// Convert file into byte array
		byte[] fileData_s = pdfToByteArray(fileName_s);
		
		// TODO
		// Initialize Connection with Receiver
		senderSocket = new DatagramSocket();
		establishConnection(receiver_host_ip, receiver_port);
		
		
		
		
		
		// TODO
		// If segment is not for connection or teardown send it through PLD module
		
		// Have a listener thread waiting for acks and a sender thread sending packets
	}
	
	// Convert file into byte array
	public static byte[] pdfToByteArray(String fName) throws Exception{
		File fSender = new File(fName);
		long fileLength_s = fSender.length();
		byte[] fileData = new byte[(int)fileLength_s];
		FileInputStream fileIS = new FileInputStream(fSender);
		fileIS.read(fileData);
		fileIS.close();
		return fileData;
	}
	
	// Establish connection with receiver
	public static void establishConnection(InetAddress receiverIP, int receiverPort) throws Exception{
		
		// Set initial sequence and ack numbers
		senderSequenceNumber = 0;
		senderACKNumber = 0;
		// Send first SYN
		Segment syn1 = new Segment(null, senderSequenceNumber, senderACKNumber, false, true, false);
		syn1.createDatagramPacket(receiverIP, receiverPort);
		senderSocket.send(syn1.segment);
		
		// Receive first ACK
		request = new DatagramPacket(new byte[1024], 1024);
		senderSocket.receive(request);
		recvBuffer = request.getData();
		// Process Segment
		Segment received = new Segment(recvBuffer);
		if (! (received.isSYN && received.isACK && (received.ACKNumber == senderSequenceNumber + 1))){
			throw new ConnectionException();
		}
		// Adjust Sequence and ACK Numbers
		senderSequenceNumber++;
		senderACKNumber = received.sequenceNumber + 1;
		
		// Send Final ACK of Connection Establishment
		Segment ack = new Segment(null, senderSequenceNumber, senderACKNumber, true, false, false);
		ack.createDatagramPacket(receiverIP, receiverPort);
		senderSocket.send(ack.segment);
	}

	public void PLDmodule(DatagramPacket segment){
		
	}

}

class ConnectionException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ConnectionException(){
		super("Connection Error");
	}
}
