package org.armanious.csci1260;

import org.armanious.csci1260.DataManager.Temporary;
import org.objectweb.asm.tree.AbstractInsnNode;

public final class JavaStack {
	
	private static final int MAX_STACK_SIZE = 300;

	private final Temporary[] stack;
	private final AbstractInsnNode[] definingInstructions;
	private int index = 0;

	private JavaStack(Temporary[] stack, AbstractInsnNode[] definingInstructions, int index){
		this.stack = stack;
		this.definingInstructions = definingInstructions;
		this.index = index;
	}

	public JavaStack(int maxSize){
		if(maxSize == 0){
			//System.err.println("Warning: creating stack with stack size of 0. Default to MAX_STACK_SIZE = " + MAX_STACK_SIZE);
			maxSize = MAX_STACK_SIZE;
		}
		stack = new Temporary[maxSize];
		definingInstructions = new AbstractInsnNode[maxSize];
	}

	public void push(Temporary t, AbstractInsnNode definingInstruction){
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

	public void insertElementAt(Temporary value, AbstractInsnNode defining, int indexToInsert) {
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
		if(stack.length > 0){
			final StringBuilder sb = new StringBuilder("[");
			for(int i = 0; i < stack.length - 1; i++){
				sb.append(stack[i]).append(", ");
			}
			return sb.append(stack[stack.length - 1]).append("]").toString();
		}else{
			return "[]";
		}
	}

}
