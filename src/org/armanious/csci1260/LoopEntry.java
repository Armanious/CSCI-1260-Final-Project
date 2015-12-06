package org.armanious.csci1260;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class LoopEntry {

	public final LoopEntry parent;
	public final BasicBlock entry;
	public final ArrayList<LoopEntry> children = new ArrayList<>();
	//must be array list to keep the relative order of the children preserved
	//first child that is executed is the first element; last is last
	public final Set<BasicBlock> blocksInLoop = new HashSet<>();
	public final BasicBlock end;
	public final int nthSibling;

	public LoopEntry(BasicBlock entry, BasicBlock endNotExit, LoopEntry prev){
		this.entry = entry;
		this.end = endNotExit;
		//endNotExit.successors.forEach((BE) -> {if(BE.classification == BlockEdge.Classification.BACK)BE.b2=entry.successors.get(0).b2;});

		while(prev != null && !prev.contains(entry)){
			prev = prev.parent;
		}
		this.parent = prev;
		if(parent != null){
			nthSibling = parent.children.size();
			parent.children.add(this);
		}else{
			nthSibling = -1;
		}

		Stack<BasicBlock> toAddStack = new Stack<>();
		toAddStack.add(end);
		BasicBlock toIgnoreAfter = entry.successors.get(0).b2;
		//first block executed within the loop; "top" of loop

		while(!toAddStack.isEmpty()){
			BasicBlock toAdd = toAddStack.pop();
			if(toAdd != toIgnoreAfter){
				blocksInLoop.add(toAdd);
				for(BlockEdge predecessor : toAdd.predecessors){
					if(!blocksInLoop.contains(predecessor.b1)){
						toAddStack.push(predecessor.b1);
					}
				}
			}
		}

	}

	public boolean contains(BasicBlock b){
		return blocksInLoop.contains(b);
	}

	@Override
	public String toString() {
		return "LE_" + entry;
	}

}