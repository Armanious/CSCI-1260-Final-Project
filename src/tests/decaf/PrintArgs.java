package tests.decaf;

public class PrintArgs {

  public static void main(String argv[])
  {
    int i = 0;
    while (i < argv.length) {
      System.out.print("argv[");
      System.out.print(i);
      System.out.print("] = \"");
      System.out.print(argv[i]);
      System.out.print("\"\n");
      i = i + 1;
    }
  }
}