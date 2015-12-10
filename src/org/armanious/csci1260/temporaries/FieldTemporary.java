package org.armanious.csci1260.temporaries;

import java.util.ArrayList;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class FieldTemporary extends Temporary {

	private final FieldTemporary parent;
	private final boolean isConstant;
	private final Temporary objectRef;
	final String owner;
	final String name;

	private final Temporary value;
	private final boolean isVolatile;

	private int numWrites;

	//loading from LocalVariable from basic clone()
	private FieldTemporary(FieldTemporary parent, Temporary value, Type type, boolean isVolatile){
		super(null, type);
		this.parent = parent;
		isConstant = false;
		objectRef = null;
		owner = null;
		name = null;
		this.value = value;
		this.isVolatile = isVolatile;
	}

	private FieldTemporary(FieldTemporary parent, AbstractInsnNode insn, MethodNode mn, Temporary value, boolean isVolatile){
		super(insn, (insn.getOpcode() == Opcodes.GETFIELD || insn.getOpcode() == Opcodes.GETSTATIC) ? parent.getType() : Type.VOID_TYPE);
		this.parent = parent;
		isConstant = false;
		objectRef = null;
		owner = null;
		name = null;
		this.value = value;
		this.isVolatile = isVolatile;

		if(getType() == Type.VOID_TYPE){
			parent.numWrites++;
		}else{
			parent.addReference(insn, mn);
		}
	}

	public FieldTemporary(boolean isConstant, Temporary objectRef, String owner, String name, Type type, boolean isVolatile){
		super(null, type);
		this.parent = null;
		this.isConstant = isConstant;
		this.objectRef = objectRef;
		this.owner = owner;
		this.name = name;
		this.value = null;
		this.isVolatile = isVolatile;
	}

	public int getConstancyInternal() {
		return parent == null ? 
				(isVolatile ? NOT_CONSTANT : (isConstant ? CONSTANT : (numWrites <= 1 ? CONSTANT : NOT_CONSTANT))) :
					parent.getConstancy();
	}

	public String getOwner(){
		return parent == null ? owner : parent.getOwner();
	}

	public String getName(){
		return parent == null ? name : parent.getName();
	}

	public Temporary getObjectRef(){
		return parent == null ? objectRef : parent.getObjectRef();
	}

	public Temporary getValue(){
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(!(o instanceof FieldTemporary)) return false;
		FieldTemporary ft = (FieldTemporary) o;
		if(!getOwner().equals(ft.getOwner()) || !getName().equals(ft.getName())){
			return false;
		}
		if(getValue() != ft.getValue() && (getValue() == null || ft.getValue() == null || !getValue().equals(ft.getValue()))){
			return false;
		}
		return (getObjectRef() == null) ? (ft.getObjectRef() == null) : (getObjectRef().equals(ft.getObjectRef()));
	}

	public String toString(){
		return (getObjectRef() == null ? getOwner() : getObjectRef().toString()) + "." + getName() + (getType() == Type.VOID_TYPE ? (" = " + getValue()) : "");
	}

	@Override
	protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
		if(!(getDeclaration() instanceof VarInsnNode)){
			if(getObjectRef() != null){
				getObjectRef().addRelevantInstructionsToListSorted(list);
			}
			if(getValue() != null){
				getValue().addRelevantInstructionsToListSorted(list);
			}
		}
		list.add(getDeclaration());
	}

	@Override
	protected void addCriticalTemporariesToList(ArrayList<Temporary> list) {
		if(getValue() != null){
			list.add(getValue());
			//getValue().addCriticalTemporariesToList(list);
		}
		if(getObjectRef() != null){
			list.add(getObjectRef());
			//getObjectRef().addCriticalTemporariesToList(list);
		}
	}

	public boolean isVolatile() {
		return isVolatile;
	}

	@Override
	protected Temporary clone() {
		return new FieldTemporary(parent, value, getType(), isVolatile);
	}

	public FieldTemporary cloneSpecialCase(AbstractInsnNode ain, MethodNode mn, Temporary value){
		return new FieldTemporary(this, ain, mn, value, isVolatile);
	}

}