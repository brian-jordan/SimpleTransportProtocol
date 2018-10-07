import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Receiver {
	
	// Incoming Data Buffer
	static int fileLength_r;
	static byte[] fileData_r;
	
	// Socket Constants
	static DatagramSocket receiverSocket;
	static DatagramPacket incomingSegment;
	static Segment received;
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
		establishConnection();
		
		// Initialize Received Data Array
		fileData_r = new byte[fileLength_r];
		
		// TODO
		// Begin Accepting Data Segments
		
		// TODO
		// Terminate Connection with Sender
		
		// Print Log Statistics and Close Log
		logStats();
		receiverLog.close();
		
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
		
		// Set initial sequence and ack numbers
		receiverSequenceNumber = 0;
		receiverACKNumber = 0;
		
		// Receive First SYN
		incomingSegment = new DatagramPacket(new byte[1024], 1024);
		receiverSocket.receive(incomingSegment);
		recvBuffer = incomingSegment.getData();
		// Get Sender Information
		sender_host_ip = incomingSegment.getAddress();
		sender_port = incomingSegment.getPort();
		// Process Segment
		received = new Segment(recvBuffer);
		if (! received.isSYN){
			throw new ConnectionException();
		}
		byte[] fileLengthBytes_r = received.segmentPayloadData;
		String fileLengthString_r = new String(fileLengthBytes_r);
		fileLength_r = Integer.parseInt(fileLengthString_r);
		
		// Adjust Sequence and ACK Numbers
		receiverACKNumber = received.sequenceNumber + 1;
		
		// Send SYN + ACK
		Segment synAck = new Segment(null, receiverSequenceNumber, receiverACKNumber, true, true, false);
		synAck.createDatagramPacket(sender_host_ip, sender_port);
		receiverSocket.send(synAck.segment);
		
		// Receive Final ACK
		incomingSegment = new DatagramPacket(new byte[1024], 1024);
		receiverSocket.receive(incomingSegment);
		recvBuffer = incomingSegment.getData();
		// Process Segment
		received = new Segment(recvBuffer);
		if (! (received.isACK && (received.ACKNumber == receiverSequenceNumber + 1))){
			throw new ConnectionException();
		}
		// Adjust Sequence and ACK Numbers
		receiverSequenceNumber++;
		receiverACKNumber = received.sequenceNumber + 1;
	}
	
	// TODO
	// Logs segment information
	public static void logSegment(Segment segmentToLog){
		
	}
	
	// Print Log Statistics
	public static void logStats(){
		receiverLog.printf("Amount of Data Received: %d Bytes\n", amountDataReceived);
		receiverLog.printf("Total segments received: %d\n", numSegmentsReceived);
		receiverLog.printf("Data segments received: %d\n", numDataSegmentsReceived);
		receiverLog.printf("Data Segments with bit errors: %d\n", numDataSegmentsWBitErrors);
		receiverLog.printf("Duplicate data segments received: %d\n", numDupDataSegments);
		receiverLog.printf("Duplicate Acks sent: %d\n", numDupAcksSent);
	}

}
