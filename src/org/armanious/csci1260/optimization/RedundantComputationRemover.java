package org.armanious.csci1260.optimization;

import java.util.ArrayList;

import org.armanious.csci1260.DataManager;
import org.armanious.csci1260.DataManager.ConstantTemporary;
import org.armanious.csci1260.DataManager.MethodInformation;
import org.armanious.csci1260.DataManager.Temporary;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
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
	
	public void optimize(){
		for(ClassNode cn : dm.classes){
			for(MethodNode mn : cn.methods){
				MethodInformation mi = dm.methodInformations.get(mn);
				if(mi == null) continue;

				final int size = mi.temporaries.size();
				final Temporary[] arr = mi.temporaries.values().toArray(new Temporary[size]);

				for(int i = 0; i < size; i++){
					final Temporary T = arr[i];
					for(int j = i + 1; j < size; j++){
						final Temporary S = arr[j];
						if(T.equals(S)){
							ArrayList<AbstractInsnNode> block = T.getContiguousBlockSorted();
							if(block != null && block.size() >= 3){
								//TODO
							}
						}
					}
				}
			}
		}
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
				ArrayList<AbstractInsnNode> block = t1.getContiguousBlockSorted();
				if(block != null && block.size() >= 3){
					for(int j = i + 1; j < temps.size(); j++){
						final Temporary t2 = temps.get(j);
						if(t1.equals(t2)){

							ArrayList<AbstractInsnNode> toDeleteBlock = t2.getContiguousBlockSorted();
							AbstractInsnNode toInsertBefore = t2.getDeclaration().getNext();

							if(toInsertBefore == null){
								System.err.println("What");
								continue;
							}
							mi.mn.instructions.insert(t1.getDeclaration(),
									new VarInsnNode(DataManager.getStoreOpcode(t1.getType()), mi.mn.maxLocals));
							mi.mn.instructions.insert(t1.getDeclaration(),
									new InsnNode(Opcodes.DUP));
							
							for(AbstractInsnNode toDelete : toDeleteBlock){
								mi.mn.instructions.remove(toDelete);
							}
							
							mi.mn.instructions.insertBefore(toInsertBefore, new VarInsnNode(DataManager.getLoadOpcode(t1.getType()), mi.mn.maxLocals));
							System.out.println("Made change");
							mi.mn.maxLocals++;
						}
					}
				}
			}
		}
	}

}
