package org.armanious.csci1260.temporaries;

import java.util.ArrayList;
import java.util.Arrays;

import org.armanious.csci1260.DataManager;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

public class PhiTemporary extends Temporary {

	private final DataManager dm;
	
	public final Temporary[] mergedTemporaries;
	public final int index;

	public PhiTemporary(DataManager dm, Temporary[] toMerge, int index, Type knownType) {
		super(null, knownType);
		this.dm = dm;
		this.mergedTemporaries = toMerge;
		this.index = index;
	}

	public void debugType() {
		Type t = dm.getCommonSuperType(mergedTemporaries);
	}

	public PhiTemporary(DataManager dm, Temporary[] toMerge, int index) {
		this(dm, toMerge, index, dm.getCommonSuperType(toMerge));
	}

	@Override
	public int getConstancyInternal() {
		return NOT_CONSTANT;
	}

	@Override
	public boolean equals(Object o) {
		if(o == this) return true;
		if(!(o instanceof PhiTemporary)) return false;
		return index == ((PhiTemporary)o).index && Arrays.equals(mergedTemporaries, ((PhiTemporary)o).mergedTemporaries);
	}

	@Override
	public String toString() {
		return "local_" + index;//"(Local " + index + ": " + getType() + ")";
	}

	@Override
	protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
		list.add(getDeclaration());
		/*for(Temporary t : mergedTemporaries){
			if(t != null){
				t.addRelevantInstructionsToList(list);
			}
		}*/
	}

	@Override
	protected void addCriticalTemporariesToList(ArrayList<Temporary> list) {
		for(Temporary t : mergedTemporaries){
			if(t != null){
				list.add(t);
				//t.addCriticalTemporariesToList(list);
			}
		}
	}

	@Override
	protected Temporary clone() {
		return new PhiTemporary(dm, mergedTemporaries, index, getType());
	}

}