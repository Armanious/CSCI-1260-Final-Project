package org.armanious.csci1260.temporaries;

import java.util.ArrayList;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class NegateOperatorTemporary extends Temporary {

	private final Temporary tmp;

	public NegateOperatorTemporary(AbstractInsnNode decl, Temporary tmp){
		super(decl, tmp.getType());
		this.tmp = tmp;
		tmp.addReference(decl, null);
	}

	@Override
	public int getConstancyInternal() {
		return tmp.getConstancy();
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(!(o instanceof NegateOperatorTemporary)) return false;
		NegateOperatorTemporary not = (NegateOperatorTemporary) o;
		return tmp.equals(not.tmp);
	}

	public String toString(){
		return "-" + tmp.toString();
	}

	public Temporary getOperand(){
		return tmp;
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
		return new NegateOperatorTemporary(getDeclaration(), tmp);
	}

}