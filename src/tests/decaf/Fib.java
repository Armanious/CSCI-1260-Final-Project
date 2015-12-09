package tests.decaf;
public class Fib {

  static int fib(int n)
  {
     if (n <= 2)
       return 1;
     return fib(n-1) + fib(n-2);
  }

  static void printFibs(int n)
  {
    int i = 1;
    while (i <= n) {
      System.out.print("fib(");
      System.out.print(i);
      System.out.print(") = ");
      System.out.print(fib(i));
      System.out.print("\n");
      i = i + 1;
    }
  }
  
  public static void main(String[] argv)
  {
    printFibs(20);
  }
}
