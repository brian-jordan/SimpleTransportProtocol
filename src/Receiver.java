import java.io.*;
import java.net.*;
import java.nio.*;

public class Receiver {
	
	// Incoming Data Buffer
	static int fileLength_r;
	static byte[] fileData_r;
	
	// Socket Constants
	static DatagramSocket receiverSocket;
	static DatagramPacket incomingSegment;
	static Segment received;
	static Segment nextACK;
	static byte[] recvBuffer;
	static int receiverSequenceNumber;
	static int receiverACKNumber;
	static PrintWriter receiverLog;
	
	// Sender Information
	static InetAddress sender_host_ip;
	static int sender_port;
	
	// Data to Log
	static int amountDataReceived;
	static int numSegmentsReceived;
	static int numDataSegmentsReceived;
	static int numDataSegmentsWBitErrors;
	static int numDupDataSegments;
	static int numDupAcksSent;
	
	// Receiving info
	static int nextACKIndex;
	static boolean receivingData;
	
	// Begin Time
	static long startTime;

	public static void main(String[] args) throws Exception{
		// Check for correct number of arguments
		if (args.length != 2) {
			System.out.println("Incorrect number of arguments");
			return;
			}
		
		// Define input arguments
		int receiver_port = Integer.parseInt(args[0]);
		String fileName_r = args[1];
		
		// Initialize Sender Log File
		receiverLog = new PrintWriter("Receiver_log.txt");
		receiverLog.println("<event> <time> <type-of-packet> <seq-number> <number-of-bytes-data> <ack-number>");
		
		// Initialize connection with Sender
		receiverSocket = new DatagramSocket(receiver_port);
		startTime = System.currentTimeMillis();
		establishConnection();
		
		// Initialize Received Data Array
		fileData_r = new byte[fileLength_r];
		
		// TODO
		// Begin Accepting Data Segments
		nextACKIndex = 0;
		receivingData = true;
		numDataSegmentsReceived = 0;
		numDataSegmentsWBitErrors = 0;
		amountDataReceived = 0;
		numDupDataSegments = 0;
		numDupAcksSent = 0;
		receiveDataSegments();
		// Maintains amountDataReceived, numSegmentsReceived, numDataSegmentsReceived, numDataSegmentsWBitErrors, numDupDataSegments, numDupAcksSent
		// Process Packet
		// Log
		// Create ACK Packet
		// create datagram
		// Log
		// Send ACK Packet
		
		// TODO
		// Terminate Connection with Sender
		terminateConnection();
		
		// Print Log Statistics and Close Log
		logStats();
		receiverLog.close();
		receiverSocket.close();
		
		// Convert byte array back into PDF
		byteArrayToPDF(fileName_r);
	}
	
	// Convert byte array back into PDF
	public static void byteArrayToPDF(String fName) throws Exception{
		File fReceiver = new File(fName);
		OutputStream receiverFileOS = new FileOutputStream(fReceiver);
		receiverFileOS.write(fileData_r);
		receiverFileOS.close();
	}
	
	// Establish connection with sender
	public static void establishConnection() throws Exception{
		// Good to Go
		
		// Set initial value of numSegmentsReceived
		numSegmentsReceived = 0;
		
		// Set initial sequence and ack numbers
		receiverSequenceNumber = 0;
		receiverACKNumber = 0;
		
		// Receive First SYN
		received = receiveSegment();
		
		// Get Sender Information
		sender_host_ip = incomingSegment.getAddress();
		sender_port = incomingSegment.getPort();
		
		// Process Segment
		if (! received.isSYN){
			throw new ConnectionException();
		}
		ByteBuffer bb = ByteBuffer.wrap(received.segmentPayloadData);
		fileLength_r = bb.getInt();
		
		// Adjust Sequence and ACK Numbers
		receiverACKNumber = received.sequenceNumber + 1;
		
		// Send SYN + ACK
		nextACK = new Segment(null, receiverSequenceNumber, receiverACKNumber, true, true, false);
		sendSegment(nextACK);
		
		// Receive Final ACK
		received = receiveSegment();
		
		// Process Segment
		if (! (received.isACK && (received.ACKNumber == receiverSequenceNumber + 1))){
			throw new ConnectionException();
		}
		
		// Adjust Sequence and ACK Numbers
		receiverSequenceNumber++;
	}
	
