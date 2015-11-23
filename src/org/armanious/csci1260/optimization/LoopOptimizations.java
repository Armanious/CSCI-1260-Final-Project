package org.armanious.csci1260.optimization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

import org.armanious.csci1260.DataManager;
import org.armanious.csci1260.DataManager.BasicBlock;
import org.armanious.csci1260.DataManager.BlockEdge;
import org.armanious.csci1260.DataManager.ConstantTemporary;
import org.armanious.csci1260.DataManager.LoopEntry;
import org.armanious.csci1260.DataManager.MethodInformation;
import org.armanious.csci1260.DataManager.PhiTemporary;
import org.armanious.csci1260.DataManager.Temporary;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.Textifier;

public class LoopOptimizations {

	private final DataManager dm;

	public LoopOptimizations(DataManager dm){
		this.dm = dm;
	}

	/*
	 * If we have a temporary calculated within a loop whose operands are all calculated
	 * not within that same loop (either in a parent loop or not in a loop at all), then
	 * move the calculation of that temporary to outside of the loop, store it in a local variable,
	 * and replace the calculation within the loop with the corresponding load instruction
	 * 
	 * we should do this when operands are essentially local variables
	 */

	private BasicBlock getBasicBlockInstructionIsIn(MethodInformation mi, AbstractInsnNode ain){
		BasicBlock block;
		do{
			block = mi.blocks.get(ain);
			ain = ain.getPrevious();
		}while(block == null && ain != null);
		return block;
	}

	private boolean isInvariant(MethodInformation mi, HashMap<Integer, LoopEntry> varianceMap, LoopEntry currentEntry, Temporary t){
		if(t instanceof PhiTemporary){
			PhiTemporary pt = (PhiTemporary) t;
			boolean nullIsOnlyBackEdges = true;
			int nonNullIndex = -1;
			for(int i = 0; i < pt.mergedTemporaries.length && nullIsOnlyBackEdges; i++){
				if(pt.mergedTemporaries[i] == null){
					nullIsOnlyBackEdges = pt.initialBlock.predecessors.get(i).classification == BlockEdge.Classification.BACK;					
				}else{
					if(nonNullIndex != -1){
						nullIsOnlyBackEdges = false;
					}else{
						nonNullIndex = i;
					}
				}
			}
			if(nullIsOnlyBackEdges && nonNullIndex != -1){
				t = pt.mergedTemporaries[nonNullIndex];
			}
		}
		ArrayList<Temporary> criticalTemporaries = t.getCriticalTemporaries();
		if(criticalTemporaries == null || criticalTemporaries.size() == 0) return false;
		for(Temporary critTemp : criticalTemporaries){
			final AbstractInsnNode ain = critTemp.getDeclaration();
			if(ain == null){
				return false;
			}
			switch(ain.getOpcode()){
			case Opcodes.ILOAD:
			case Opcodes.ALOAD:
			case Opcodes.LLOAD:
			case Opcodes.DLOAD:
			case Opcodes.FLOAD:
				LoopEntry loop = varianceMap.get(((VarInsnNode)critTemp.getDeclaration()).var);
				if(loop != null && currentEntry.contains(getBasicBlockInstructionIsIn(mi, ain))){
					return false;
				}
				continue;
				//break;
			default:
				return false;
			}
		}
		return true;
	}

