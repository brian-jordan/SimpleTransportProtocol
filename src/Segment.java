import java.util.*;

public class Segment {
	
	// Constants
	final int ACKbit = 1;
	final int SYNbit = 2;
	final int FINbit = 4;
	
	// Variables
	String header = "";
	int payloadLength;
	int headerLength;
	byte[] segmentPayloadData;
	byte[] segmentHeader;
	byte[] segment;
	int flags = 0;
	int sequenceNumber;
	int ACKNumber;
	
	public Segment(byte[] data,boolean ACK, boolean SYN, boolean FIN, int seqNum, int ACKNum){
		// Process packet information
		this.segmentPayloadData = data;
		if (ACK == true){
			this.flags = this.flags | ACKbit;
		}
		if (SYN == true){
			this.flags = this.flags | SYNbit;
		}
		if (FIN == true){
			this.flags = this.flags | FINbit;
		}
		this.sequenceNumber = seqNum;
		this.ACKNumber = ACKNum;
		
		// Create Header
		this.header = this.header + this.sequenceNumber + this.ACKNumber + flags;
		this.segmentHeader = this.header.getBytes();
		this.headerLength = 1 + this.segmentHeader.length;
		this.header = this.headerLength + this.header;
		this.segmentHeader = this.header.getBytes();
		
		// Create Segment
		segmentPayloadData = new byte[this.segmentHeader.length + this.segmentPayloadData.length];
		int index = 0;
		for (int i = 0; i < this.segmentHeader.length; i++){
			this.segment[index] = this.segmentHeader[i];
			index++;
		}
		for(int i = 0; i < this.segmentPayloadData.length; i++){
			this.segment[index] = this.segmentPayloadData[i];
			index++;
		}
	}

}