	public static void terminateConnection() throws Exception{
		// Send ACK in Response to FIN
		nextACK = new Segment(null, receiverSequenceNumber, receiverACKNumber, true, false, false);
		sendSegment(nextACK);
		
		// Send FIN
		nextACK = new Segment(null, receiverSequenceNumber, receiverACKNumber, false, false, true);
		sendSegment(nextACK);
		
		// Receiver Final ACK
		received = receiveSegment();
		if (! (received.isACK && (received.ACKNumber == receiverSequenceNumber + 1))){
			throw new ConnectionException();
		}
	}

	// Logs segment information
	public static void logSegment(Segment segmentToLog){
		// Call Set Event
		segmentToLog.setEvent();
		segmentToLog.setTypeOfPacket();
		segmentToLog.setTime();
		receiverLog.printf("%s     %f     %s     %d     %d     %d\n", segmentToLog.event, (double)((segmentToLog.packetTime - startTime) / 1000), segmentToLog.typeOfPacket, segmentToLog.sequenceNumber, segmentToLog.payloadLength, segmentToLog.ACKNumber);
	}
	
	public static void receiveDataSegments()throws Exception{
		while (receivingData == true){
			received = receiveSegment();
			if (received.isFIN == true){
				receivingData = false;
				continue;
			}
			numDataSegmentsReceived++;
			amountDataReceived += received.payloadLength;
			if (received.corr == true){
				numDataSegmentsWBitErrors++;
				continue;
			}
			if (fileData_r[received.sequenceNumber - 1] != 0){
				numDupDataSegments++;
				continue;
			}
			else System.arraycopy(received.segmentPayloadData, 0, fileData_r, (received.sequenceNumber - 1), received.payloadLength);
			nextACK = new Segment(null, receiverSequenceNumber, receiverACKNumber, true, false, false);
			if (received.sequenceNumber == receiverACKNumber){
				for (int i = receiverACKNumber - 1; i < fileLength_r; i++){
					if (fileData_r[i] == 0){
						receiverACKNumber = i + 1;
						nextACK = new Segment(null, receiverSequenceNumber, receiverACKNumber, true, false, false);
						break;
					}
				}
			}
			else {
				nextACK.DA = true;
				numDupAcksSent++;
			}
			sendSegment(nextACK);
		}
	}
	
	public static Segment receiveSegment() throws Exception{
		incomingSegment = new DatagramPacket(new byte[1024], 1024);
		receiverSocket.receive(incomingSegment);
		recvBuffer = incomingSegment.getData();
		
		// Process Segment
		received = new Segment(recvBuffer);
		logSegment(received);
		
		numSegmentsReceived++;
		return received;
	}
	
	public static void sendSegment(Segment sendingPacket) throws Exception{
		sendingPacket.createDatagramPacket(sender_host_ip, sender_port);
		logSegment(sendingPacket);
		receiverSocket.send(sendingPacket.segment);
	}
	
	// Print Log Statistics
	public static void logStats(){
		receiverLog.println("=========================================");
		receiverLog.printf("Amount of Data Received: %d Bytes\n", amountDataReceived);
		receiverLog.printf("Total segments received: %d\n", numSegmentsReceived);
		receiverLog.printf("Data segments received: %d\n", numDataSegmentsReceived);
		receiverLog.printf("Data Segments with bit errors: %d\n", numDataSegmentsWBitErrors);
		receiverLog.printf("Duplicate data segments received: %d\n", numDupDataSegments);
		receiverLog.printf("Duplicate Acks sent: %d\n", numDupAcksSent);
		receiverLog.println("=========================================");
	}

}
