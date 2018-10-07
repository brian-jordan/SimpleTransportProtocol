import java.net.*;
import java.util.Arrays;

public class Segment {
	
	// Constants
	final int ACKbit = 1;
	final int SYNbit = 2;
	final int FINbit = 4;
	final int headerLength = 16;
	
	// Variables
	String header = "";
	int payloadLength;
	int segmentLength;
	byte[] segmentPayloadData;
	byte[] segmentHeader;
	byte[] segmentBytes;
	int flags = 0;
	boolean isACK;
	boolean isSYN;
	boolean isFIN;
	int sequenceNumber;
	int ACKNumber;
	DatagramPacket segment;
	
	public Segment(byte[] data, int seqNum, int ACKNum, boolean ACK, boolean SYN, boolean FIN){
		// Process packet information
		this.segmentPayloadData = data;
		if (this.segmentPayloadData == null){
			this.payloadLength = 0;
		}
		else this.payloadLength = this.segmentPayloadData.length;
		this.isACK = ACK;
		this.isSYN = SYN;
		this.isFIN = FIN;
		if (this.isACK == true){
			this.flags = this.flags | ACKbit;
		}
		if (this.isSYN == true){
			this.flags = this.flags | SYNbit;
		}
		if (this.isFIN == true){
			this.flags = this.flags | FINbit;
		}
		this.sequenceNumber = seqNum;
		this.ACKNumber = ACKNum;
		
		// Create Header
		this.header = this.header + this.sequenceNumber + "a" + this.ACKNumber + "f" + this.flags;
		this.segmentHeader = this.header.getBytes();
		
		// Create Segment
		this.segmentLength = headerLength + this.payloadLength;
		this.segmentBytes = new byte[this.segmentLength];
		int index = 0;
		for (int i = 0; i < this.segmentHeader.length; i++){
			this.segmentBytes[index] = this.segmentHeader[i];
			index++;
		}
		if (this.segmentPayloadData != null){
			for(int i = 0; i < this.segmentPayloadData.length; i++){
				this.segmentBytes[index] = this.segmentPayloadData[i];
				index++;
			}
		}
	}
	
	public Segment(byte[] incomingSegmentData){
		// Process Header
		this.segmentBytes = incomingSegmentData;
		this.segmentHeader = Arrays.copyOfRange(this.segmentBytes, 0, headerLength);
		this.header = new String(this.segmentHeader);
		this.sequenceNumber = Integer.parseInt(this.header.substring(0, this.header.indexOf('a')));
		this.ACKNumber = Integer.parseInt(this.header.substring(this.header.indexOf('a'), this.header.indexOf('f')));
		this.flags = Integer.parseInt(this.header.substring(this.header.indexOf('f'), this.header.length()));
		if ((this.flags & ACKbit) != 0){
			this.isACK = true;
		}
		else this.isACK = false;
		if ((this.flags & SYNbit) != 0){
			this.isSYN = true;
		}
		else this.isSYN = false;
		if ((this.flags & FINbit) != 0){
			this.isFIN = true;
		}
		else this.isFIN = false;
		
		// Process Data
		if (this.segmentBytes.length > this.segmentHeader.length){
			this.segmentPayloadData = Arrays.copyOfRange(this.segmentBytes, headerLength, this.segmentBytes.length);
			this.payloadLength = this.segmentPayloadData.length;
		}
		else {
			this.segmentPayloadData = null;
			this.payloadLength = 0;
		}
	}
	
	public void createDatagramPacket(InetAddress destinationIP, int port){
		 this.segment =  new DatagramPacket(this.segmentBytes, this.segmentBytes.length, destinationIP, port);
	}

}
