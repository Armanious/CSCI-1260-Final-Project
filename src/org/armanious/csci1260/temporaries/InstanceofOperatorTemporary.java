package org.armanious.csci1260.temporaries;

import java.util.ArrayList;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class InstanceofOperatorTemporary extends Temporary {

	public final Temporary objectRef;
	public final Type toCheck;

	public InstanceofOperatorTemporary(AbstractInsnNode decl, Temporary objectRef, Type toCheck){
		super(decl, Type.INT_TYPE);
		this.objectRef = objectRef;
		this.toCheck = toCheck;
		objectRef.addReference(decl, null);
	}

	public int getConstancyInternal() {
		return objectRef.getConstancy();
	};

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(!(o instanceof InstanceofOperatorTemporary))	return false;
		InstanceofOperatorTemporary iot = (InstanceofOperatorTemporary) o;
		return toCheck.equals(iot.toCheck) && objectRef.equals(iot.objectRef);
	}

	public String toString(){
		return objectRef.toString() + " instanceof " + toCheck.toString();
	}

	@Override
	protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
		if(!(getDeclaration() instanceof VarInsnNode)){
			objectRef.addRelevantInstructionsToListSorted(list);
		}
		list.add(getDeclaration());
	}

	@Override
	protected void addCriticalTemporariesToList(ArrayList<Temporary> list) {
		list.add(objectRef);
		//objectRef.addCriticalTemporariesToList(list);
	}

	@Override
	protected Temporary clone() {
		return new InstanceofOperatorTemporary(getDeclaration(), objectRef, toCheck);
	}

}