
public class Test {
	
	static int num1;
	static int num2;
	
	public static void main(String[] args){
		
		byte flipping = (byte)0x90;
		System.out.println(flipping);
		
		byte isFlipped = (byte) (flipping ^ 0x80);
		
		
		System.out.println(isFlipped);
	}

}
