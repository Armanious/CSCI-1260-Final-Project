package org.armanious.csci1260;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.armanious.csci1260.temporaries.Temporary;
import org.objectweb.asm.tree.AbstractInsnNode;

public class BasicBlock {
	//index of block within the Method
	public final int index;

	public final ArrayList<BlockEdge> predecessors = new ArrayList<>();
	public final ArrayList<BlockEdge> successors = new ArrayList<>();
	//maximize size of 4, one for each BlockEdge.TYPE (TRUE, FALSE, ALWAYS, NEVER)

	public final Set<BasicBlock> dominates = new HashSet<>();

	public JavaStack stackAtStart;// = new Stack<>();
	public final ArrayList<Temporary> locals = new ArrayList<>();
	//public final ArrayList<Temporary> localsAtStart = new ArrayList<>();
	public final HashMap<AbstractInsnNode, Tuple<Temporary[], Temporary>> operandsAndResultPerInsn = new HashMap<>();
	public final HashMap<Integer, Temporary> localsSetInBlock = new HashMap<>();

	AbstractInsnNode firstInsnInBlock;
	AbstractInsnNode lastInsnInBlock;

	public BasicBlock(int index){
		this.index = index;
	}

	public AbstractInsnNode getFirstInsnInBlock(){
		return firstInsnInBlock;
	}

	public AbstractInsnNode getLastInsnInBlock(){
		return lastInsnInBlock;
	}

	public Iterator<AbstractInsnNode> instructionIteratorForward(){
		return new Iterator<AbstractInsnNode>() {
			private AbstractInsnNode cur = firstInsnInBlock;
			@Override
			public AbstractInsnNode next() {
				if(cur == null || cur == lastInsnInBlock.getNext()){
					return null;
				}
				AbstractInsnNode toRet = cur;
				cur = cur.getNext();
				return toRet;
			}

			@Override
			public boolean hasNext() {
				return cur != null && cur != lastInsnInBlock.getNext();
			}
		};
	}

	public Iterator<AbstractInsnNode> instructionIteratorReverse(){
		return new Iterator<AbstractInsnNode>() {
			private AbstractInsnNode cur = lastInsnInBlock;
			@Override
			public AbstractInsnNode next() {
				if(cur == null || cur == firstInsnInBlock.getPrevious()){
					return null;
				}
				AbstractInsnNode toRet = cur;
				cur = cur.getPrevious();
				return toRet;
			}

			@Override
			public boolean hasNext() {
				return cur != null && cur != firstInsnInBlock.getPrevious();
			}
		};
	}

	public String toString(){
		return "B" + String.valueOf(firstInsnInBlock.getIndex()) + "-" + String.valueOf(lastInsnInBlock.getIndex());
	}

}