package tests.decaf;

public class c {
   c x;

   public int[] y()	{ return new int[10]; }

   public void m () {
      int i = x.y()[1];
    }

   public int n() {
      if (true) { 
    	  return 0;
      }
      else {
	 return 1;
       }
    }

   public static void main(String[] args) { }

}
