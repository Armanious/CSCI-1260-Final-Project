package tests.decaf;

import java.util.Scanner;

class NodeSsort {
  public int data;
  public NodeSsort next;
  public NodeSsort(int a, NodeSsort n) {
    data = a;
    next = n;
  }
}


class MultipleNodeSsorts {
 public   NodeSsort high;
 public   NodeSsort equal;
 public   NodeSsort low;

 public MultipleNodeSsorts(NodeSsort h, NodeSsort e, NodeSsort l) {
   high = h;
   equal = e;
   low = l;
 }
}
 


public class Ssort {
  
  public static NodeSsort Append(NodeSsort in, NodeSsort x) {
    if (in == null) 
      return x;
    in.next = Append(in.next, x);
    return in;
  }      

  public static MultipleNodeSsorts split3Ways(NodeSsort l, int pivot)
    {
      MultipleNodeSsorts result;   
      if (l == null)
	return new MultipleNodeSsorts(null, null, null);
      NodeSsort rst;
      NodeSsort curr;
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

  static int least(NodeSsort list) {
    int rst;
    if (list.next == null)
      return list.data;

    rst = least(list.next);
    if (rst < list.data)
      return rst;
    return list.data;
  }

  static NodeSsort ssort(NodeSsort list) {
    MultipleNodeSsorts result;
    if (list == null) 
      return list;
    if (list.next == null)
      return list;
    result = split3Ways(list, least(list));
    result.high = ssort(result.high);
    return Append(result.equal, result.high);
  }
    
    static public void dump(NodeSsort n) {

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
    NodeSsort L;
    L = null;

    Scanner s = new Scanner(System.in);
    int n = s.nextInt();
    while (n >= 0) {
      NodeSsort l = new NodeSsort(n, null);
      L = Append(L, l);
      n = s.nextInt();
    }
    s.close();
    System.out.print("Input data\n");
   dump(L);

    L = ssort(L);

    System.out.print("Output data\n");
    dump(L);
  }
}
      

 

