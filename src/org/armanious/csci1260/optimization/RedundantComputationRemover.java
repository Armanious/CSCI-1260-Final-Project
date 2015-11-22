package org.armanious.csci1260.optimization;

import java.util.ArrayList;

import org.armanious.csci1260.DataManager;
import org.armanious.csci1260.DataManager.MethodInformation;
import org.armanious.csci1260.DataManager.Temporary;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class RedundantComputationRemover {

	private final DataManager dm;

	public RedundantComputationRemover(DataManager dm){
		this.dm = dm;
	}
	
	private int getLength(AbstractInsnNode start, AbstractInsnNode end){
		if(start == null || end == null) return 0;
		if(start == end) return 1;
		int i = 2;
		while(start != null && (start = start.getNext()) != end){
			i++;
		}
		return i;
	}
	
	//copied to StackManipulator
	private int getStoreOpcode(Type t){
		if(t == Type.BOOLEAN_TYPE ||
				t == Type.INT_TYPE ||
				t == Type.BYTE_TYPE ||
				t == Type.CHAR_TYPE ||
				t == Type.SHORT_TYPE){
			return Opcodes.ISTORE;
		}else if(t == Type.DOUBLE_TYPE){
			return Opcodes.DSTORE;
		}else if(t == Type.FLOAT_TYPE){
			return Opcodes.FSTORE;
		}else if(t == Type.LONG_TYPE){
			return Opcodes.LSTORE;
		}
		return Opcodes.ASTORE;
	}
	
	private int getLoadOpcode(Type t){
		if(t == Type.BOOLEAN_TYPE ||
				t == Type.INT_TYPE ||
				t == Type.BYTE_TYPE ||
				t == Type.CHAR_TYPE ||
				t == Type.SHORT_TYPE){
			return Opcodes.ILOAD;
		}else if(t == Type.DOUBLE_TYPE){
			return Opcodes.DLOAD;
		}else if(t == Type.FLOAT_TYPE){
			return Opcodes.FLOAD;
		}else if(t == Type.LONG_TYPE){
			return Opcodes.LLOAD;
		}
		return Opcodes.ALOAD;
	}
	
	public void optimize(){
		System.err.println("Warning: redundant computation optimizations not implemented.");
	}
	
	public void optimizeLegacy(){
		System.out.println("Optimizing redundant computations");
		//TODO
		//Do DFS search and sort temporaries per basic block in descending order according to length,
		//store in Map<Temporary, Integer> sorted by the keys
		for(MethodInformation mi : dm.methodInformations.values()){
			if(!mi.mn.name.equals("loopTest")) continue;
			ArrayList<Temporary> temps = new ArrayList<>(mi.temporaries.values());
			for(int i = 0; i < temps.size(); i++){
				final Temporary t1 = temps.get(i);
				ArrayList<AbstractInsnNode> block = t1.getContiguousBlock();
				if(block != null && block.size() >= 3){
					for(int j = i + 1; j < temps.size(); j++){
						final Temporary t2 = temps.get(j);
						if(t1.equals(t2)){

							ArrayList<AbstractInsnNode> toDeleteBlock = t2.getContiguousBlock();
							AbstractInsnNode toInsertBefore = t2.getDeclaration().getNext();

							if(toInsertBefore == null){
								System.err.println("What");
								continue;
							}
							mi.mn.instructions.insert(t1.getDeclaration(),
									new VarInsnNode(getStoreOpcode(t1.getType()), mi.mn.maxLocals));
							mi.mn.instructions.insert(t1.getDeclaration(),
									new InsnNode(Opcodes.DUP));
							
							for(AbstractInsnNode toDelete : toDeleteBlock){
								mi.mn.instructions.remove(toDelete);
							}
							
							mi.mn.instructions.insertBefore(toInsertBefore, new VarInsnNode(getLoadOpcode(t1.getType()), mi.mn.maxLocals));
							System.out.println("Made change");
							mi.mn.maxLocals++;
						}
					}
				}
			}
		}
	}

}
