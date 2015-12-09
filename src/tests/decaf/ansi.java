package tests.decaf;

public class ansi {
    //private static IOClass IO = new IOClass();

    private static void esc() {
        System.out.print((char)27);
        System.out.print('[');
    }

    private static void putchars(int n) {
        int tens = (n / 10) % 10;
        int ones = n % 10;
        System.out.print((char)(tens + 48));
        System.out.print((char)(ones + 48));
    }

    public static void cls() {
        esc();
        System.out.print('2');
        System.out.print('J');
    }

    public static void position(int row, int col) {
        esc();
        putchars(row);
       	System.out.print(';');
        putchars(col);
        System.out.print('H');
    }

    public static void home() {
        esc();
        System.out.print('H');
    }

    
    public static void setattrs(int a, int fg, int bg) {
        int i;
        esc();
        System.out.print('0');
        System.out.print('m');
        esc();
        putchars(a);
        System.out.print(';');
        putchars(fg);
        System.out.print(';');
        putchars(bg);
        System.out.print('m');
    }

    public static void up(int n) {
        esc();
        putchars(n);
        System.out.print('A');
    }

    public static void down(int n) {
        esc();
        putchars(n);
        System.out.print('B');
    }

    public static void right(int n) {
        esc();
        putchars(n);
        System.out.print('C');
    }

    public static void left(int n) {
        esc();
        putchars(n);
        System.out.print('D');
    }

    public static void main(String[] args) {
        cls();

        setattrs(1, 34, 40);
        home();
        System.out.print("Hello world!");
        up(1);
        right(55);
        System.out.print("Hello world!");
        home();
        down(25);
        System.out.print("Hello world!");
        up(1);
        right(55);
        System.out.print("Hello world!");
        home();
        down(12);
        right(30);
        setattrs(1, 31, 40);
        System.out.print("World, hello!");
        home();
        down(26);
        setattrs(0, 39, 49);
    }
}

