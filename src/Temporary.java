

public class Temporary {
	
	public static enum Source {
		LOCAL_STACK, INSTANCED_FIELD, STATIC_FIELD, CONSTANT
	}
	
	public static enum Type {
		INT, FLOAT, LONG, DOUBLE, BOOL, OBJECT, //load
		CONSTANT_INT, CONSTANT_FLOAT, CONSTANT_STRING, //ldc and ldc_w
		CONSTANT_DOUBLE, CONSTANT_LONG //ldc2_w
	}
	
	public final Type type;
	public final Object value;
	
	public int constantFlag = 0; //-1 is definitely NOT constant, 0 is undefined, 1 IS constant
	
	public Temporary(Type type, Object value){
		this.type = type;
		this.value = value;
	}

}
