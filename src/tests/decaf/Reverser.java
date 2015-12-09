package tests.decaf;

import java.io.IOException;
import java.util.Scanner;

class CharNode {

   public CharNode (char c, CharNode x)
   {
     value = c;
     next = x;
   }

   public char value() { return value; }
   public CharNode next() { return next; }
   public boolean isNext() { return next != null; }
   public int length()
   {
      if (!isNext()) return 1;

      return 1 + next().length();
   }

   private char value;
   private CharNode next;
}

public class Reverser {

   public static CharNode readCharList () throws IOException
   {
     CharNode head = null;
     Scanner s = new Scanner(System.in);
     String string = s.next();
     s.close();
     for(char c : string.toCharArray()) {
       head = new CharNode (c, head);
     }
     return head;
   }
   
   public static void printCharList (CharNode l)
   {
     while (l != null) {
       System.out.print((char)l.value());
       l = l.next();
     }
   }
   
   public static void main(String argv[]) throws IOException
   {
     CharNode l = readCharList ();
     printCharList(l);
   }
}

