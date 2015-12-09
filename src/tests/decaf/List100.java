package tests.decaf;

import java.util.Scanner;

class list {

class Node {
  public int data;
  public Node next;
  public Node(int a, Node n) {
    data = a;
    next = n;
  }

}
	
  private Node myNode;

  public void AddFirst(int a) {
      myNode = new Node(a, myNode);
    }

  public void dump() {
     Node n = myNode;
     System.out.print("The list contains:\n");
     if (n == null) {
       System.out.print("Nothing\n");
       return;
     }
     else  {
       System.out.print("[");
     }
     while (true) {
       System.out.print(n.data);
       n = n.next;
       if (n == null)  {
         System.out.print("]\n");
         return;
       }
       else
         System.out.print(", ");
      }
  }

  public  list() {
    myNode = null;
  }

  private Node AddLastPrime(int a, Node n) {
    if (n == null)
      return new Node(a, null);
    if (n.next == null) {
      n.next = new Node(a, null);
      return n;
    }
    AddLastPrime(a, n.next);
    return n;
  }

  public void AddLast (int a) {
    Node temp;
    myNode = AddLastPrime(a, myNode);
  }

  public boolean isEmpty () {
    return (myNode == null);
  }

  private  int lengthPrime(Node n) {
    if (n == null) return 0;
    return 1+lengthPrime(n.next);
  }

  public int length() {
    return lengthPrime(myNode);
  }

  private Node deletePrime(int a, Node n) {
    if (n == null)
      return null;
    else if (a == n.data)
      return n.next;
    return deletePrime(a, n.next);
  }

  public void delete(int a) {
    myNode = deletePrime(a, myNode);
  }

  private boolean memberPrime(Node n, int a) {
    if (n == null)
      return false;
    if (n.data == a)
      return true;
    return memberPrime(n.next, a);
  }

  public boolean member(int a) {
    return memberPrime(myNode, a);
  }
}


public class List100 {
  static void main(String[] argv) {
    list L = new list();

    Scanner s = new Scanner(System.in);
    
    int n = s.nextInt();
    while (n >= 0) {
      if (n <100)
	L.AddFirst(n);
      else
        L.AddLast(n);
      n = s.nextInt();
    }
    s.close();
    L.dump();
  }
}


