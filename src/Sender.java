import java.io.*;
import java.net.*;

public class Sender {
	
	// Socket Constants
	static DatagramSocket senderSocket;
	static DatagramPacket incomingSegment;
	static Segment received;
	static byte[] recvBuffer;
	static int senderSequenceNumber;
	static int senderACKNumber;
	static PrintWriter senderLog;
	
	// PLD Constants
	static int numSegmentsHandledPLD;
	static int numSegmentsDroppedPLD;
	static int numSegmentsCorruptedPLD;
	static int numSegmentsReOrderedPLD;
	static int numSegmentsDuplicated;
	static int numSegmentsDelayedPLD;
	
	// Other Data for Log
	static int fileLength;
	static int numSegmentsTrans;
	static int numTimeoutRetrans;
	static int numFastRetrans;
	static int numDupAcks;
	
	

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
		fileLength = fileData_s.length;
		String fileLengthString = "" + fileLength;
		byte[] fileLengthBytes = fileLengthString.getBytes();
		
		// Initialize Sender Log File
		senderLog = new PrintWriter("Sender_log.txt");
		senderLog.println("<event> <time> <type-of-packet> <seq-number> <number-of-bytes-data> <ack-number>");
		
		// Initialize Connection with Receiver
		senderSocket = new DatagramSocket();
		establishConnection(receiver_host_ip, receiver_port, fileLengthBytes);
		
		// TODO
		// Initialize Receiver Thread
		
		// TODO
		// Initialize Sender Thread
		
		// TODO
		// Terminate Connection with Receiver
		
		// Print Log Statistics and Close Log
		logStats();
		senderLog.close();
		
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
	public static void establishConnection(InetAddress receiverIP, int receiverPort, byte[] fileLengthB) throws Exception{
		
		// Set initial sequence and ack numbers
		senderSequenceNumber = 0;
		senderACKNumber = 0;
		// Send first SYN
		Segment syn1 = new Segment(fileLengthB, senderSequenceNumber, senderACKNumber, false, true, false);
		syn1.createDatagramPacket(receiverIP, receiverPort);
		senderSocket.send(syn1.segment);
		
		// Receive first ACK
		incomingSegment = new DatagramPacket(new byte[1024], 1024);
		senderSocket.receive(incomingSegment);
		recvBuffer = incomingSegment.getData();
		// Process Segment
		received = new Segment(recvBuffer);
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
	
	// TODO
	// Logs segment information
	public static void logSegment(Segment segmentToLog){
		
	}
	
	// Print Log Statistics
	public static void logStats(){
		senderLog.printf("Size of the file: %d Bytes\n", fileLength);
		senderLog.printf("Segments transmitted: %d\n", numSegmentsTrans);
		senderLog.printf("Number of Segments handled by PLD: %d\n", numSegmentsHandledPLD);
		senderLog.printf("Number of Segments Dropped: %d\n", numSegmentsDroppedPLD);
		senderLog.printf("Number of Segments Corrupted: %d\n", numSegmentsCorruptedPLD);
		senderLog.printf("Number of Segments Re-ordered: %d\n", numSegmentsReOrderedPLD);
		senderLog.printf("Number of Segments Duplicated: %d\n", numSegmentsDuplicated);
		senderLog.printf("Number of Segments Delayed: %d\n", numSegmentsDelayedPLD);
		senderLog.printf("Number of Retransmissions due to timeout: %d\n", numTimeoutRetrans);
		senderLog.printf("Number of Fast Retransmissions: %d\n", numFastRetrans);
		senderLog.printf("Number of Duplicate Acknowledgements received: %d\n", numDupAcks);
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
