import java.io.*;
import java.net.*;

public class Sender {

	public static void main(String[] args) {
		
		// Check for correct number of arguments
		if (args.length != 14) {
			System.out.println("Incorrect number of arguments");
			return;
			}
		
		// Define input arguments
		String receiver_host_ipString = args[0];
		try {
			InetAddress receiver_host_ip = InetAddress.getByName(receiver_host_ipString);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return;
		}
		int receiver_port = Integer.parseInt(args[1]);
		String fileName = args[2];
		int MWS = Integer.parseInt(args[3]);
		int MSS = Integer.parseInt(args[4]);
		int gamma = Integer.parseInt(args[5]);
		
		// Input Arguments used by PLD Module
		double pDrop = Double.parseDouble(args[6]);
		double pDuplicate = Double.parseDouble(args[7]);
		double pCorrupt = Double.parseDouble(args[8]);
		double pOrder = Double.parseDouble(args[9]);
		int maxOrder = Integer.parseInt(args[10]);
		double pDelay = Double.parseDouble(args[11]);
		int maxDelay = Integer.parseInt(args[12]);
		int seed = Integer.parseInt(args[13]);
		
		
		
	}

}
