package tests.decaf;

class X
{
  String val;

  X() { val = "a"; }
  String f() { return "b"; }
  static String g() { return "c"; }
}

class Y extends X
{
  String val;

  Y() { val = "d"; }
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

public class SuperTest {
  public static void main(String argv[])
  {
    System.out.print("Expected Output: abcdef\n  Actual Output: ");
    (new Y()).abcdef();
  }
}