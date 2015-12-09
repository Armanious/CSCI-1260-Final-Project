package tests.decaf;

/* This evaluate lambda-calculus expressions. */
/* But it doesn't compile: 
   cslab5c /u/jay/tmp/cs126 % /course/cs126/bin/decaf lc.decaf
Exception in thread "main" java.lang.NullPointerException
        at edu.brown.cs.cs126.decaf.spr.SprAsmGenerator.getAsmStore(SprAsmGenerator.java:771)
        at edu.brown.cs.cs126.decaf.spr.SprAsmGenerator.asmStore(SprAsmGenerator.java:508)
        at edu.brown.cs.cs126.decaf.spr.SprAsmGenerator.generateBlock(SprAsmGenerator.java:231)
        at edu.brown.cs.cs126.decaf.spr.SprAsmGenerator.generateRoutine(SprAsmGenerator.java:138)
        at edu.brown.cs.cs126.decaf.spr.SprAsmGenerator.generateCode(SprAsmGenerator.java:79)
        at edu.brown.cs.cs126.decaf.DecafMain.processCodeGeneration(DecafMain.java:351)
        at edu.brown.cs.cs126.decaf.DecafMain.process(DecafMain.java:182)
        at edu.brown.cs.cs126.decaf.DecafMain.main(DecafMain.java:32)
*/

class Context
{
    boolean isEmpty ()
    {
	return true;
    }

    boolean contains ( char abinding )
    {
	return false;
    }

    Expression valueOf () 
    {
	return new Expression();
    }

    Context cdr ()
    {
	return this;
    }
}

class Binding extends Context
{
    private char binding;
    private Expression value;
    private Context r;

    public Binding ( char b, Expression v, Context c )
    {
	binding = b;
	value = v;
	r = c;
    }

    boolean isEmpty ()
    {
	return false;
    }

    boolean contains ( char ab )
    {
	return (ab == binding);
    }

    Expression valueOf ()
    {
	return value;
    }

    Context cdr ()
    {
	return r;
    }
}
	
class Expression
{
    Expression eval ( Context c )
    {
	return this;
    }

    Expression apply ( Expression v, Context bc )
    {
	return (this.eval(bc)).apply( v, bc );
    }

    void print () 
    {
	System.out.print( "STUCK" );
    }
}

class Variable extends Expression
{
    private char binding;

    public Variable ( char b )
    {
	binding = b;
    }

    Expression eval ( Context c )
    {
	if ( c.isEmpty() ) {
	    return new Expression();
	} else {
	    if ( c.contains( binding ) ) {
		return c.valueOf();
	    } else {
		return this.eval( c.cdr() );
	    }
	}
    }

    void print ()
    {
	System.out.print((char) binding );
    }
}

class Abstraction extends Expression
{
    public char binding;
    private Expression body;

    public Abstraction ( char b0, Expression b1 )
    {
	binding = b0;
	body = b1;
    }
    
    Expression apply ( Expression v, Context bc )
    {
	return v.eval( new Binding( binding, v, bc ) );
    }

    void print ()
    {
	System.out.print( "\\ " );
	System.out.print((char)binding );
	System.out.print( " (" );
	body.print();
	System.out.print( ")" );
    }
}

class Application extends Expression
{
    private Expression fp;
    private Expression ap;

    public Application ( Expression f, Expression a )
    {
	fp = f;
	ap = a;
    }

    Expression eval ( Context c )
    {
	return fp.apply( ap.eval(c), c );
    }

    void print ()
    {
	System.out.print( '(' );
	fp.print();
	System.out.print( ' ' );
	ap.print();
	System.out.print( ')' );
    }
}

public class lc
{
    public static void main ( String [] argv )
    {
	Abstraction Zero = new Abstraction( 'f', new Abstraction( 'x', new Variable( 'x' ) ) );
	Abstraction Succ = new Abstraction( 'n', new Abstraction( 'f', new Abstraction( 'x', new Application( new Variable('f'), new Application( new Application( new Variable('n'), new Variable('f')), new Variable('x'))))));

	new Application( Succ, Zero ).eval( new Context() ).print();
    }
}