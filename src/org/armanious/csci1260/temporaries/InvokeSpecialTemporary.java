package org.armanious.csci1260.temporaries;

import java.util.Arrays;

import org.armanious.csci1260.DataManager;
import org.objectweb.asm.tree.AbstractInsnNode;

public class InvokeSpecialTemporary extends MethodInvocationTemporary {


	public InvokeSpecialTemporary(DataManager dm, AbstractInsnNode insn, Temporary[] args, String name, String desc) {
		//args will include the ObjectInstance Temporary
		/*
		 * new X  //X
		 * dup  //X, X
		 * aload 1 //X, X, a1
		 * invokespecial X //POPS ALL 3; pushed Constructor(X)
		 * astore 2 //a2 = Constructor(X) empty stack
		 */
		super(dm, insn, args, args[0].getType().getInternalName(), name, desc, args[0].getType());
	}

	@Override
	public String toString() {
		return "new " + owner + Arrays.toString(Arrays.copyOfRange(args, 2, args.length)).replace('[', '(').replace(']', ')');
	}

	@Override
	protected Temporary clone() {
		return new InvokeSpecialTemporary(dm, getDeclaration(), args, name, desc);
	}

}