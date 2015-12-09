package org.armanious.csci1260.temporaries;

import java.util.ArrayList;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ArrayInstanceTemporary extends Temporary {

	private final Object fakeValue;
	public final Temporary[] dimensionCounts;

	private static Type createArrayType(Type type, int numDimensions){
		final StringBuilder sb = new StringBuilder();
		for(int i = 0; i < numDimensions; i++){
			sb.append('[');
		}
		sb.append(type.getDescriptor());
		return Type.getType(sb.toString());
	}

	private ArrayInstanceTemporary(Object fakeValue, AbstractInsnNode decl, Type type, Temporary...dimensionCounts){
		super(decl, dimensionCounts.length > 1 ? type : createArrayType(type, dimensionCounts.length));//createArrayType(type, dimensionCounts.length));
		this.dimensionCounts = dimensionCounts;
		for(Temporary t : dimensionCounts){
			t.addReference(decl, null);
		}
		this.fakeValue = fakeValue;
	}

	public ArrayInstanceTemporary(AbstractInsnNode decl, Type type, Temporary...dimensionCounts){
		this(new Object(), decl, type, dimensionCounts);
	}

	@Override
	public int getConstancyInternal() {
		return mergeConstancy(dimensionCounts);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof ArrayInstanceTemporary && (((ArrayInstanceTemporary)o).fakeValue == fakeValue);
	}

	public String toString(){
		StringBuilder sb = new StringBuilder("new ").append(type.toString());
		for(Temporary dim : dimensionCounts){
			sb.append('[').append(dim.toString()).append(']');
		}
		return sb.toString();
	}

	@Override
	protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
		if(!(getDeclaration() instanceof VarInsnNode)){
			if(getDeclaration().getOpcode() == Opcodes.NEWARRAY || getDeclaration().getOpcode() == Opcodes.ANEWARRAY || getDeclaration().getOpcode() == Opcodes.MULTIANEWARRAY){
		
			for(int i = dimensionCounts.length - 1; i >= 0; i--){
				dimensionCounts[i].addRelevantInstructionsToListSorted(list);
			}
		}
		}
		list.add(getDeclaration());
	}

	@Override
	protected void addCriticalTemporariesToList(ArrayList<Temporary> list) {
		for(Temporary t : dimensionCounts){
			list.add(t);
			//t.addCriticalTemporariesToList(list);
		}
	}

	@Override
	protected Temporary clone() {
		return new ArrayInstanceTemporary(fakeValue, getDeclaration(), getType(), dimensionCounts);
	}

	public Temporary getLengthTemporary() {
		return dimensionCounts[0];
	}

}