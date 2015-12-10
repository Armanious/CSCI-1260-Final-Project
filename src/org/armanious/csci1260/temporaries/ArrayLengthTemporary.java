package org.armanious.csci1260.temporaries;

import java.util.ArrayList;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ArrayLengthTemporary extends Temporary {

	public final Temporary arrayRef;

	public ArrayLengthTemporary(AbstractInsnNode decl, Temporary arrayRef){
		super(decl, Type.INT_TYPE);
		this.arrayRef = arrayRef;
		arrayRef.addReference(decl, null);
	}

	@Override
	public int getConstancyInternal() {
		return arrayRef.getConstancy();
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(!(o instanceof ArrayLengthTemporary))	return false;
		ArrayLengthTemporary alo = (ArrayLengthTemporary) o;
		return alo.arrayRef.equals(arrayRef);
	}

	public String toString(){
		return arrayRef.toString() + ".length";
	}

	@Override
	protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
		if(!(getDeclaration() instanceof VarInsnNode)){
			arrayRef.addRelevantInstructionsToListSorted(list);
		}
		list.add(getDeclaration());
	}

	@Override
	protected void addCriticalTemporariesToList(ArrayList<Temporary> list) {
		list.add(arrayRef);
		//arrayRef.addCriticalTemporariesToList(list);
	}

	@Override
	protected Temporary clone() {
		return new ArrayLengthTemporary(getDeclaration(), arrayRef);
	}

}