package org.armanious.csci1260.temporaries;

import java.util.ArrayList;
import java.util.HashMap;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

public class ObjectInstanceTemporary extends Temporary {

	private static HashMap<Object, Boolean> isDupped = new HashMap<>();

	private final Object fakeValue;

	private ObjectInstanceTemporary(AbstractInsnNode decl, Type type, Object val){
		super(decl, type);
		this.fakeValue = val;
		isDupped.put(fakeValue, false);
	}

	public ObjectInstanceTemporary(AbstractInsnNode decl, Type type){
		this(decl, type, new Object());
	}

	@Override
	public int getConstancyInternal() {
		return CONSTANT;
	}

	public boolean isDupped(){
		return isDupped.get(fakeValue);
	}

	public void setIsDupped(boolean newIsDupped){
		isDupped.put(fakeValue, newIsDupped);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof ObjectInstanceTemporary && (((ObjectInstanceTemporary)o).fakeValue == fakeValue);
	}

	public String toString(){
		return type.toString() + "@" + fakeValue.hashCode();
	}

	@Override
	protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
		list.add(getDeclaration());
	}

	@Override
	protected Temporary clone() {
		return new ObjectInstanceTemporary(getDeclaration(), type, fakeValue);
	}

}