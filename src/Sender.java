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
	static long maxRTO = 60000;
	
	// Socket Constants
	static DatagramSocket senderSocket;
	static DatagramPacket incomingSegment;
	static Segment received;
	static Segment nextPacket;
	static Segment resendingSegment;
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
//	static HashMap<Integer, Long> segmentSentTimeHM;
	static boolean calculateRTT;
	static long RTTReference;
	static int RTTWaiting;
	
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
		
		// Convert file into byte array
		fileData_s = pdfToByteArray(fileName_s);
		
		// Initialize HashMap for segment send times
//		segmentSentTimeHM = new HashMap<>();
		
		
		// Find length of file to send during initialization
		fileLength = fileData_s.length;
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(fileLength);
		byte[] fileLengthBytes = bb.array();
		
		// Initialize Sender Log File
		senderLog = new PrintWriter("Sender_log.txt");
		senderLog.println("<event>     <time> <type-of-packet> <seq-number> <number-of-bytes-data> <ack-number>");
		
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
		
		// Initialize Receiver Thread
		Thread receiverThread = new Thread(){
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
							resendDataSegment = new byte[MSS];
							System.arraycopy(fileData_s, leftEdgePointer, resendDataSegment, 0, MSS);
							resendingSegment = new Segment(resendDataSegment, leftEdgePointer + 1, senderACKNumber, false, false, false);
							resendingSegment.RXT = true;
//							segmentSentTimeHM.put(resendingSegment.expectedACK, (long)-1);
							calculateRTT = false;
							numFastRetrans++;
							timeoutTimer = System.currentTimeMillis();
							multiACKcnt = 0;
							sendSegment(resendingSegment);
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
		};
		
		Thread tamperedSenderThread = new Thread(){
			public void run(){
				while (sendingData == true){
					if (reordering == true && reorderCount == maxOrder){
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
				while (sendingData == true){
					calculateRTT = true;
					RTTReference = System.currentTimeMillis();
					RTTWaiting = nextSendPointer + 1 + MSS;
					while (((nextSendPointer + MSS - 1) < rightEdgePointer) && rightEdgePointer <= fileLength){
						byte[] nextDataSegment = new byte[MSS];
						System.arraycopy(fileData_s, nextSendPointer, nextDataSegment, 0, MSS);
						try {
							nextPacket = new Segment(nextDataSegment, nextSendPointer + 1, senderACKNumber, false, false, false);
							if (sendingData == true && nextSendPointer == leftEdgePointer){
								timeoutTimer = System.currentTimeMillis();
								calculateRTT = true;
								RTTReference = System.currentTimeMillis();
								RTTWaiting = nextSendPointer + 1 + MSS;
							}
							nextSendPointer = nextSendPointer + MSS;
							sendSegment(nextPacket);
						} catch (Exception e) {
							e.printStackTrace();
						}
						try {
							sleep(1);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
					if ((fileLength - nextSendPointer) < MSS && (nextSendPointer < fileLength)){
						byte[] lastDataSegment = new byte[fileLength - nextSendPointer];
						System.arraycopy(fileData_s, nextSendPointer, lastDataSegment, 0, fileLength - nextSendPointer);
						try {
							nextPacket = new Segment(lastDataSegment, nextSendPointer + 1, senderACKNumber, false, false, false);
							if (sendingData == true && nextSendPointer == leftEdgePointer){
								timeoutTimer = System.currentTimeMillis();
								calculateRTT = true;
								RTTReference = System.currentTimeMillis();
								RTTWaiting = nextSendPointer + 1 + lastDataSegment.length;
							}
							nextSendPointer = nextSendPointer + lastDataSegment.length;
							sendSegment(nextPacket);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					if ((System.currentTimeMillis() - timeoutTimer) >= timeout){
						byte[] resendDataSegment;
						resendDataSegment = new byte[MSS];
						if ((fileLength - leftEdgePointer) < MSS){
							resendDataSegment = new byte[fileLength - leftEdgePointer];
						}
						else resendDataSegment = new byte[MSS];
						System.arraycopy(fileData_s, leftEdgePointer, resendDataSegment, 0, resendDataSegment.length);
						try {
							resendingSegment = new Segment(resendDataSegment, leftEdgePointer + 1, senderACKNumber, false, false, false);
							resendingSegment.RXT = true;
//							segmentSentTimeHM.put(resendingSegment.expectedACK, (long)-1);
							calculateRTT = false;
							numTimeoutRetrans++;
							timeoutTimer = System.currentTimeMillis();
							sendSegment(resendingSegment);
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
		terminateConnection();

		
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
		
		// Receive FIN
		received = receiveSegment();
		
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
			sendSegment(dupSegment);
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
			reorderingSegment = new Segment(segmentToPLD);
			reorderCount = 0;
			reorderingSegment.rord = true;
			segmentToPLD.rord = true;
			reordering = true;
			numSegmentsReOrderedPLD++;
		}
		else if (random.nextFloat() < pDelay){
			delayPacketTime = (long)random.nextInt(maxDelay);
			delayTimer = System.currentTimeMillis();
			delayedSegment = new Segment(segmentToPLD);
			segmentToPLD.dely = true;
			delayedSegment.dely = true;
			delaying = true;
			numSegmentsDelayedPLD++;
		}

	}
	
	// Logs segment information
	public static void logSegment(Segment segmentToLog){
		// Call Set Event
		segmentToLog.setEvent();
		segmentToLog.setTypeOfPacket();
		segmentToLog.setTime();
		senderLog.printf("%-12s%-7.2f%-17s%-13d%-23d%-12d\n", segmentToLog.event, ((segmentToLog.packetTime - startTime) / (double)1000), segmentToLog.typeOfPacket, segmentToLog.sequenceNumber, segmentToLog.payloadLength, segmentToLog.ACKNumber);
	}
	
	// Print Log Statistics
	public static void logStats(){
		senderLog.println("==================================================");
		senderLog.printf("%-50s%d Bytes\n", "Size of the file:",fileLength);
		senderLog.printf("%-50s%d\n", "Segments transmitted:",numSegmentsTrans);
		senderLog.printf("%-50s%d\n", "Number of Segments handled by PLD:", numSegmentsHandledPLD);
		senderLog.printf("%-50s%d\n", "Number of Segments Dropped:", numSegmentsDroppedPLD);
		senderLog.printf("%-50s%d\n", "Number of Segments Corrupted:", numSegmentsCorruptedPLD);
		senderLog.printf("%-50s%d\n", "Number of Segments Re-ordered:", numSegmentsReOrderedPLD);
		senderLog.printf("%-50s%d\n", "Number of Segments Duplicated: ", numSegmentsDuplicated);
		senderLog.printf("%-50s%d\n", "Number of Segments Delayed: ", numSegmentsDelayedPLD);
		senderLog.printf("%-50s%d\n", "Number of Retransmissions due to timeout:", numTimeoutRetrans);
		senderLog.printf("%-50s%d\n", "Number of Fast Retransmissions:", numFastRetrans);
		senderLog.printf("%-50s%d\n", "Number of Duplicate Acknowledgements received:", numDupAcks);
		senderLog.println("==================================================");
	}
	
	// Maintain RTT value
	public static void adjustRTT(long sampleRTT){
		estimatedRTT = (long)((0.875 * estimatedRTT) + (0.125 * sampleRTT));
		devRTT = (long)((0.75 * devRTT) + (0.25 * Math.abs(sampleRTT - estimatedRTT)));
		timeout = estimatedRTT + (gamma * devRTT);
	}
	
	public static void sendSegment(Segment sendingPacket) throws Exception{
		if (!(sendingPacket.isSYN || sendingPacket.isACK || sendingPacket.isFIN || sendingPacket.dup)){
			PLDmodule(sendingPacket);
		}
		if (sendingPacket.rord != true && sendingPacket.dely != true){
			sendingPacket.createDatagramPacket(receiver_host_ip, receiver_port);
			logSegment(sendingPacket);
			if (reordering == true){
				reorderCount++;
			}
			if (!sendingPacket.drop){
				senderSocket.send(sendingPacket.segment);
			}
			numSegmentsTrans++;
//			if (!segmentSentTimeHM.containsKey(sendingPacket.expectedACK) && (sendingPacket.dup != true)){
//				segmentSentTimeHM.put(sendingPacket.expectedACK, sendingPacket.packetTime);
//			}
		}
	}
	
	public static void sendTamperedSegment(Segment sendingPacket) throws Exception{
		sendingPacket.createDatagramPacket(receiver_host_ip, receiver_port);
		logSegment(sendingPacket);
		if (reordering == true){
			reorderCount++;
		}
		senderSocket.send(sendingPacket.segment);
		numSegmentsTrans++;
//		if (!segmentSentTimeHM.containsKey(sendingPacket.expectedACK)){
//			segmentSentTimeHM.put(sendingPacket.expectedACK, sendingPacket.packetTime);
//		}
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
		
		if (calculateRTT == true && RTTWaiting == received.ACKNumber && received.DA != true){
			long potentialRTT = received.packetTime - RTTReference;
			if(potentialRTT < maxRTO){
				adjustRTT(potentialRTT);
			}
		}
		return received;
	}
}