	public void optimize(){
		for(MethodInformation mi : dm.methodInformations.values()){
			System.out.println(mi.mn.name + ":");

			HashMap<BasicBlock, LoopEntry> loops = mi.loops;
			HashMap<BasicBlock, LoopEntry> loopRoots = mi.loopRoots;

			HashSet<BasicBlock> searched = new HashSet<>();
			HashMap<Integer, LoopEntry> variantLocals = new HashMap<>();
			HashMap<LoopEntry, HashSet<Temporary>> invariantTemporaries = new HashMap<>();

			for(BasicBlock loopRootEntry : loopRoots.keySet()){
				LoopEntry loopRoot = loopRoots.get(loopRootEntry);
				Stack<LoopEntry> toAnalyze = new Stack<>();
				Stack<LoopEntry> toSearch = new Stack<>();
				toAnalyze.push(loopRoot);
				toSearch.push(loopRoot);
				while(!toSearch.isEmpty()){
					LoopEntry searching = toSearch.pop();
					for(LoopEntry child : searching.children){
						toAnalyze.push(child);
						toSearch.push(child);
					}
				}
				//from innermost loop out
				@SuppressWarnings("unchecked")
				Stack<LoopEntry> toAnalyzeClone = (Stack<LoopEntry>) toAnalyze.clone();

				while(!toAnalyze.isEmpty()){
					LoopEntry entry = toAnalyze.pop();
					for(BasicBlock block : entry.blocksInLoop){
						if(!searched.add(block)) continue; //ensures that we analyze only 
						//the blocks that are not part of any inner loops
						Iterator<AbstractInsnNode> iter = block.instructionIteratorForward();
						while(iter.hasNext()){
							AbstractInsnNode ain = iter.next();
							switch(ain.getOpcode()){
							case Opcodes.ASTORE:
							case Opcodes.ISTORE:
							case Opcodes.LSTORE:
							case Opcodes.DSTORE:
							case Opcodes.FSTORE:
								VarInsnNode vin = (VarInsnNode) ain;
								if(!variantLocals.containsKey(vin.var)){
									variantLocals.put(vin.var, entry);
								}
								break;
							case Opcodes.IINC:
								IincInsnNode iin = (IincInsnNode) ain;
								if(!variantLocals.containsKey(iin.var)){
									variantLocals.put(iin.var, entry);
								}
								break;
							case Opcodes.IASTORE:
							case Opcodes.AASTORE:
							case Opcodes.LASTORE:
							case Opcodes.DASTORE:
							case Opcodes.FASTORE:
							case Opcodes.SASTORE:
							case Opcodes.CASTORE:
							case Opcodes.BASTORE:
								//TODO
								break;
							}

							//can't do this yet; local variable might be set after this instruction
							/*Temporary t = mi.temporaries.get(ain);
								if(t != null && isInvariant(mi, variantLocals, entry, t)){
									HashSet<Temporary> set = invariantTemporaries.get(entry);
									if(set == null){
										set = new HashSet<>();
										invariantTemporaries.put(entry, set);
									}
									set.add(t);
								}*/

						}
					}
				}

				searched.clear();
				toAnalyze = toAnalyzeClone;

				while(!toAnalyze.isEmpty()){
					LoopEntry entry = toAnalyze.pop();
					for(BasicBlock block : entry.blocksInLoop){
						if(!searched.add(block)) continue; //ensures that we analyze only 
						//the blocks that are not part of any inner loops
						Iterator<AbstractInsnNode> iter = block.instructionIteratorForward();
						while(iter.hasNext()){
							AbstractInsnNode ain = iter.next();
							Temporary t = mi.temporaries.get(ain);
							if(t != null && isInvariant(mi, variantLocals, entry, t)){
								HashSet<Temporary> set = invariantTemporaries.get(entry);
								if(set == null){
									set = new HashSet<>();
									invariantTemporaries.put(entry, set);
								}
								set.add(t);
							}

						}
					}
				}

				System.out.println("variantLocals = " + variantLocals);
				if(invariantTemporaries.size() > 0){
					System.out.println("invariantTemporaries = ");
					for(LoopEntry key : invariantTemporaries.keySet()){
						System.out.println("\t" + key.entry.toString() + ": " + invariantTemporaries.get(key));
					}
				}else{
					System.out.println("invariantTemporaries = " + invariantTemporaries);
				}
				
			}

			/*
				for(BasicBlock loopEntry : loops.keySet()){
					LoopEntry loop = loops.get(loopEntry);

					for(BasicBlock block : loop.blocksInLoop){
						Iterator<AbstractInsnNode> blockIter = block.instructionIteratorForward();
						while(blockIter.hasNext()){
							AbstractInsnNode insn = blockIter.next();
							Temporary t = mi.temporaries.get(insn);
							if(t == null || t instanceof ConstantTemporary) continue;
							ArrayList<Temporary> criticalTemporaries = t.getCriticalTemporaries();
							if(criticalTemporaries == null || criticalTemporaries.size() == 0) continue;
							boolean validForAnalysis = true;
							for(Temporary critTemp : criticalTemporaries){
								if(critTemp.getDeclaration() == null){
									System.out.println(t + " is not valid for analysis: " + critTemp);
									validForAnalysis = false;
									break;
								}
								switch(critTemp.getDeclaration().getOpcode()){
								case Opcodes.ILOAD:
								case Opcodes.ALOAD:
								case Opcodes.LLOAD:
								case Opcodes.DLOAD:
								case Opcodes.FLOAD:
									continue;
								default:
									validForAnalysis = false;
									break;
								}
								if(!validForAnalysis){
									System.out.println(t + " is not valid for analysis: " + critTemp);
									break;
								}
							}
							if(validForAnalysis){
								System.err.println(t + " is valid for analysis.");
							}
						}
					}
				}*/

			System.out.println("\n");
		}
	}
	
	
	
}
