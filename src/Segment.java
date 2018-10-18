import java.net.*;
import java.security.*;
import java.nio.*;

public class Segment {
	
	// Constants
	final int ACKbit = 1;
	final int SYNbit = 2;
	final int FINbit = 4;
	final int headerLength = 32;
	final int checksumLength = 16;
	
	// Variables
	byte[] checksum;
	byte[] checksumComp;
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
	int expectedACK;
	long packetTime;
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
	
	// Constructor for Segments to be sent
	public Segment(Segment segmentToCopy)throws Exception{
		this.checksum = segmentToCopy.checksum;
		this.payloadLength = segmentToCopy.payloadLength;
		this.segmentLength = segmentToCopy.segmentLength;
		this.segmentPayloadData = segmentToCopy.segmentPayloadData;
		this.segmentHeader = segmentToCopy.segmentHeader;
		this.segmentBytes = segmentToCopy.segmentBytes;
		this.flags = segmentToCopy.flags;
		this.isACK = segmentToCopy.isACK;
		this.isSYN = segmentToCopy.isSYN;
		this.isFIN = segmentToCopy.isFIN;
		this.sequenceNumber = segmentToCopy.sequenceNumber;
		this.ACKNumber = segmentToCopy.ACKNumber;
		this.expectedACK = segmentToCopy.expectedACK;
		this.packetTime = segmentToCopy.packetTime;
		this.snd = segmentToCopy.snd;
		this.rcv = segmentToCopy.rcv;
		this.drop = segmentToCopy.drop;
		this.corr = segmentToCopy.corr;
		this.dup = segmentToCopy.dup;
		this.rord = segmentToCopy.rord;
		this.dely = segmentToCopy.dely;
		this.DA = segmentToCopy.DA;
		this.RXT = segmentToCopy.RXT;
	}
	public Segment(byte[] data, int seqNum, int ACKNum, boolean ACK, boolean SYN, boolean FIN) throws Exception{
		// Process packet information
		this.segmentPayloadData = data;
		if (this.segmentPayloadData == null || (SYN == true)){
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

		// Create Checksum of Data
		this.checksum = createChecksum(this.segmentPayloadData);

		// Create Header Byte Array
		ByteBuffer bb = ByteBuffer.allocate(4);
		this.segmentHeader = new byte[headerLength];
		bb.putInt(this.sequenceNumber);
		System.arraycopy(bb.array(), 0, this.segmentHeader, 0, 4);
		bb.clear();
		bb.putInt(this.ACKNumber);
		System.arraycopy(bb.array(), 0, this.segmentHeader, 4, 4);
		bb.clear();
		bb.putInt(this.flags);
		System.arraycopy(bb.array(), 0, this.segmentHeader, 8, 4);
		bb.clear();
		if (this.segmentPayloadData != null){
			bb.putInt(this.segmentPayloadData.length);
		}
		else bb.putInt(0);
		System.arraycopy(bb.array(), 0, this.segmentHeader, 12, 4);
		System.arraycopy(this.checksum, 0, this.segmentHeader, 16, 16);
		
		// Create Segment
		if (this.isSYN == true && this.isACK == false){
			this.segmentLength = headerLength + this.segmentPayloadData.length;
		}
		else this.segmentLength = headerLength + this.payloadLength;
		this.segmentBytes = new byte[this.segmentLength];
		System.arraycopy(this.segmentHeader, 0, this.segmentBytes, 0, this.headerLength);
		if (this.segmentPayloadData != null){
			System.arraycopy(this.segmentPayloadData, 0, this.segmentBytes, this.headerLength, this.segmentPayloadData.length);
		}

		if (this.isSYN == true || this.isACK == true || this.isFIN == true){
			this.expectedACK = this.sequenceNumber + 1;
		}
		else this.expectedACK = this.sequenceNumber + this.payloadLength;
		
		// Set PLD values to false
		this.snd = true;
		this.rcv = false;
		this.drop = false;
		this.corr = false;
		this.dup = false;
		this.rord = false;
		this.dely = false;
		this.DA = false;
		this.RXT = false;
	}
	
	public Segment(byte[] incomingSegmentData) throws Exception{
		// this.segmentBytes = incomingSegmentData;
		// this.segmentLength = this.segmentBytes.length;
		// Process Header
		this.segmentHeader = new byte[headerLength];
		System.arraycopy(incomingSegmentData, 0, this.segmentHeader, 0, headerLength);
		ByteBuffer bb = ByteBuffer.wrap(this.segmentHeader);
		this.sequenceNumber = bb.getInt();
		this.ACKNumber = bb.getInt();
		this.flags = bb.getInt();
		this.payloadLength = bb.getInt();
		this.checksum = new byte[checksumLength];
		System.arraycopy(this.segmentHeader, 16, this.checksum, 0, checksumLength);
		this.segmentLength = headerLength + this.payloadLength;
		this.segmentBytes = new byte[this.segmentLength];
		System.arraycopy(incomingSegmentData, 0, this.segmentBytes, 0, this.segmentLength);
		// Changed to what is above
//		System.arraycopy(this.checksum, 0, this.segmentHeader, 12, checksumLength);
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
		if (this.payloadLength > 0 && !this.isACK && !this.isFIN){
			this.segmentPayloadData = new byte[this.payloadLength];
			System.arraycopy(this.segmentBytes, headerLength, this.segmentPayloadData, 0, (this.payloadLength));
		}
		else this.segmentPayloadData = null;
		if (this.isSYN || this.segmentPayloadData == null){
			this.payloadLength = 0;
		}
		
		this.segmentLength = headerLength + this.payloadLength;

		// Process Checksum (sets corr)
		this.checksumComp = createChecksum(this.segmentPayloadData);
		processChecksum();

		// Set Events
		this.snd = false;
		this.rcv = true;
		this.drop = false;
		this.dup = false;
		this.rord = false;
		this.dely = false;
		this.DA = false;
		this.RXT = false;
	}
	
	public void createDatagramPacket(InetAddress destinationIP, int port){
		 this.segment =  new DatagramPacket(this.segmentBytes, this.segmentBytes.length, destinationIP, port);
	}
	
	public void setTime(){
		this.packetTime = System.currentTimeMillis();
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
	
	// Sets Packet Type
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

	// Implement Checksum creation
	public byte[] createChecksum(byte[] data) throws Exception{
		MessageDigest checksumDigest = MessageDigest.getInstance("MD5");
		if (data != null && this.isSYN == false && this.isACK == false && this.isFIN == false){
			checksumDigest.update(data);
			return checksumDigest.digest();
		}
		else {
			return new byte[16];
		}
	}
	
	// Process Checksum and edit state data
	public void processChecksum() throws Exception{
		this.corr = ! MessageDigest.isEqual(this.checksum, this.checksumComp);
	}

}
