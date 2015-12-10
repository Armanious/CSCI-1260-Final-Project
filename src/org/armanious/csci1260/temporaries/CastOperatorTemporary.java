package org.armanious.csci1260.temporaries;

import java.util.ArrayList;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class CastOperatorTemporary extends Temporary {

	private static Type getResultType(int opcode){
		switch(opcode){
		case Opcodes.L2I:
		case Opcodes.F2I:
		case Opcodes.D2I:
			return Type.INT_TYPE;
		case Opcodes.I2L:
		case Opcodes.F2L:
		case Opcodes.D2L:
			return Type.LONG_TYPE;
		case Opcodes.I2F:
		case Opcodes.L2F:
		case Opcodes.D2F:
			return Type.FLOAT_TYPE;
		case Opcodes.I2D:
		case Opcodes.L2D:
		case Opcodes.F2D:
			return Type.DOUBLE_TYPE;
		case Opcodes.I2B:
			return Type.BYTE_TYPE;
		case Opcodes.I2C:
			return Type.CHAR_TYPE;
		case Opcodes.I2S:
			return Type.SHORT_TYPE;
		}
		throw new IllegalArgumentException("Cast opcode: " + opcode);
	}

	private final Temporary tmp;
	private final int opcode;

	public CastOperatorTemporary(AbstractInsnNode decl, Temporary tmp, int opcode){
		super(decl, getResultType(opcode));
		this.tmp = tmp;
		this.opcode = opcode;
		tmp.addReference(decl, null);
	}

	public Temporary getOperand(){
		return tmp;
	}

	@Override
	public int getConstancyInternal() {
		return tmp.getConstancy();
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(!(o instanceof CastOperatorTemporary)) return false;
		CastOperatorTemporary ct = (CastOperatorTemporary) o;
		return getType().equals(ct.getType()) && tmp.equals(ct.tmp);
	}

	public String toString(){
		return "((" + getType().toString() + ") " + tmp.toString() + ")";
	}

	@Override
	protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
		if(!(getDeclaration() instanceof VarInsnNode)){
			tmp.addRelevantInstructionsToListSorted(list);
		}
		list.add(getDeclaration());
	}

	@Override
	protected void addCriticalTemporariesToList(ArrayList<Temporary> list) {
		list.add(tmp);
		//tmp.addCriticalTemporariesToList(list);
	}

	@Override
	protected Temporary clone() {
		return new CastOperatorTemporary(getDeclaration(), tmp, opcode);
	}

}