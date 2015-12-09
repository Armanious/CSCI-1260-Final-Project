package tests.decaf;

class CX
{
  String val; // comment 1

  CX() { val = "a"; } /* comme
  nt */
  String f() { return "b"; }
  static String g() { return "c"; }

}

class CY extends CX
/* commenet // "sddfhjasdjfkls" 'sd'fsadkljjksdahfkjs\\ // */
{
  String val;
  // /*\f\d\d\d\d /* sdkfjadjflksd

/*
/ /*/

  CY() { val = "d"; }
  String f() { return "e"; }
  static String g() { return "f"; }
  void abcdef()
  {
    System.out.print(super.val);
    System.out.print(super.f());
    System.out.print(super.g());
    System.out.print(val);
    System.out.print(f());
    System.out.print(g());
    System.out.print("\n");
  }
}

public class Comments {
  public static void main(String argv[])
  {
    System.out.print("Expected Output: abcdef\n  Actual Output: ");
    (new CY()).abcdef();
  }
}
