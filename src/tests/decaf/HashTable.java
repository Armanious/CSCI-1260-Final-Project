package tests.decaf;

/************************************************************************/
/*                                                                      */
/*              HashTable.decaf                                         */
/*                                                                      */
/*      Generic chained hash table                                      */
/*                                                                      */
/************************************************************************/


class Integer {
    private int i;
    public Integer(int n) {
        i = n;
    }
}

/************************************************************************/
/*                                                                      */
/*      Generic linked list                                             */
/*                                                                      */
/*      add(), remove(), contains()                                     */
/*                                                                      */
/************************************************************************/

class ListElement {
    public Object elem;
    public Object next;

    public ListElement(Object o) {
        elem = o;
        next = null;
    }
}



class LinkedList {
    private ListElement first;


    /*  findPred() -- find predecessor node of specified element
     */
    private ListElement findPred(Object o) {
        ListElement pred, curr;
        
        pred = first;
        curr = (ListElement) first.next;

        while (curr != null) {
            if (curr.elem == o)
                return pred;
            pred = curr;
            curr = (ListElement) curr.next;
        }
        return null;
    }


    /*  LinkedList() -- constructor
     */
    public LinkedList() {
        first = new ListElement(null);
    }


    /*  add() -- insert specified object at head of list
     */
    public boolean add(Object o) {
        ListElement e = new ListElement(o);
        e.next = first.next;
        first.next = e;
        return true;
    }


    /*  remove() -- remove first of specified object from list
     */
    public boolean remove(Object o) {
        ListElement pred = findPred(o);
        if (pred == null)
            return false;

        ListElement curr = (ListElement) pred.next;
        pred.next = curr.next;
        return true;
    }


    /*  contains() -- return true if specified object is in list
     */
    public boolean contains(Object o) {
        ListElement pred = findPred(o);
        if (pred == null)
            return false;
        return true;
    }
}


/************************************************************************/
/*                                                                      */
/*      Hash table                                                      */
/*                                                                      */
/************************************************************************/

public class HashTable {
    private LinkedList[] buckets;
    private int numBuckets;


    /*  hash() -- compute hash of specified object (uses reference)
     */
    private int hash(Object o) {
        //int k = o.hashCode();
        int k = o.hashCode();
        return k % numBuckets;
    }


    /*  HashTable() -- constructor
     */
    public HashTable(int num) {
        int i = 0;
        numBuckets = num;
        buckets = new LinkedList[num];
        while (i < numBuckets) {
            buckets[i] = new LinkedList();
            i = i + 1;
        }
    }


    /*  add() -- add specified object to table
     */
    public boolean add(Object o) {
        int h = hash(o);
        return buckets[h].add(o);
    }


    /*  remove() -- remove first of specified object from table
     */
    public boolean remove(Object o) {
        int h = hash(o);
        return buckets[h].remove(o);
    }


    /*  contains() -- returns true if specified object is in table
     */
    public boolean contains(Object o) {
        int h = hash(o);
        return buckets[h].contains(o);
    }


    /*  main() -- runs tests on the hash table implementation
     */
    public static void main(String[] args) {
        int i = 0;
        Integer n, p = new Integer(-1);
        Object o;
        HashTable ht;
        //IOClass IO = new IOClass();

        // create and initialize hash table
        ht = new HashTable(100);
        while (i < 50) {
            n = new Integer(i);
            ht.add(n);
            if (i == 42)
                p = n;
            i = i + 1;
        }

        // the following should not be in the table
        n = new Integer(99);
        if (ht.contains(n))
            System.out.print("Table contains non-inserted element!");

        // this should be in the table
        if (!ht.contains(p)) {
            System.out.print("Table does not contain inserted element!");
            return;
        }

        // remove element from table and try again
        ht.remove(p);
        if (ht.contains(p)) {
            System.out.print("Table contains removed element!");
            return;
        }

        // done
        System.out.print("Tests pass.");
    }
}

