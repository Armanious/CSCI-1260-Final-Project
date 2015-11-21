package org.armanious.csci1260.obfuscation.legacy;

import org.armanious.csci1260.DataManager;

@Deprecated
public class StackManipulatorAttempt2 {

	private final DataManager dm;

	public StackManipulatorAttempt2(DataManager manager) {
		this.dm = manager;
	}

	public void obfuscate() {
		/*for(ClassNode cn : dm.classes){
			methods: for(MethodNode mn : cn.methods){
				if(mn.instructions == null || mn.instructions.size() == 0) continue;
				final MethodInformation mi = dm.methodInformations.get(mn);
				for(AbstractInsnNode ain = mn.instructions.getFirst(); ain != null; ain = ain.getNext()){
					if(ain.getType() == AbstractInsnNode.METHOD_INSN && !((MethodInsnNode)ain).name.equals("<init>")){
						Tuple<Temporary[], Temporary> tuple = mi.operandsResultPerInsn.get(ain);
						if(tuple == null) continue;
						Temporary t = tuple.val2;
						AbstractInsnNode start = t.getContiguousBlockStart();
						if(start == null) continue;
						//if(j++ % 10 != 0) continue;
						if(t.getType() == Type.VOID_TYPE){
							mn.instructions.insertBefore(start, new InsnNode(Opcodes.ICONST_5));

							mn.instructions.insert(ain, new InsnNode(Opcodes.POP));
							//System.out.println("Manipulated in " + cn.name + "." + mn.name + mn.desc);
							//continue methods;
						}
						else if(t.getType() == Type.LONG_TYPE || t.getType() == Type.DOUBLE_TYPE){
							mn.instructions.insertBefore(start, new InsnNode(Opcodes.ICONST_5));
							mn.instructions.insert(ain, new InsnNode(Opcodes.POP));
							mn.instructions.insert(ain, new InsnNode(Opcodes.POP2));
							mn.instructions.insert(ain, new InsnNode(Opcodes.DUP2_X1));
						}

						else if(t.getType() == Type.INT_TYPE || t.getType() == Type.FLOAT_TYPE ||
								t.getType() == Type.SHORT_TYPE || t.getType() == Type.CHAR_TYPE ||
								t.getType() == Type.BOOLEAN_TYPE){
							mn.instructions.insertBefore(start, new InsnNode(Opcodes.ICONST_5));
							mn.instructions.insert(ain, new InsnNode(Opcodes.POP));
							mn.instructions.insert(ain, new InsnNode(Opcodes.SWAP));
						}else{
							mn.instructions.insertBefore(start, new InsnNode(Opcodes.ICONST_5));
							mn.instructions.insert(ain, new InsnNode(Opcodes.POP));
							mn.instructions.insert(ain, new InsnNode(Opcodes.SWAP));
						}
					}
				}
			}
		}*/
		System.err.println("Warning: stack manipulator not implemented");
	}

}
