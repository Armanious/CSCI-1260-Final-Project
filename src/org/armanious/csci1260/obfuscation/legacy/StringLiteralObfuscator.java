package org.armanious.csci1260.obfuscation.legacy;

import java.lang.reflect.Modifier;
import java.util.ArrayList;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

@Deprecated
public class StringLiteralObfuscator {
	
	
	//produce a graph of String usage to obfuscate and intersperse the encryption of strings?
	/*
	 * great idea: create a single decryption algorithm that takes into consideration
	 * the elements on the stack trace (obviously, graph theory to the max here, but holy
	 * crap that would be an amazing obfuscation)
	 */
	public void obfuscateStringLiterals(ArrayList<ClassNode> classes){
		for(ClassNode cn : classes){
			for(FieldNode fn : cn.fields){
				if(Modifier.isFinal(fn.access) && fn.value == null){
					System.err.println(cn.name + "." + fn.name);
				}
				if(fn.value instanceof String){
					
				}
			}
			
			for(MethodNode mn : cn.methods){
				for(AbstractInsnNode ain = mn.instructions.getFirst(); ain != null; ain = ain.getNext()){
					if(ain.getType() == AbstractInsnNode.LDC_INSN){
						LdcInsnNode lin = (LdcInsnNode) ain;
						if(lin.cst instanceof String){
							
						}
					}
				}
			}
		}
	}
	
	private void insertStringLiteralEncryption(AbstractInsnNode toInsertAfter,
			FieldNode fn){
	}

}
