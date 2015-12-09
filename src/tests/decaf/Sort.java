package tests.decaf;

import java.util.Scanner;

public class Sort {
  static public void swap (int[] A, int i, int j)
  {
    int tmp = A[i];
    A[i] = A[j];
    A[j] = tmp;
  }

  static public void sort (int[] A)
  {
    int i = 0;
    while (i < A.length) {
      int j = i+1;
      while (j < A.length) {
        if (A[j] < A[i]) {
          swap(A, i, j);
        }
        j = j + 1;
      }
      i = i + 1;
    }
  }

  public static int min(int x, int y)
  {
    if (x < y) return x;
    return y;
  }

  public static int[] resize(int[] A, int oldsz, int newsz)
  {
    int [] A2 = new int[newsz];
    int i = 0;
    while (i < min(oldsz, newsz)) {
      A2[i] = A[i];
      i = i + 1;
    }
    return A2;
  }

  public static int[] readInts ()
  {
    int max = 100;
    int[] A = new int[max];

    int i = 0;
    Scanner s = new Scanner(System.in);
    while (true) {
      int x = s.nextInt();
      if (x < 0)
        break;
      if (i >= max) {
        int newmax = max * 2;
        A = resize(A, max, newmax);
        max = newmax;
      }
      A[i] = x;
      i = i + 1;
    }
    s.close();
    return resize(A, max, i);
  }

  public static void printInts(int A[])
  {
    int i = 0;
    while(i < A.length) {
      System.out.print(A[i]);
      System.out.print("\n");
      i = i + 1;
    }
  }

  public static void main(String[] argv)
  {
    System.out.print("Enter some integers. Enter a negative value to terminate.\n");
    int[] A = readInts();
    if (A.length > 0) {
      sort(A);
      System.out.print("The sorted integers are:\n");
      printInts(A);
    } else {
      System.out.print("No non-negative values entered.\n");
    }
  }      
}