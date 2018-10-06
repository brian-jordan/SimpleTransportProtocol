import java.io.*;
import java.net.*;

public class Sender {
	
	// PLD Module Arguments
	static double pDrop;
	static double pDuplicate;
	static double pCorrupt;
	static double pOrder;
	static int maxOrder;
	static double pDelay;
	static int maxDelay;
	static int seed;
	
	// File Arguments
	static String fileName_s;
	static File fSender;
	static long fileLength_s;
	static byte[] fileData_s;
	static FileInputStream fileIS;

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
		fileName_s = args[2];
		int MWS = Integer.parseInt(args[3]);
		int MSS = Integer.parseInt(args[4]);
		int gamma = Integer.parseInt(args[5]);
		
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
		pdfToByteArray();
		
		// TODO
		// Initialize Connection with Receiver
		DatagramSocket senderSocket = new DatagramSocket();
		
		
		
		
		
		// TODO
		// If segment is not for connection or teardown send it through PLD module
		
		// Have a listener thread waiting for acks and a sender thread sending packets
	}
	
	// Convert file into byte array
	public static void pdfToByteArray() throws Exception{
		fSender = new File(fileName_s);
		fileLength_s = fSender.length();
		fileData_s = new byte[(int)fileLength_s];
		fileIS = new FileInputStream(fSender);
		fileIS.read(fileData_s);
		fileIS.close();
	}
	
	public void PLDmodule(DatagramPacket segment){
		
	}

}
