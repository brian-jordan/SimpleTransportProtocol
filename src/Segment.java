import java.net.*;
import java.util.*;

public class Segment {
	
	// Constants
	final int ACKbit = 1;
	final int SYNbit = 2;
	final int FINbit = 4;
	final int headerLength = 20;
	
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
	long sendTime;
	DatagramPacket segment;
	
	// Events
	String event;
	String typeOfPacket;
	boolean snd;
	boolean rcv;
	boolean drop;
	boolean corr;
	boolean dup;
	boolean rord;
	boolean dely;
	boolean DA;
	boolean RXT;
	
	// TODO Might not need this parameter
	// Number of Times Acked
	int Acks;
	
	// Constructor for Segments to be sent
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
		
		// TODO
		// Create Checksum and append to header
		
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
		
		// Set PLD values to false
		this.snd = false;
		this.rcv = false;
		this.drop = false;
		this.corr = false;
		this.dup = false;
		this.rord = false;
		this.dely = false;
		this.DA = false;
		this.RXT = false;
		
		// Set number of times ackd to zero
		this.Acks = 0;
		
		// TODO
		// Set Event
		
		// TODO
		// Set Type of Packet
	}
	
	// Constructor for Segments received for processing 
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
		
		// TODO
		// Process Checksum
		
		// TODO
		// Set Event
		
		// TODO
		// Set Type of Packet
		
	}
	
	public void createDatagramPacket(InetAddress destinationIP, int port){
		 this.segment =  new DatagramPacket(this.segmentBytes, this.segmentBytes.length, destinationIP, port);
	}
	
	public void setSendTime(){
		this.sendTime = System.currentTimeMillis();
	}
	
	public void setEvent(){
		StringBuilder eventSB = new StringBuilder();
		if (this.snd == true){
			eventSB.append("snd/");
		}
		else if (this.rcv == true){
			eventSB.append("rcv/");
		}
		else if (this.drop == true){
			eventSB.append("drop/");
		}
		if (this.corr == true){
			eventSB.append("corr/");
		}
		else if (this.dup == true){
			eventSB.append("dup/");
		}
		else if (this.rord == true){
			eventSB.append("rord/");
		}
		else if (this.dely == true){
			eventSB.append("dely/");
		}
		if (this.DA == true){
			eventSB.append("DA/");
		}
		if (this.RXT == true){
			eventSB.append("RXT");
		}
		if (eventSB.charAt(eventSB.length() - 1) == '/'){
			eventSB.deleteCharAt(eventSB.length() - 1);
		}
		this.event = eventSB.toString();
	}
	
	public void setTypeOfPacket(){
		StringBuilder typeOfPacketSB = new StringBuilder();
		if (this.isSYN == true){
			typeOfPacketSB.append("S");
		}
		if (this.isACK == true){
			typeOfPacketSB.append("A");
		}
		if (this.isFIN == true){
			typeOfPacketSB.append("F");
		}
		if (typeOfPacketSB.length() == 0){
			typeOfPacketSB.append("D");
		}
		this.typeOfPacket = typeOfPacketSB.toString();
	}
	
	// TODO 
	// Implement Checksum creation
	
	// TODO
	// Implement Checksum processing 

}
