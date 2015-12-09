package org.armanious.csci1260.temporaries;

import java.util.ArrayList;
import java.util.Arrays;

import org.armanious.csci1260.DataManager;
import org.armanious.csci1260.MethodInformation;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

//NOT static because we need access to the enclosing DataManager class to determine side effects
public class MethodInvocationTemporary extends Temporary {

	final DataManager dm;
	
	final Temporary[] args;
	public final String owner;
	public final String name;
	public final String desc;
	final boolean isStatic;

	private boolean calculatedSideEffects;
	private boolean hasSideEffects;

	public MethodInvocationTemporary(DataManager dm, boolean isStatic, Temporary[] args, String owner, String name, String desc, Type returnType){
		super(null, returnType);
		this.dm = dm;
		this.args = args;
		this.owner = owner;
		this.name = name;
		this.isStatic = isStatic;
		this.desc = desc;
	}

	public MethodInvocationTemporary(DataManager dm, AbstractInsnNode decl, Temporary[] args, String owner, String name, String desc, Type returnType){
		super(decl, returnType);
		this.dm = dm;
		this.args = args;
		this.owner = owner;
		this.name = name;
		this.desc = desc;
		isStatic = decl.getOpcode() == Opcodes.INVOKESTATIC || decl.getOpcode() == Opcodes.INVOKEDYNAMIC;
		for(Temporary arg : args){
			arg.addReference(decl, null);
		}
	}

	public boolean hasSideEffects(){
		if(!calculatedSideEffects){

			ClassNode cn = dm.getClassNode(owner);
			if(cn == null){
				//not a user-specified method, might be library
				String s = owner + "." + name + desc;
				hasSideEffects = Arrays.binarySearch(DataManager.METHODS_KNOWN_NO_SIDE_EFFECTS, s) < 0;
				calculatedSideEffects = true;
				return hasSideEffects;
			}
			MethodNode mn = cn.getMethodNode(name, desc);
			if(mn == null){
				return true;
			}
			MethodInformation mi = dm.methodInformations.get(mn);
			if(mi == null){
				return true;
			}
			hasSideEffects = mi.hasSideEffects();
			calculatedSideEffects = true;
		}
		return hasSideEffects;
	}

	public Temporary[] getArgs(){
		return args;
	}

	@Override
	public int getConstancyInternal() {
		return hasSideEffects() ? NOT_CONSTANT : mergeConstancy(args);
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(!(o instanceof MethodInvocationTemporary)) return false;
		MethodInvocationTemporary mit = (MethodInvocationTemporary) o;
		if(!owner.equals(mit.owner) || !name.equals(mit.name) || args.length != mit.args.length){
			return false;
		}
		for(int i = 0; i < args.length; i++){
			if(!args[i].equals(mit.args[i])){
				return false;
			}
		}
		return true;
	}

	public String toString(){
		if(isStatic){
			return owner + "." + name + Arrays.toString(args).replace('[', '(').replace(']', ')');
		}else{
			return args[0].toString() + "." + name + Arrays.toString(Arrays.copyOfRange(args, 1, args.length)).replace('[', '(').replace(']', ')');
		}
		//return "M()" + type.toString();
	}

	@Override
	protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
		if(!(getDeclaration() instanceof VarInsnNode)){
			for(int i = 0; i < args.length; i++){
			args[i].addRelevantInstructionsToListSorted(list);
		}
		}
		list.add(getDeclaration());
	}

	@Override
	protected void addCriticalTemporariesToList(ArrayList<Temporary> list) {
		for(Temporary t : args){
			list.add(t);
			//t.addCriticalTemporariesToList(list);
		}
	}

	@Override
	protected Temporary clone() {
		return new MethodInvocationTemporary(dm, isStatic, args.clone(), owner, name, desc, getType());
	}

}