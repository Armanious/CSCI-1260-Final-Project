package tests.personal;


public class HelloExtended extends Hello {
	
	private int i;
	private int j;
	private byte k;
	
	private String someString;
	
	public void appendString(String a, String b){
		someString = a + b;
	}
	
	public int hi(){
		System.out.println(api);
		
		
		byte i = 122;
		byte j = 65;
		
		k = (byte) (i + j);
		
		byte pres = (byte) (j + i);
		
		System.out.println("i = " + i);
		System.out.println("j = " + j);
		
		return k;
	}

}
