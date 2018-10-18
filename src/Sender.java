import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.*;
import java.util.concurrent.*;

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
	
	static Random random;
	
	// Acting on PLD
	static Thread tamperedSenderThread;
	
	static Segment dupSegment;
	static Segment reorderingSegment;
	static Segment delayedSegment;
	
	static int reorderCount;
	static long delayPacketTime;
	static long delayTimer;
	
	static boolean delaying;
	static boolean reordering;
	static boolean multipleSegment;
	
	// PLD Variables
	static double pDrop;
	static double pDuplicate;
	static double pCorrupt;
	static double pOrder;
	static int maxOrder;
	static double pDelay;
	static int maxDelay;
	static int seed;
	
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
		pDrop = Double.parseDouble(args[6]);
		pDuplicate = Double.parseDouble(args[7]);
		pCorrupt = Double.parseDouble(args[8]);
		pOrder = Double.parseDouble(args[9]);
		maxOrder = Integer.parseInt(args[10]);
		pDelay = Double.parseDouble(args[11]);
		maxDelay = Integer.parseInt(args[12]);
		seed = Integer.parseInt(args[13]);
		
		System.out.println("Arguments Accepted Receiver");
		
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
		System.out.println("<event> <time> <type-of-packet> <seq-number> <number-of-bytes-data> <ack-number>");
		senderLog.println("<event> <time> <type-of-packet> <seq-number> <number-of-bytes-data> <ack-number>");
		
		System.out.println("sender log initialized");
		
		// Initialize Data Variables
		numSegmentsTrans = 0;
		numTimeoutRetrans = 0;
		numFastRetrans = 0;
		numDupAcks = 0;
		
		// Initialize Connection with Receiver
		senderSocket = new DatagramSocket();
		startTime = System.currentTimeMillis();
		receivingACKs = false;
		sendingData = false;
		justACKd = 0;
		multiACKcnt = 0;
		leftEdgePointer = 0;
		rightEdgePointer = leftEdgePointer + MWS;
		nextSendPointer = 0;
		establishConnection(receiver_host_ip, receiver_port, fileLengthBytes);
		
		// Constants for PLD
		numSegmentsHandledPLD = 0;
		numSegmentsDroppedPLD = 0;
		numSegmentsCorruptedPLD = 0;
		numSegmentsReOrderedPLD = 0;
		numSegmentsDuplicated = 0;
		numSegmentsDelayedPLD = 0;
		
		delaying = false;
		reordering = false;
		multipleSegment = false;
		
		random = new Random(seed);
		
		System.out.println("Connection Established sender");
		
		System.out.println("Sender file length: " + fileLength);
		
		// Initialize Receiver Thread
		Thread receiverThread = new Thread(){
			public void run(){
				System.out.println("Sender: ready to receive ACKs");
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
							System.out.println("Print Me !!!!");
							receivingACKs = false;
							sendingData = false;
							senderSequenceNumber = received.ACKNumber;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				System.out.println("Receiver Thread Stopped");
			}
		};
		
		tamperedSenderThread = new Thread(){
			public void run(){
				while (sendingData == true){
					if(multipleSegment == true){
						try {
							sendTamperedSegment(dupSegment);
							multipleSegment = false;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					if (reordering == true && reorderCount - 2 == maxOrder){
						try {
							sendTamperedSegment(reorderingSegment);
							reorderCount = 0;
							reordering = false;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					if (delaying == true && ((System.currentTimeMillis() - delayTimer) >= delayPacketTime)){
						try {
							sendTamperedSegment(delayedSegment);
							delaying = false;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					try {
						sleep(1);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
				System.out.println("Tamper Thread Stopped");
			}
		};
		// Maintains numTimeoutRetrans, numFastRetrans, numDupAcks
		// Process Packet
		// Log
		// Update Sender window left edge
		// Update RTT
		
		// Initialize Sender Thread
		Thread senderThread = new Thread(){
			public void run(){
				System.out.println("sender: Sending Data");
				while (sendingData == true){
					// System.out.println(nextSendPointer);
					// System.out.println(leftEdgePointer);
					while (((nextSendPointer + MSS - 1) < rightEdgePointer) && ((nextSendPointer + MSS - 1) < fileLength)){
						try {
							sleep(100);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
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
					if ((fileLength - nextSendPointer) < MSS && (nextSendPointer < fileLength)){
						byte[] lastDataSegment = new byte[fileLength - nextSendPointer];
						System.arraycopy(fileData_s, nextSendPointer, lastDataSegment, 0, fileLength - nextSendPointer);
						try {
							nextPacket = new Segment(lastDataSegment, nextSendPointer + 1, senderACKNumber, false, false, false);
							sendSegment(nextPacket);
							nextSendPointer = nextSendPointer + lastDataSegment.length;
						} catch (Exception e) {
							e.printStackTrace();
						}
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
				System.out.println("Sender Thread Stopped");
			}
		};
		
		ExecutorService executor = Executors.newFixedThreadPool(3);
		executor.execute(receiverThread);
		executor.execute(tamperedSenderThread);
		executor.execute(senderThread);
		
		executor.shutdown();
		while(!executor.isTerminated()){
			
		}
		
		// Terminate Connection with Receiver
		System.out.println("Connection is being terminated");
		terminateConnection();
		
		System.out.println("Connection terminated sender");

		
		// Print Log Statistics and Close Log
		logStats();
		senderLog.close();
		senderSocket.close();
		
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
		receivingACKs = true;
	}
	
	public static void terminateConnection() throws Exception{
		// Send first FIN
		nextPacket = new Segment(null, senderSequenceNumber, senderACKNumber, false, false, true);
		sendSegment(nextPacket);
		
		// Receive ACK
		received = receiveSegment();
//		if (!(received.isACK && received.ACKNumber == senderSequenceNumber + 1)){
//			throw new ConnectionException();
//		}
		
		// Receive FIN
		received = receiveSegment();
//		if (! (received.isFIN && (received.ACKNumber == senderSequenceNumber + 1))){
//			throw new ConnectionException();
//		}
		
		senderSequenceNumber = received.ACKNumber;
		senderACKNumber = received.sequenceNumber + 1;
		
		// Send final ACK
		nextPacket = new Segment(null, senderSequenceNumber, senderACKNumber, true, false, false);
		sendSegment(nextPacket);
	}

	public static void PLDmodule(Segment segmentToPLD)throws Exception{
		// Maintains numSegmentsHandledPLD, numSegmentsDroppedPLD, numSegmentsCorruptedPLD, numSegmentsReOrderedPLD,
		// numSegmentsDuplicated, numSegmentsDelayedPLD
		// Sets Event variables
		numSegmentsHandledPLD++;
		
		if (random.nextFloat() < pDrop){
			segmentToPLD.drop = true;
			numSegmentsDroppedPLD++;
		}
		else if(random.nextFloat() < pDuplicate){
			dupSegment =  new Segment(segmentToPLD);
			multipleSegment = true;
			segmentToPLD.dup = true;
			numSegmentsDuplicated++;
		}
		else if(random.nextFloat() < pCorrupt){
			byte[] bytesToFlip = new byte[4];
			System.arraycopy(segmentToPLD.segmentBytes, 32, bytesToFlip, 0, 4);
			ByteBuffer bb = ByteBuffer.wrap(bytesToFlip);
			int originalByte = bb.getInt();
			int newByte = originalByte + 1;
			bb.clear();
			bb.putInt(newByte);
			System.arraycopy(bb.array(), 0, segmentToPLD.segmentBytes, 32, 4);
			segmentToPLD.corr = true;
			numSegmentsCorruptedPLD++;
		}
		else if (random.nextFloat() < pOrder && reordering == false){
			reordering = true;
			reorderingSegment = new Segment(segmentToPLD);
			reorderCount = 0;
			reorderingSegment.rord = true;
			segmentToPLD.rord = true;
			numSegmentsReOrderedPLD++;
		}
		else if (random.nextFloat() < pDelay){
			delaying = true;
			delayPacketTime = (long)random.nextInt(maxDelay);
			delayTimer = System.currentTimeMillis();
			delayedSegment = new Segment(segmentToPLD);
			delayedSegment.dely = true;
			numSegmentsDelayedPLD++;
		}

	}
	
	// Logs segment information
	public static void logSegment(Segment segmentToLog){
		// Call Set Event
		segmentToLog.setEvent();
		segmentToLog.setTypeOfPacket();
		segmentToLog.setTime();
		System.out.printf("sender: %s     %f     %s     %d     %d     %d\n", segmentToLog.event, (double)((segmentToLog.packetTime - startTime) / 1000), segmentToLog.typeOfPacket, segmentToLog.sequenceNumber, segmentToLog.payloadLength, segmentToLog.ACKNumber);
		senderLog.printf("%s     %f     %s     %d     %d     %d\n", segmentToLog.event, (double)((segmentToLog.packetTime - startTime) / 1000), segmentToLog.typeOfPacket, segmentToLog.sequenceNumber, segmentToLog.payloadLength, segmentToLog.ACKNumber);
	}
	
	// Print Log Statistics
	public static void logStats(){
		senderLog.println("=========================================");
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
		senderLog.println("=========================================");
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
		if (sendingPacket.rord != true && sendingPacket.dely != true){
			if (! sendingPacket.isACK && segmentSentTimeHM.containsKey(sendingPacket.expectedACK)){
				segmentSentTimeHM.put(sendingPacket.expectedACK, (long)-1);
				sendingPacket.RXT = true;
			}
			sendingPacket.createDatagramPacket(receiver_host_ip, receiver_port);
			logSegment(sendingPacket);
			if (reordering == true){
				reorderCount++;
			}
			if (!sendingPacket.drop){
				senderSocket.send(sendingPacket.segment);
			}
			numSegmentsTrans++;
			if (! sendingPacket.isACK && !segmentSentTimeHM.containsKey(sendingPacket.expectedACK)){
				segmentSentTimeHM.put(sendingPacket.expectedACK, sendingPacket.packetTime);
			}
			if ((sendingPacket.sequenceNumber - 1) == leftEdgePointer && sendingData == true){
				timeoutTimer = System.currentTimeMillis();
			}
		}
	}
	
	public static void sendTamperedSegment(Segment sendingPacket) throws Exception{
		if (segmentSentTimeHM.containsKey(sendingPacket.expectedACK)){
			segmentSentTimeHM.put(sendingPacket.expectedACK, (long)-1);
			sendingPacket.RXT = true;
		}
		sendingPacket.createDatagramPacket(receiver_host_ip, receiver_port);
		logSegment(sendingPacket);
		senderSocket.send(sendingPacket.segment);
		numSegmentsTrans++;
		if (!segmentSentTimeHM.containsKey(sendingPacket.expectedACK)){
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
		if (justACKd == received.ACKNumber && receivingACKs == true){
			received.DA = true;
			multiACKcnt++;
			numDupAcks++;
		}
		else {
			justACKd = received.ACKNumber;
			multiACKcnt = 0;
		}
		logSegment(received);
		
		// Assuming that we do not adjust RTT when duplicate acks are received
		if (segmentSentTimeHM.containsKey(received.ACKNumber) && (segmentSentTimeHM.get(received.ACKNumber) != -1) && (received.DA != true)){
			adjustRTT(received.packetTime - segmentSentTimeHM.get(received.ACKNumber));
		}
		return received;
	}
}

class ConnectionException extends Exception{

	private static final long serialVersionUID = 1L;

	public ConnectionException(){
		super("Connection Error");
	}
}
