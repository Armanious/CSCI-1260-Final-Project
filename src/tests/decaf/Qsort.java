package tests.decaf;

import java.util.Scanner;


class NodeQsort {
  public int data;
  public NodeQsort next;
  public NodeQsort(int a, NodeQsort n) {
    data = a;
    next = n;
  }
}


class MultipleNodesQsort {
 public   NodeQsort high;
 public   NodeQsort equal;
 public   NodeQsort low;

 public MultipleNodesQsort(NodeQsort h, NodeQsort e, NodeQsort l) {
   high = h;
   equal = e;
   low = l;
 }
}


public class Qsort {
  
  public static NodeQsort Append(NodeQsort in, NodeQsort x) {
    if (in == null) 
      return x;
    in.next = Append(in.next, x);
    return in;
  }      

  public static MultipleNodesQsort split3Ways(NodeQsort l, int pivot)
    {
	  MultipleNodesQsort result;   
      if (l == null)
	return new MultipleNodesQsort(null, null, null);
      NodeQsort rst;
      NodeQsort curr;
      rst = l.next;
      curr = l;
      curr.next= null;

      result = split3Ways(rst, pivot);

      if (curr.data == pivot) 
	result.equal = Append(result.equal, curr);
      else if (curr.data < pivot)
	result.low = Append(result.low, curr);	
      else
	result.high = Append(result.high, curr);
      return result;
    }


  static NodeQsort qsort(NodeQsort list) {
	  MultipleNodesQsort result;
    if (list == null)
      return null;
    result = split3Ways(list, list.data);
    result.low = qsort(result.low);
    result.high = qsort(result.high);
    return Append(result.low, Append(result.equal, result.high));
  }
    
    static public void dump(NodeQsort n) {

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

  public static void main(String[] argv) {
	  NodeQsort L;
    L = null;

    Scanner s = new Scanner(System.in);
    
    int n = s.nextInt();
    while (n >= 0) {
    	NodeQsort l = new NodeQsort(n, null);
      L = Append(L, l);
      n = s.nextInt();
    }
    s.close();
    
    System.out.print("Input data\n");
   dump(L);

    L = qsort(L);

    System.out.print("Output data\n");
    dump(L);
  }
}
      

 

