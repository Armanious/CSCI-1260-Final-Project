package org.armanious.csci1260.temporaries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

public abstract class Temporary {

	public static int numTemporaries = 0;
	public static List<Temporary> GLOBAL_TEMPORARIES = Collections.synchronizedList(new ArrayList<Temporary>());

	public static final int NOT_CONSTANT = -1;
	public static final int CONSTANCY_UNKNOWN = 0;
	public static final int CONSTANT = 1;

	private Temporary parentTemporary = null;

	public final int index;
	public final Type type;

	private AbstractInsnNode declaration;

	public Map<AbstractInsnNode, MethodNode> references; 

	private boolean overrideConstancy = false;
	private int forcedConstancy = Integer.MAX_VALUE;

	public Temporary(AbstractInsnNode declaration, Type type){
		this.declaration = declaration;
		this.type = type;
		index = numTemporaries++;
		GLOBAL_TEMPORARIES.add(this);
	}

	int getConstancyInternal(){
		return CONSTANCY_UNKNOWN;
	}

	public final int getConstancy(){
		return parentTemporary != null ? parentTemporary.getConstancy() : (overrideConstancy ? forcedConstancy : getConstancyInternal());
	}

	public final void forceConstancy(int forcedConstancy){
		if(parentTemporary == null){
			overrideConstancy = true;
			this.forcedConstancy = forcedConstancy;
		}else{
			parentTemporary.forceConstancy(forcedConstancy);
		}
	}

	protected static int mergeConstancy(Temporary...others){
		int constancy = others[0].getConstancy();
		if(constancy == NOT_CONSTANT){
			return constancy;
		}
		for(int i = 1; i < others.length; i++){
			int otherConstancy = others[i].getConstancy(); //can be an expensive operation
			if(otherConstancy < constancy){
				constancy = otherConstancy;
				if(constancy == NOT_CONSTANT){
					break;
				}
			}
		}
		return constancy;
	}

	public AbstractInsnNode getDeclaration(){
		return declaration;
	}

	protected abstract void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list);

	protected void addCriticalTemporariesToList(ArrayList<Temporary> list){

	}

	public ArrayList<Temporary> getCriticalTemporaries(){
		ArrayList<Temporary> t = new ArrayList<Temporary>();
		addCriticalTemporariesToList(t);
		t.sort((t1,t2) -> {
			return (t1.getDeclaration() == null ? Integer.MAX_VALUE : t1.getDeclaration().getIndex())
					- (t2.getDeclaration() == null ? Integer.MAX_VALUE : t2.getDeclaration().getIndex());
		});
		return t;
	}

	protected abstract Temporary clone();

	public final Temporary cloneOnInstruction(AbstractInsnNode ain){
		Temporary t = clone();
		t.parentTemporary = parentTemporary == null ? this : parentTemporary;
		t.declaration = ain;
		return t;
	}

	public final ArrayList<AbstractInsnNode> getContiguousBlockSorted(){
		final ArrayList<AbstractInsnNode> list = new ArrayList<>();
		if(getDeclaration() != null && getDeclaration().getOpcode() >= Opcodes.ILOAD && getDeclaration().getOpcode() <= Opcodes.ALOAD){
			//cloned instruction with only a load; i.e. we are stored in a local variable
			//we don't need to go through the other checks
			list.add(getDeclaration());
			return list;
		}
		addRelevantInstructionsToListSorted(list);
		if(list.contains(null)){
			return null;
		}
		//list.sort((a1, a2) -> a1.getIndex() - a2.getIndex());
		int fingerIndex = 0;
		for(AbstractInsnNode ain = list.get(0); ain != null && ain != list.get(list.size() - 1).getNext(); ain = ain.getNext()){
			if(list.get(fingerIndex) != ain){
				//somethings interupting the list; only special cases allowed:
				switch(ain.getOpcode()){ //the one interrupting the list
				case Opcodes.DUP:
				case Opcodes.DUP2:
				case Opcodes.DUP2_X1:
				case Opcodes.DUP2_X2:
				case Opcodes.DUP_X1:
				case Opcodes.DUP_X2:
				case Opcodes.SWAP:
				case Opcodes.POP:
				case Opcodes.POP2:
					continue;
				default:
					return null;
				}
			}else{
				fingerIndex++; //match, increment in list
			}
		}
		return list;
	}

	public void addReference(AbstractInsnNode insn, MethodNode mn){
		if(references == null){
			references = new HashMap<>();
		}
		references.put(insn, mn);
	}

	public final Type getType(){
		return type;
	}

	public abstract boolean equals(Object o);

}