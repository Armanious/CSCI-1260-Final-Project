package tests.decaf;

class Animal {
  public String name() {
    return "anonymous";
  }
}


class Dog extends Animal {
  public String name() {
    return "dog";
  }
}

class Cat extends Animal {
  public String name() {
    return "cat";
  }
}

class Ape extends Animal {
  public String name() {
    return "ape";
  }
}

class Doberman extends Dog {
  public String name() {
    return "doberman";
  }
}

class Orangutan extends Ape {
  public String name() {
    return "orangutan";
  }
}

public class Animals {
  public static void printName(Animal x) {
    System.out.print(x.name());
    System.out.print("\n");
  }

  public static void main(String[] argv) {
    Orangutan o = new Orangutan();
    Doberman d = new Doberman ();
    Cat c = new Cat();
    Dog e = new Dog();

    printName(o);
    printName(d);
    printName(c);
    printName(e);
  }
}
