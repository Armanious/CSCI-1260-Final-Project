package org.armanious.csci1260.temporaries;

import java.util.ArrayList;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class ParameterTemporary extends Temporary {

	private final MethodNode owner;
	private final int index;

	private ParameterTemporary(AbstractInsnNode varInsnNode, MethodNode owner, int index, Type type){
		super(varInsnNode, type);
		this.owner = owner;
		this.index = index;
	}

	public ParameterTemporary(MethodNode owner, int index, Type type){
		this(null, owner, index, type);
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(!(o instanceof ParameterTemporary)) return false;
		ParameterTemporary pt = (ParameterTemporary) o;
		return pt.owner == owner && pt.index == index;
	}

	public String toString(){
		return index == -1 ? "this" : ("local_" + index);//(type.toString() + " (Param " + index + ")");
	}

	@Override
	protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
		list.add(getDeclaration());
	}

	@Override
	protected Temporary clone() {
		return new ParameterTemporary(owner, index, type);
	}

}