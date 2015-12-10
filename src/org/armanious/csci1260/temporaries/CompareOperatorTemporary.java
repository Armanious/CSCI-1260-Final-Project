package org.armanious.csci1260.temporaries;

import java.util.ArrayList;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class CompareOperatorTemporary extends Temporary {

	public final Temporary rhs;
	public final Temporary lhs;
	public final int opcode;

	public CompareOperatorTemporary(AbstractInsnNode decl, Temporary lhs, Temporary rhs, int opcode) {
		super(decl, Type.INT_TYPE);
		this.rhs = rhs;
		this.lhs = lhs;
		this.opcode = opcode;
		if(rhs != null){
			rhs.addReference(decl, null);
		}
		lhs.addReference(decl, null);
	}

	public int getConstancyInternal() {
		return rhs == null ? lhs.getConstancy() : mergeConstancy(rhs, lhs);
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(!(o instanceof CompareOperatorTemporary)) return false;
		CompareOperatorTemporary cot = (CompareOperatorTemporary) o;
		return opcode == cot.opcode && rhs.equals(cot.rhs) && 
				(lhs == null ? cot.lhs == null : lhs.equals(cot.lhs));
	}

	public String toString(){
		return rhs == null ? ("CMP(" + lhs.toString() + ")") : (lhs.toString() + " CMP(" + opcode + ") " + rhs.toString());
	}

	@Override
	protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
		if(!(getDeclaration() instanceof VarInsnNode)){
			lhs.addRelevantInstructionsToListSorted(list);
		if(rhs != null){
			rhs.addRelevantInstructionsToListSorted(list);
		}
		}
		list.add(getDeclaration());
	}

	@Override
	protected void addCriticalTemporariesToList(ArrayList<Temporary> list) {
		list.add(lhs);
		//lhs.addCriticalTemporariesToList(list);
		if(rhs != null){
			list.add(rhs);
		}
		//rhs.addCriticalTemporariesToList(list);
	}

	@Override
	protected Temporary clone() {
		return new CompareOperatorTemporary(getDeclaration(), lhs, rhs, opcode);
	}

}