package tests.decaf;

public class Pascal {
  public static int[][] makePascalTable(int n1, int n2)
  {
    int[][] tab = new int[n1][n2];

    int i = 0;
    while (i < n1) {
      int j = 0;
      while (j < n2) {
        if (i == 0 || j == 0)
          tab[i][j] = 1;
        else
          tab[i][j] = tab[i-1][j] + tab[i][j-1];
        j = j + 1;
      }
      i = i + 1;
    }
    return tab;
  }

  static void printTable (int A[][])
  {
    int i = 0;
    while(i < A.length) {
      int j = 0;

      while (j < A[i].length) {
        System.out.print(A[i][j]);
        System.out.print("  ");
        j = j + 1;
      }
      System.out.print("\n");
      i = i + 1;
    }
  }

  public static void main(String[] argv)
  {
    int A[][] = makePascalTable(4, 8);
    printTable (A);
  }
}