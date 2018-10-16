import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.*;

public class Sender {
	
	// File Data
	static byte[] fileData_s;
	static int receiver_port;
	static InetAddress receiver_host_ip;
	
	// Socket Constants
	static DatagramSocket senderSocket;
	static DatagramPacket incomingSegment;
	static Segment received;
	static Segment nextPacket;
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
	static int MWS;
	static int MSS;
	static int gamma;
	
	// Send Control Variables
	static boolean sendingData;
	static boolean receivingACKs;
	static int justACKd;
	static int multiACKcnt;
	static int leftEdgePointer;
	static int rightEdgePointer;
	static int nextSendPointer;
	static long timeoutTimer;

	public static void main(String[] args) throws Exception {
	
		// Check for correct number of arguments
		if (args.length != 14) {
			System.out.println("Incorrect number of arguments");
			return;
			}
		
		// Define input arguments
		String receiver_host_ipString = args[0];
		receiver_host_ip = InetAddress.getByName(receiver_host_ipString);

		receiver_port = Integer.parseInt(args[1]);
		String fileName_s = args[2];
		MWS = Integer.parseInt(args[3]);
		MSS = Integer.parseInt(args[4]);
		gamma = Integer.parseInt(args[5]);
		
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
		fileData_s = pdfToByteArray(fileName_s);
		
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
		
		// Initialize Data Variables
		numSegmentsTrans = 0;
		numTimeoutRetrans = 0;
		numFastRetrans = 0;
		numDupAcks = 0;
		
		// Initialize Connection with Receiver
		senderSocket = new DatagramSocket();
		startTime = System.currentTimeMillis();
		receivingACKs = true;
		sendingData = false;
		justACKd = 0;
		multiACKcnt = 0;
		leftEdgePointer = 0;
		rightEdgePointer = leftEdgePointer + MWS;
		nextSendPointer = 0;
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
		
		// Set initial time values
		estimatedRTT = 500;
		devRTT = 250;
		timeout = estimatedRTT + (gamma * devRTT);
		
		// Set initial sequence and ack numbers
		senderSequenceNumber = 0;
		senderACKNumber = 0;
		
		// Send first SYN
		nextPacket = new Segment(fileLengthB, senderSequenceNumber, senderACKNumber, false, true, false);
		sendSegment(nextPacket);
		
		// Receive first ACK
		received = receiveSegment();
		if (! (received.isSYN && received.isACK && (received.ACKNumber == senderSequenceNumber + 1))){
			throw new ConnectionException();
		}
		
		// Adjust Sequence and ACK Numbers
		senderSequenceNumber = received.ACKNumber;
		senderACKNumber = received.sequenceNumber + 1;
		
		// Send Final ACK of Connection Establishment
		nextPacket = new Segment(null, senderSequenceNumber, senderACKNumber, true, false, false);
		sendSegment(nextPacket);
		sendingData = true;
	}
	
	public static void terminateConnection() throws Exception{
		// Send first FIN
		nextPacket = new Segment(null, senderSequenceNumber, senderACKNumber, false, false, true);
		sendSegment(nextPacket);
		
		// Receive ACK
		received = receiveSegment();
		if (!(received.isACK && received.ACKNumber == senderSequenceNumber + 1)){
			throw new ConnectionException();
		}
		
		// Receive FIN
		received = receiveSegment();
		if (! (received.isFIN && (received.ACKNumber == senderSequenceNumber + 1))){
			throw new ConnectionException();
		}
		
		senderSequenceNumber = received.ACKNumber;
		senderACKNumber = received.sequenceNumber + 1;
		
		// Send final ACK
		nextPacket = new Segment(null, senderSequenceNumber, senderACKNumber, true, false, false);
		sendSegment(nextPacket);
	}

	public static void PLDmodule(Segment segmentToPLD){
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
		timeout = estimatedRTT + (gamma * devRTT);
	}
	
	public static void sendSegment(Segment sendingPacket) throws Exception{
		if (!(sendingPacket.isSYN || sendingPacket.isACK || sendingPacket.isFIN)){
			PLDmodule(sendingPacket);
		}
		if (segmentSentTimeHM.containsKey(sendingPacket.expectedACK)){
			segmentSentTimeHM.put(sendingPacket.expectedACK, (long)-1);
			sendingPacket.RXT = true;
		}
		sendingPacket.createDatagramPacket(receiver_host_ip, receiver_port);
		logSegment(sendingPacket);
		if (!sendingPacket.drop){
			senderSocket.send(sendingPacket.segment);
		}
		numSegmentsTrans++;
		if (! segmentSentTimeHM.containsKey(sendingPacket.expectedACK)){
			segmentSentTimeHM.put(sendingPacket.expectedACK, sendingPacket.packetTime);
		}
		if ((sendingPacket.sequenceNumber - 1) == leftEdgePointer && sendingData == true){
			timeoutTimer = System.currentTimeMillis();
		}
	}
	
