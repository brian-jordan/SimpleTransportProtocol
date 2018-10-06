import java.io.*;
import java.net.DatagramSocket;

public class Receiver {
	
	// Receiver file variables
	static OutputStream receiverFileOS;
	static File fReceiver;
	static String fileName_r;
	// Figure out how to find file length when creating this
	static long fileLength_r;
	static byte[] fileData_r = new byte[6];

	public static void main(String[] args) throws Exception{
		// Check for correct number of arguments
		if (args.length != 2) {
			System.out.println("Incorrect number of arguments");
			return;
			}
		
		// Define input arguments
		int receiver_port = Integer.parseInt(args[0]);
		fileName_r = args[1];
		
		// Convert byte array back into PDF
		byteArrayToPDF();
		
		// Initialize connection with Sender
		DatagramSocket socket = new DatagramSocket(receiver_port);

	}
	
	// Convert byte array back into PDF
	public static void byteArrayToPDF(){
		fReceiver = new File(fileName_r);
		try {
			receiverFileOS = new FileOutputStream(fReceiver);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		try {
			receiverFileOS.write(fileData_r);
			receiverFileOS.close();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}
	

}
