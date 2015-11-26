package org.armanious.csci1260;

import org.armanious.csci1260.DataManager.Temporary;
import org.objectweb.asm.tree.AbstractInsnNode;

public final class JavaStack {
	
	private static final int DEFAULT_SIZE = 8;

	private Temporary[] stack;
	private AbstractInsnNode[] definingInstructions;
	private int index = 0;

	private JavaStack(Temporary[] stack, AbstractInsnNode[] definingInstructions, int index){
		this.stack = stack;
		this.definingInstructions = definingInstructions;
		this.index = index;
	}

	public JavaStack(){
		//if(maxSize == 0){
			//System.err.println("Warning: creating stack with stack size of 0. Default to MAX_STACK_SIZE = " + MAX_STACK_SIZE);
			//maxSize = MAX_STACK_SIZE;
		//}
		stack = new Temporary[DEFAULT_SIZE];
		definingInstructions = new AbstractInsnNode[DEFAULT_SIZE];
	}
	
	private void ensureCapacity(int newSize){
		if(newSize >= stack.length){
			Temporary[] newStack = new Temporary[stack.length * 2];
			System.arraycopy(stack, 0, newStack, 0, stack.length);
			AbstractInsnNode[] newDefiningInstructions = new AbstractInsnNode[definingInstructions.length * 2];
			System.arraycopy(definingInstructions, 0, newDefiningInstructions, 0, definingInstructions.length);
			stack = newStack;
			definingInstructions = newDefiningInstructions;
		}
	}

	public void push(Temporary t, AbstractInsnNode definingInstruction){
		ensureCapacity(index);
		stack[index] = t;
		definingInstructions[index] = definingInstruction;
		index++;
	}

	public Temporary pop(){
		index--;
		final Temporary res = stack[index];
		stack[index] = null;
		definingInstructions[index] = null;
		return res;
	}

	public Temporary peek() {
		return stack[index - 1];
	}

	public void insertElementAt(Temporary value, AbstractInsnNode defining, int indexToInsert){
		ensureCapacity(index);
		for(int i = index; i > indexToInsert; i--){
			stack[i] = stack[i - 1];
			definingInstructions[i] = definingInstructions[i - 1];
		}
		stack[indexToInsert] = value;
		definingInstructions[indexToInsert] = defining;
		index++;
	}

	public Temporary elementAt(int index){
		return stack[index];
	}

	public int size(){
		return index;
	}

	public AbstractInsnNode definingInstruction(int index){
		return definingInstructions[index];
	}

	public JavaStack clone(){
		return new JavaStack(stack.clone(), definingInstructions.clone(), index);
	}

	@Override
	public String toString() {
		if(stack[0] != null){
			final StringBuilder sb = new StringBuilder("[");
			for(int i = 0; i < stack.length; i++){
				if(stack[i] == null) break;
				sb.append(stack[i]).append(", ");
			}
			return sb.substring(0, sb.length() - 2).concat("]");
		}else{
			return "[]";
		}
	}

	public void set(int i, Temporary t, AbstractInsnNode definingInstruction) {
		stack[i] = t;
		definingInstructions[i] = definingInstruction;
	}

}
