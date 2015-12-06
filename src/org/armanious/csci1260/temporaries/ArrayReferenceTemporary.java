package org.armanious.csci1260.temporaries;

import java.util.ArrayList;

import org.armanious.csci1260.DataManager;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ArrayReferenceTemporary extends Temporary {

	private static Type computeArrayDereferenceType(Temporary arrayRef){
		return Type.getType(arrayRef.getType().getDescriptor().substring(1));
	}

	public final Temporary arrayRef;
	public final Temporary index;

	public ArrayReferenceTemporary(AbstractInsnNode decl, Temporary arrayRef, Temporary index) {
		super(decl, computeArrayDereferenceType(arrayRef));
		if(arrayRef instanceof ConstantTemporary && ((ConstantTemporary)arrayRef).value == null){
			System.err.println("bpp");
		}
		this.arrayRef = arrayRef;
		this.index = index;
		arrayRef.addReference(decl, null);
	}

	@Override
	public int getConstancyInternal() {
		return mergeConstancy(arrayRef, index);
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(!(o instanceof ArrayReferenceTemporary)) return false;
		ArrayReferenceTemporary art = (ArrayReferenceTemporary) o;
		return art.arrayRef.equals(arrayRef) && art.index.equals(index);
	}

	public String toString(){
		return arrayRef.toString() + "[" + index.toString() + "]";
	}

	@Override
	protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
		if(!(getDeclaration() instanceof VarInsnNode)){
			arrayRef.addRelevantInstructionsToListSorted(list);
		index.addRelevantInstructionsToListSorted(list);
		}
		list.add(getDeclaration());
	}

	@Override
	protected void addCriticalTemporariesToList(ArrayList<Temporary> list) {
		list.add(arrayRef);
		//arrayRef.addCriticalTemporariesToList(list);
		list.add(index);
		//index.addCriticalTemporariesToList(list);
	}

	//int[][][] arr = new int[100][200][300];
	//arr.length = 100
	//arr[i].length = 200
	//arr[i][j].length = 300;
	//we are handling arr[i].length, with arrayRef = arr (ArrayInstance), index = i
	//and arr[i][j].length with arrayRef = arr[i] (ArrayReference), index = j
	public Temporary attemptGetReferencedArrayLength(){
		Temporary arrayInstance = arrayRef;
		int dimensionToGetLengthOf = 0;
		while(!(arrayInstance instanceof ArrayInstanceTemporary)){
			arrayInstance = ((ArrayReferenceTemporary)arrayInstance).arrayRef;
			dimensionToGetLengthOf++;
		}
		return ((ArrayInstanceTemporary)arrayInstance).dimensionCounts[dimensionToGetLengthOf];
	}

	@Override
	protected Temporary clone() {
		return new ArrayReferenceTemporary(getDeclaration(), arrayRef, index);
	}

}