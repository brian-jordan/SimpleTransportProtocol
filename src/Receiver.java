import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Receiver {
	
	// TODO
	// Figure out how to find file length when creating this
	static long fileLength_r;
	static byte[] fileData_r = new byte[6];
	
	static DatagramSocket receiverSocket;
	static DatagramPacket request;
	static Segment received;
	static byte[] recvBuffer;
	static int receiverSequenceNumber;
	static int receiverACKNumber;
	
	static InetAddress sender_host_ip;
	static int sender_port;

	public static void main(String[] args) throws Exception{
		// Check for correct number of arguments
		if (args.length != 2) {
			System.out.println("Incorrect number of arguments");
			return;
			}
		
		// Define input arguments
		int receiver_port = Integer.parseInt(args[0]);
		String fileName_r = args[1];
		
		// Convert byte array back into PDF
		byteArrayToPDF(fileName_r);
		
		// Initialize connection with Sender
		receiverSocket = new DatagramSocket(receiver_port);
		establishConnection();
		

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
		request = new DatagramPacket(new byte[1024], 1024);
		receiverSocket.receive(request);
		recvBuffer = request.getData();
		// Get Sender Information
		sender_host_ip = request.getAddress();
		sender_port = request.getPort();
		// Process Segment
		received = new Segment(recvBuffer);
		if (! received.isSYN){
			throw new ConnectionException();
		}
		
		// Adjust Sequence and ACK Numbers
		receiverACKNumber = received.sequenceNumber + 1;
		
		// Send SYN + ACK
		Segment synAck = new Segment(null, receiverSequenceNumber, receiverACKNumber, true, true, false);
		synAck.createDatagramPacket(sender_host_ip, sender_port);
		receiverSocket.send(synAck.segment);
		
		// Recieve Final ACK
		request = new DatagramPacket(new byte[1024], 1024);
		receiverSocket.receive(request);
		recvBuffer = request.getData();
		// Process Segment
		received = new Segment(recvBuffer);
		if (! (received.isACK && (received.ACKNumber == receiverSequenceNumber + 1))){
			throw new ConnectionException();
		}
		// Adjust Sequence and ACK Numbers
		receiverSequenceNumber++;
		receiverACKNumber = received.sequenceNumber + 1;
	}
	

}
