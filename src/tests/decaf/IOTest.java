package tests.decaf;

import java.io.IOException;
import java.util.Scanner;

/**************************************************************
 * IOTest -- tests all the I/O functions.
 **************************************************************/

public class IOTest {
  public static char[] readChars(Scanner s) throws IOException
  {
    char A[] = new char[100];
    int i = 0;
    while (true) {
      if (i == 99) {
        A[i] = '\n';
      } else {
        A[i] = (char) System.in.read();//IO.getChar();
      }
      if (A[i] == '\n') break;
      i = i + 1;
    }
    return A;
  }

  public static void putChars(char[] A)
  {
    int i = 0;
    while(i < A.length) {
      if (A[i] == '\n') break;
      System.out.print((char)A[i]);
      i = i + 1;
    }
    System.out.print("\n");
  }

  public static void main(String[] argv) throws IOException
  {
	  Scanner scanner = new Scanner(System.in);
    System.out.print("Please enter a string: ");
    String s = scanner.nextLine();
//    System.out.print("Please enter a floating-point number: ");
//    float f = IO.getFloat(); IO.getLine();
    System.out.print("Please enter an integer: ");
    int i = scanner.nextInt();  scanner.nextLine();
    System.out.print("Please enter some characters: ");
    char[] A = readChars(scanner);
    System.out.print("Please enter some more characters: ");
    char p = (char) System.in.read(); scanner.next();
    System.out.print("The string was: \"");
    System.out.print(s);
//    System.out.print("\"\nThe float was: ");
//    System.out.print(f);
    System.out.print("\nThe integer was: ");
    System.out.print(i);
    System.out.print("\nThe characters were: ");
    putChars(A);
    System.out.print("The first of the more characters is: ");
    System.out.print(p);
    System.out.print("\n");
    scanner.close();
  }
}