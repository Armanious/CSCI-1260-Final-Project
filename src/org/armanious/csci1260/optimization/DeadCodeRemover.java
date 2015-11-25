package org.armanious.csci1260.optimization;

import java.lang.reflect.Modifier;

import org.armanious.csci1260.DataManager;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class DeadCodeRemover {

	private final DataManager dm;

	public DeadCodeRemover(DataManager dm){
		this.dm = dm;
	}

	//TODO also, if we had locals like:
	// I, I, [I, I
	//that we optimized to
	// I, uninitialized, [I, I
	// change the indices i of all locals after uninitialized local to i - 1, dec mn.maxLocals
	/*
	 * for each method that is not marked static that behaves as static, make it static,
	 * change the locals, update all references to that method (removing the instance being
	 * pushed onto the stack)
	 */
	public void optimize(){
		for(ClassNode cn : dm.classes){
			for(MethodNode mn : cn.methods){
				if(Modifier.isAbstract(mn.access) || Modifier.isNative(mn.access)) continue;
				
				mn.instructions.get(0);
				boolean changed = true;
				while(changed){
					changed = false;
					for(AbstractInsnNode ain = mn.instructions.getFirst(); ain != null; ain = ain.getNext()){
						if(ain.getOpcode() == Opcodes.POP){
							if(ain.getPrevious().getOpcode() == Opcodes.ILOAD || 
									ain.getPrevious().getOpcode() == Opcodes.FLOAD || 
									ain.getPrevious().getOpcode() == Opcodes.ALOAD){
								mn.instructions.remove(ain.getPrevious());
								mn.instructions.remove(ain);
								changed = true;
							}
						}else if(ain.getOpcode() == Opcodes.POP2){
							if(ain.getPrevious() == null){
								System.err.println(cn.name + "." + mn.name + mn.desc + "\n\tInstruction " + ain.getIndex());
							}
							if(ain.getPrevious().getOpcode() == Opcodes.DLOAD ||
									ain.getPrevious().getOpcode() == Opcodes.LLOAD){
								mn.instructions.remove(ain.getPrevious());
								mn.instructions.remove(ain);
								changed = true;
							}else if(ain.getPrevious().getOpcode() == Opcodes.ILOAD || 
									ain.getPrevious().getOpcode() == Opcodes.FLOAD || 
									ain.getPrevious().getOpcode() == Opcodes.ALOAD){
								if(ain.getPrevious().getPrevious().getOpcode() == Opcodes.ILOAD || 
										ain.getPrevious().getPrevious().getOpcode() == Opcodes.FLOAD || 
										ain.getPrevious().getPrevious().getOpcode() == Opcodes.ALOAD){
									mn.instructions.remove(ain.getPrevious().getPrevious());
									mn.instructions.remove(ain.getPrevious());
									mn.instructions.remove(ain);
									changed = true;
								}
							}
						}
					}
				}
			}
		}
	}

}
