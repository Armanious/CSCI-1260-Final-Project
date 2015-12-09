package tests.decaf;

import java.util.Scanner;

public class Adder {
  static public void main(String[] argv)
  {
    System.out.print("enter two integers: ");
    Scanner s = new Scanner(System.in);
    int x = s.nextInt();
    int y = s.nextInt();
    s.close();
    
    System.out.print(x);
    System.out.print(" + ");
    System.out.print(y);
    System.out.print(" = ");
    System.out.print(x+y);
    System.out.print("\n");
  }
}