package org.armanious.csci1260.optimization;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.armanious.csci1260.DataManager;
import org.armanious.csci1260.MethodInformation;
import org.armanious.csci1260.temporaries.Temporary;
import org.objectweb.asm.Opcodes;
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

	//TODO
	//For each temporary T, if there is a temporary in locals at the current block that equals T,
	//replace T.getContiguousBlockSorted() with the correct VarInsnNode

	//TODO
	//If there is a local variable LV that is initialized with a ConstantTemporary or other local
	//variable and is not set again, remove LV and update code accordingly
	public void optimize(){
		for(ClassNode cn : dm.classes){
			for(MethodNode mn : cn.methods){
				MethodInformation mi = dm.methodInformations.get(mn);
				if(mi == null) continue;
				//System.out.println(cn.name + "." + mn.name + mn.desc);

				final int size = mi.temporaries.size();
				final Temporary[] arr = mi.temporaries.values().toArray(new Temporary[size]);

				Set<Set<Temporary>> setsOfEqualTemporaries = new HashSet<>();

				for(int i = 0; i < size; i++){
					final Temporary T = arr[i];
					final ArrayList<AbstractInsnNode> block = T.getContiguousBlockSorted();
					if(block == null || block.size() < 5) continue;
					/*for(int q = 0; q < size; q++){
						if(i == q) continue;
						if(arr[q].getCriticalTemporaries().contains(T)){
							System.err.println("Parent temporary of " + T + ": " + arr[q]);
						}
					}*/
					Set<Temporary> setOfEqualTemps = null;
					for(int j = i + 1; j < size; j++){
						final Temporary S = arr[j];
						if(T == S || S.getDeclaration() instanceof VarInsnNode || !T.equals(S)) continue;
						ArrayList<AbstractInsnNode> block2 = S.getContiguousBlockSorted();
						if(block2 != null && block2.size() >= 5){
							if(setOfEqualTemps == null){
								setOfEqualTemps = new HashSet<>();
								setOfEqualTemps.add(T);
								setsOfEqualTemporaries.add(setOfEqualTemps);
							}
							setOfEqualTemps.add(S);
						}
					}
				}

			}
		}
	}

	public void optimizeLegacy(){
		//System.out.println("Optimizing redundant computations");
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
							//System.out.println("Made change");
							mi.mn.maxLocals++;
						}
					}
				}
			}
		}
	}

}
