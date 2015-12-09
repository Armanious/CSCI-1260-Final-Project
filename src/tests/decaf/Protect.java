package tests.decaf;

class One{
   protected int f1;
}

class Two{
   public void test(){
      One n1=new One();
      n1.f1=5;
    }
}

public class Protect{
   public static void main(String[] argv){
      Object[] x=argv;
      //return 0;
    }
}
