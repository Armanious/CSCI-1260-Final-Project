package org.armanious.csci1260.temporaries;

import java.util.ArrayList;

import org.armanious.csci1260.DataManager;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class BinaryOperatorTemporary extends Temporary {

	private static final String[] TEXT_REP = {"+","-","*","/","%","<<",
			">>",">>>","&","|","^"};

	public static final int ADD = 0;
	public static final int SUB = 1;
	public static final int MUL = 2;
	public static final int DIV = 3;
	public static final int REM = 4;
	public static final int SHL = 5;
	public static final int SHR = 6;
	public static final int USHR = 7;
	public static final int AND = 8;
	public static final int OR = 9;
	public static final int XOR = 10;

	private final Temporary rhs;
	private final Temporary lhs;
	private final int arithmeticType;


	private static Type determineType(Temporary lhst, Temporary rhst){
		Type lhs = lhst.getType();
		Type rhs = rhst.getType();
		if(lhs == rhs){
			return lhs;
		}else if(lhs == Type.BYTE_TYPE || lhs == Type.SHORT_TYPE || lhs == Type.CHAR_TYPE || lhs == Type.BOOLEAN_TYPE){
			return rhs;
		}else if(rhs == Type.BYTE_TYPE || rhs == Type.SHORT_TYPE || rhs == Type.CHAR_TYPE || rhs == Type.BOOLEAN_TYPE){
			return lhs;
		}else if(lhs == Type.LONG_TYPE && rhs == Type.INT_TYPE){
			//LSHL, LSHR, LUSHR
			return rhs;
		}
		lhst.getType();
		((PhiTemporary)lhst).debugType();
		rhst.getType();
		new RuntimeException(lhs + ", " + rhs).printStackTrace();
		throw new RuntimeException();
	}

	public BinaryOperatorTemporary(AbstractInsnNode decl, Temporary rhs, Temporary lhs, int arithmeticType) {
		super(decl, determineType(lhs, rhs));
		this.rhs = rhs;
		this.lhs = lhs;
		this.arithmeticType = arithmeticType;

		rhs.addReference(decl, null);
		lhs.addReference(decl, null);
	}

	public int getArithmeticType(){
		return arithmeticType;
	}

	public Temporary getLeftHandSideTemporary(){
		return lhs;
	}

	public Temporary getRightHandSideTemporary(){
		return rhs;
	}

	@Override
	public int getConstancyInternal() {
		return mergeConstancy(rhs, lhs);
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(!(o instanceof BinaryOperatorTemporary)) return false;
		BinaryOperatorTemporary at = (BinaryOperatorTemporary) o;
		if(rhs.equals(at.rhs) && lhs.equals(at.lhs)) return true;
		if(arithmeticType == ADD || arithmeticType == MUL){
			if(rhs.equals(at.lhs) && lhs.equals(at.rhs)){
				return true;
			}
		}
		return false;
	}

	public String toString(){
		return "(" + lhs.toString() + TEXT_REP[arithmeticType] + rhs.toString() + ")";
	}

	@Override
	protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
		if(!(getDeclaration() instanceof VarInsnNode)){
			lhs.addRelevantInstructionsToListSorted(list);
		
		rhs.addRelevantInstructionsToListSorted(list);
		}
		list.add(getDeclaration());
	}

	@Override
	protected void addCriticalTemporariesToList(ArrayList<Temporary> list) {
		list.add(lhs);
		//lhs.addCriticalTemporariesToList(list);
		list.add(rhs);
		//rhs.addCriticalTemporariesToList(list);
	}

	@Override
	protected Temporary clone() {
		return new BinaryOperatorTemporary(getDeclaration(), rhs, lhs, arithmeticType);
	}

}