	public static Segment receiveSegment() throws Exception{
		incomingSegment = new DatagramPacket(new byte[1024], 1024);
		senderSocket.receive(incomingSegment);
		recvBuffer = incomingSegment.getData();
		
		// Process Segment
		received = new Segment(recvBuffer);
		// Add if it is a duplicate ACK
		if (justACKd == received.ACKNumber){
			received.DA = true;
			multiACKcnt++;
			numDupAcks++;
		}
		else {
			justACKd = received.ACKNumber;
			multiACKcnt = 0;
		}
		logSegment(received);
		if ((segmentSentTimeHM.get(received.ACKNumber) != -1) && (received.DA != true)){
			adjustRTT(received.packetTime - segmentSentTimeHM.get(received.ACKNumber));
		}
		return received;
	}
	
	class SendThread extends Thread{
		
		//TODO look at sequence numbers and make sure they will be correct
		// Pointer numbers are 1 less than sequence numbers and acks
		
		public void run(){
			while (sendingData == true){
				while (((nextSendPointer + MSS - 1) < rightEdgePointer) && ((nextSendPointer + MSS - 1) < fileLength)){
					byte[] nextDataSegment = new byte[MSS];
					System.arraycopy(fileData_s, nextSendPointer, nextDataSegment, 0, MSS);
					try {
						nextPacket = new Segment(nextDataSegment, nextSendPointer + 1, senderACKNumber, false, false, false);
						sendSegment(nextPacket);
						nextSendPointer = nextSendPointer + nextDataSegment.length;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if ((fileLength - nextSendPointer) < MSS && (nextSendPointer < fileLength));
					byte[] lastDataSegment = new byte[fileLength - nextSendPointer];
					System.arraycopy(fileData_s, nextSendPointer, lastDataSegment, 0, fileLength - nextSendPointer);
				try {
					nextPacket = new Segment(lastDataSegment, nextSendPointer + 1, senderACKNumber, false, false, false);
					sendSegment(nextPacket);
					nextSendPointer = nextSendPointer + lastDataSegment.length;
				} catch (Exception e) {
					e.printStackTrace();
				}
				if ((System.currentTimeMillis() - timeoutTimer) >= timeout){
					byte[] resendDataSegment;
					if ((fileLength - leftEdgePointer) < MSS){
						resendDataSegment = new byte[fileLength - leftEdgePointer];
					}
					else resendDataSegment = new byte[MSS];
					System.arraycopy(fileData_s, leftEdgePointer, resendDataSegment, 0, resendDataSegment.length);
					try {
						nextPacket = new Segment(resendDataSegment, leftEdgePointer + 1, senderACKNumber, false, false, false);
						numTimeoutRetrans++;
						sendSegment(nextPacket);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	class ReceiveThread extends Thread{
		
		public void run(){
			while (receivingACKs == true){
				// Receive first ACK
				try {
					received = receiveSegment();
					if (multiACKcnt == 0){
						leftEdgePointer = received.ACKNumber - 1;
						rightEdgePointer = leftEdgePointer + MWS;
					}
					else if (multiACKcnt == 3){
						byte[] resendDataSegment;
						if ((fileLength - leftEdgePointer) < MSS){
							resendDataSegment = new byte[fileLength - leftEdgePointer];
						}
						else resendDataSegment = new byte[MSS];
						System.arraycopy(fileData_s, leftEdgePointer, resendDataSegment, 0, resendDataSegment.length);
						nextPacket = new Segment(resendDataSegment, leftEdgePointer + 1, senderACKNumber, false, false, false);
						numFastRetrans++;
						sendSegment(nextPacket);
						
						multiACKcnt = 0;
					}
					if (received.ACKNumber - 1 == fileLength){
						receivingACKs = false;
						sendingData = false;
						senderSequenceNumber = received.ACKNumber;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}
		
	}
}

class ConnectionException extends Exception{

	private static final long serialVersionUID = 1L;

	public ConnectionException(){
		super("Connection Error");
	}
}
