package org.armanious.csci1260.temporaries;

import java.util.ArrayList;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

public class ConstantTemporary extends Temporary {

	Object value;

	public ConstantTemporary(AbstractInsnNode decl, Object value, Type type){
		super(decl, type);
		this.value = value;
	}

	public void setValue(Object newValue){
		if(!(getDeclaration() instanceof LdcInsnNode)){
			System.err.println("Warning: cannot set value of cloned ConstantTemporary.");
			return;
		}
		((LdcInsnNode)getDeclaration()).cst = newValue;
		value = newValue;
	}

	@Override
	public int getConstancyInternal() {
		return CONSTANT;
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(!(o instanceof ConstantTemporary)) return false;
		Object otherValue = ((ConstantTemporary)o).value;
		if(value == otherValue) return true;
		if(value == null || otherValue == null) return false;
		return value.equals(otherValue);
	}

	public String toString(){
		if(value instanceof String){
			return "\"" + ((String)value) + "\"";
		}
		return value == null ? "ConstantNull" : String.valueOf(value);
	}

	public Object getValue(){
		return value;
	}

	@Override
	protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
		list.add(getDeclaration());
	}

	@Override
	protected Temporary clone() {
		return new ConstantTemporary(getDeclaration(), value, type);
	}

}