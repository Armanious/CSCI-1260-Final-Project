package tests.decaf;

public class Morgan {

public static void maxcol(int [][] a,int [] large,int [] value)
{
   int n = large.length;

   int i = 0;
   while (i < n) {
      large[i] = 1;
      value[i] = +a[1][i];
      int j = 1;
      while (j < n) {
	 if ( +a[j][i] > value[i]) {
	    value[i] = +a[j][i];
	    large[i] = j;
	  }
	 j = j+1;
       }
      i = i+1;
    }
}

static public void main(String [] argv)
{ }



}
