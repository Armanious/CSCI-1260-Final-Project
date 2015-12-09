package tests.decaf;

class Car {
  private String builder;
  private String model;

  public Car(String b, String m)
  {
    builder = b;
    model = m;
  }

  public String builder()
  {
    return builder;
  }

  public String model()
  {
    return model;
  }
}

class Ford extends Car {
  public Ford(String x)
  {
    super("Ford", x);
  }
}

class Chevrolet extends Car {
  public Chevrolet(String x)
  {
    super("Chevrolet", x);
  }
}

class Escort extends Ford {
  public Escort()
  {
    super("Escort");
  }
}

class F150 extends Ford {
  public F150()
  {
    super("F150");
  }
}

class Suburban extends Chevrolet {
  public Suburban()
  {
    super("Suburban");
  }
}

class Caprice extends Chevrolet {
  public Caprice()
  {
    super("Caprice");
  }
}

public class Cars
{
  static void describe(Car c)
  {
    System.out.print(c.builder());
    System.out.print(" ");
    System.out.print(c.model());
    System.out.print("\n");
  }

  public static void main(String argv[])
  {
    Escort escort = new Escort();
    F150 f150 = new F150();
    Caprice caprice = new Caprice();
    Suburban suburban = new Suburban();

    describe(escort);
    describe(f150);
    describe(caprice);
    describe(suburban);
  }
}

