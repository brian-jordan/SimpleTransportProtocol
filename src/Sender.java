import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.*;

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
	
	// Set of Segments
	static HashMap<Integer, Long> segmentSentTimeHM;
	
	// Begin Time
	static long startTime;
	
	// Timeout Variables
	static long timeout;
	static long estimatedRTT;
	static long devRTT;
	
	// Control Variables
	static int expectedACK;

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
		
		// Initialize HashMap for segment send times
		segmentSentTimeHM = new HashMap<>();
		
		// Find length of file to send during initialization
		fileLength = fileData_s.length;
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(fileLength);
		byte[] fileLengthBytes = bb.array();
		
		// Initialize Sender Log File
		senderLog = new PrintWriter("Sender_log.txt");
		senderLog.println("<event> <time> <type-of-packet> <seq-number> <number-of-bytes-data> <ack-number>");
		
		// Initialize Connection with Receiver
		senderSocket = new DatagramSocket();
		startTime = System.currentTimeMillis();
		establishConnection(receiver_host_ip, receiver_port, fileLengthBytes);
		
		// TODO
		// Initialize Receiver Thread
		// Maintains numTimeoutRetrans, numFastRetrans, numDupAcks
		// Process Packet
		// Log
		// Update Sender window left edge
		// Update RTT
		
		// TODO
		// Initialize Sender Thread
		// Maintains numSegmentsTrans
		// Create Packet
		// Pass packets through PLD Module
		// Set Event Type
		// Create datagram
		// Log
		// Send
		// Maintain Segment Sent Time HashMap make value -1 if retransmitted 
		
		// TODO
		// Implement delayed sending thread
		// create datagram
		// Log
		// Send
		// Maintain Segment Sent Time HashMap make value -1 if retransmitted 
		
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
		// Maintains numSegmentsTrans
		// Create Packet
		// Pass packets through PLD Module
		// Set Event Type
		// Create datagram
		// Log
		// Send
		// Maintain Segment Sent Time HashMap make value -1 if retransmitted
		
		// Set initial time values
		estimatedRTT = 500;
		devRTT = 250;
		timeout = estimatedRTT + (4 * devRTT);
		
		// Set initial value of numSegmentsTrans
		numSegmentsTrans = 0;
		
		// Set initial sequence and ack numbers
		senderSequenceNumber = 0;
		senderACKNumber = 0;
		expectedACK = senderSequenceNumber + 1;
		
		// create 
		
		// Send first SYN
		Segment syn1 = new Segment(fileLengthB, senderSequenceNumber, senderACKNumber, false, true, false);
		syn1.createDatagramPacket(receiverIP, receiverPort);
		logSegment(syn1);
		senderSocket.send(syn1.segment);
		segmentSentTimeHM.put(expectedACK, syn1.packetTime);
		numSegmentsTrans++;
		
		// Receive first ACK
		incomingSegment = new DatagramPacket(new byte[1024], 1024);
		senderSocket.receive(incomingSegment);
		recvBuffer = incomingSegment.getData();
		
		// Process Segment
		received = new Segment(recvBuffer);
		if (! (received.isSYN && received.isACK && (received.ACKNumber == expectedACK))){
			throw new ConnectionException();
		}
		logSegment(received);
		if (segmentSentTimeHM.get(received.ACKNumber) != -1){
			adjustRTT(received.packetTime - segmentSentTimeHM.get(received.ACKNumber));
		}
		
		// Adjust Sequence and ACK Numbers
		senderSequenceNumber = received.ACKNumber;
		senderACKNumber = received.sequenceNumber + 1;
		expectedACK = senderSequenceNumber + 1;
		
		// Send Final ACK of Connection Establishment
		Segment ack = new Segment(null, senderSequenceNumber, senderACKNumber, true, false, false);
		ack.createDatagramPacket(receiverIP, receiverPort);
		logSegment(ack);
		senderSocket.send(ack.segment);
		segmentSentTimeHM.put(expectedACK, ack.packetTime);
		numSegmentsTrans++;
	}

	public void PLDmodule(Segment segmentToPLD){
		// Maintains numSegmentsHandledPLD, numSegmentsDroppedPLD, numSegmentsCorruptedPLD, numSegmentsReOrderedPLD,
		// numSegmentsDuplicated, numSegmentsDelayedPLD
		// Sets Event variables
		
	}
	
	// Logs segment information
	public static void logSegment(Segment segmentToLog){
		// Call Set Event
		segmentToLog.setEvent();
		segmentToLog.setTypeOfPacket();
		segmentToLog.setTime();
		senderLog.printf("%s     %f     %s     %d     %d     %d\n", segmentToLog.event, (double)((segmentToLog.packetTime - startTime) / 1000), segmentToLog.typeOfPacket, segmentToLog.sequenceNumber, segmentToLog.payloadLength, segmentToLog.ACKNumber);
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
	
	// Maintain RTT value
	public static void adjustRTT(long sampleRTT){
		estimatedRTT = (long)((0.875 * estimatedRTT) + (0.125 * sampleRTT));
		devRTT = (long)((0.75 * devRTT) + (0.25 * Math.abs(sampleRTT - estimatedRTT)));
		timeout = estimatedRTT + (4 * devRTT);
	}

}

class ConnectionException extends Exception{

	private static final long serialVersionUID = 1L;

	public ConnectionException(){
		super("Connection Error");
	}
}
