package tests.decaf;

import java.util.Scanner;

class IntStack {
  private int top;
  private int max;
  private int members[];

  public IntStack(int size)
  {
    max = size;
    top = -1;
    members = new int[max];
  }

  public int top()
  {
    if (top >= 0) return members[top];
    return -1;
  }

  public boolean isEmpty()
  {
    return top == -1;
  }

  public int pop()
  {
    int t = top();
    if (top >= 0) top = top - 1;
    return t;
  }

  public void push(int f)
  {
    if (top < max-1) {
      top = top + 1;
      members[top] = f;
    }
  }

  public boolean isFull()
  {
    return top >= max-1;
  }
}


public class Calculator
{
  static boolean isDigit(char c)
  {
    return '0' <= c && c <= '9';
  }

  static int digitToInt(char c)
  {
    return c - '0';
  }

  public static void main(String[] argv)
  {
    IntStack S = new IntStack(100);
    int sign = 1;
    final Scanner s = new Scanner(System.in);
    String input = s.nextLine();
    s.close();
    System.out.print(input);
    char[] cArr = input.toCharArray();
    for(int i = 0; i < cArr.length; i++) {
    	char c = cArr[i];
      if (c == '+') {
        S.push(S.pop() + S.pop());

      } else if (c == '-') {
        S.push(S.pop() - S.pop());

      } else if (c == '*') {
        S.push(S.pop() * S.pop());

      } else if (c == '/') {
        S.push(S.pop() / S.pop());

      } else if (c == '\n' || c == -1) {
        System.out.print(S.pop());
        System.out.print("\n");
        break;

      } else if (c == '~') {
        sign = sign * -1;      

      } else if (isDigit(c)) {
        int num = digitToInt(c);

        while (++i < cArr.length && isDigit(c = cArr[i])) {
          num = 10 * num + digitToInt(c);
        }
        i--;
        S.push(sign * num);
        sign = 1;

      } else if (c == ' ' || c == '\t') {

      } else {
        System.out.print("\nillegal character: '");
        System.out.print((char)c);
        System.out.print("'\n");
      }
    }
    s.close();
  }
}