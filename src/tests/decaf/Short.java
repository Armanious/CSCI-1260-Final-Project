package tests.decaf;

public class Short {

	public static void main(String[]args) {
		int i = 0;
		int f0 = 1;
		int f1 = 1;

		while (i < 10) {
			int f2 = f0 + f1;
			f0 = f1;
			f1 = f2;
			i = i+1;
		}

		System.out.print(f1);
		System.out.print("\n");
	}

}

