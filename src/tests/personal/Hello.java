package tests.personal;


import java.lang.reflect.Method;

public class Hello {
	
	private static void screwMeUp(){
		int[] arr = null;
		if(System.currentTimeMillis() < System.currentTimeMillis()){
			arr = new int[100];
		}
		if(arr != null){
			System.out.println(arr[0]);
		}
	}
	
	private static void fundamentalLoopTest(int p){
		char constantChar = 'f';
		int arr[][] = new int[10][10];
		for(int i = 0; i < arr.length; i++){
			for(int j = 0; j < arr[i].length; j++){
				arr[i][j] = i * i + j;
			}
		}
	}
	//we want this effectively converted to:
	private static void fundamentalLoopTestDesired(int p){
		int arr[][] = new int[10][10];
		for(int i = 0; i < 10; i++){
			int subArrayLength = arr[i].length;
			int iSquared = i * i;
			for(int j = 0; j < subArrayLength; j++){
				arr[i][j] = iSquared + j;
			}
		}
	}

	private static void loopTest(){
		
		boolean b = true || false;
		System.out.println(b);

		int kasd = 23;
		int jasd = 93;

		int qasd = kasd * jasd * kasd;
		int lasd = kasd * kasd * jasd;
		int pasd = jasd * kasd * kasd;

		System.out.println(qasd + "\n" + lasd + "\n" + pasd);

		int[][][] t = new int[1000][1000][100];
		//On my machine:
		//unoptimized: ~3270-3310 ms
		//optimized: ~2990-3030 ms
		//thats almost a 10% improvement
		//reasonable to expect a 5-8% runtime improvement with current optimizations
		int qb = 0;
		for(int i = 0; i < t.length; i++){
			for(int j = 0; j < t[i].length; j++){
				for(int q = 0; q < t[i][j].length; q++){
					t[i][j][q] = (int)(Math.random() * 100);
				}
				for(int q = 0; q < t[i][j].length; q++){
					t[i][j][q] = t[i][j][q] * t[i][j][q];
					qb += t[i][j][q] * t[i][j][q];
				}
			}
		}


	}

	public static void testMethod(){
		System.err.println("Calling testMethod");
	}

	public static void main(String[] args) throws Exception {
		System.out.println(Class.forName("tests.personal.Hello").getName());
		Method m = Class.forName("tests.personal.Hello").getMethod("testMethod", new Class[]{});
		
		m.invoke(null, new Object[]{});

		System.out.println("Hello world!");
		Hello l = new Hello();
		try{
			Inner[][][] t = new Inner[20][20][40];
			t[10][1][3] = l.new Inner();
			t[10][1][3].doSomethingElse();
		}catch(Throwable t){

		}


		final long start = System.currentTimeMillis();
		loopTest();
		final long end = System.currentTimeMillis();

		System.out.println("loopTest took " + (end - start) + " ms.");

		try {
			l.doSomething();
		} catch (T e) {
			e.printStackTrace();
		}
		HelloExtended he = new HelloExtended();
		he.hi();
		he.appendString("obfuscated", "java");
	}

	protected int api = 5;

	public void doSomething() throws T {
		System.out.println("Hello world #2");
		Inner i = new Inner();
		i.doSomethingElse();
	}

	class Inner {

		public void doSomethingElse() {
			System.out.println("Hello world #3");

			int j;
			if(api == 2){
				j = 4;
			}else if(api == 4){
				j = 7;
			}else{
				j = 23;
			}
			System.out.println(j);
			System.err.println(j);

			InnerOfInner t2 = new InnerOfInner(){
				{
					System.out.println("From InnerAnonymous");
				}
			};
			System.out.println(t2);
		}

		public class InnerOfInner {
			{
				System.out.println(this.getClass().getName());
			}
		}

	}

}
