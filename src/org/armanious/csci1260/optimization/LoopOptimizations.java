package org.armanious.csci1260.optimization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

import org.armanious.csci1260.DataManager;
import org.armanious.csci1260.DataManager.ArrayReferenceTemporary;
import org.armanious.csci1260.DataManager.BasicBlock;
import org.armanious.csci1260.DataManager.BlockEdge;
import org.armanious.csci1260.DataManager.LoopEntry;
import org.armanious.csci1260.DataManager.MethodInformation;
import org.armanious.csci1260.DataManager.MethodInvocationTemporary;
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

	private Temporary resolvePrematurePhiTemporary(PhiTemporary pt){
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
			return pt.mergedTemporaries[nonNullIndex].cloneOnInstruction(pt.getDeclaration());
		}
		return pt;
	}

	private boolean isVarInsnInvariant(HashMap<Integer, LoopEntry> varianceMap, LoopEntry currentEntry, VarInsnNode vin){
		final LoopEntry loop = varianceMap.get(vin.var);
		if(loop != null && (!loop.contains(currentEntry.entry) || loop == currentEntry)){
			return false;
		}
		return true;
	}
	
	private boolean isInvariant(MethodInformation mi, HashMap<Integer, LoopEntry> varianceMap, LoopEntry currentEntry, Temporary t){
		if(t instanceof PhiTemporary){
			t = resolvePrematurePhiTemporary((PhiTemporary)t);
		}
		if(t instanceof MethodInvocationTemporary) return false; //assume all methods have side effects for now
		if(t.getDeclaration() != null){
			if(t.getDeclaration().getOpcode() >= Opcodes.IASTORE && t.getDeclaration().getOpcode() <= Opcodes.SASTORE){
				return false;
			}
			if(t.getDeclaration() instanceof VarInsnNode && !isVarInsnInvariant(varianceMap, currentEntry, (VarInsnNode)t.getDeclaration())){
				return false;
			}
		}
		ArrayList<Temporary> criticalTemporaries = t.getCriticalTemporaries();
		if(criticalTemporaries == null || criticalTemporaries.size() == 0) return false;
		for(Temporary critTemp : criticalTemporaries){
			final AbstractInsnNode ain = critTemp.getDeclaration();
			if(ain == null || !(ain instanceof VarInsnNode) || !isVarInsnInvariant(varianceMap, currentEntry, (VarInsnNode)ain)){
				return false;
			}
		}
		return true;
	}
	
	private void addToVariantLocals(HashMap<Integer, LoopEntry> map, int index, LoopEntry cur){
		LoopEntry prev = map.get(index);
		if(prev == null){
			map.put(index, cur);
		}else if(prev.contains(cur.entry)){
			//prev is some parent of cur
			map.put(index, cur);
		}
	}

	public void optimize(){
		for(MethodInformation mi : dm.methodInformations.values()){
			//if(!mi.mn.name.equals("contains")) continue;

			HashMap<BasicBlock, LoopEntry> loopRoots = mi.loopRoots;

			HashSet<BasicBlock> searched = new HashSet<>();
			HashMap<Integer, LoopEntry> variantLocals = new HashMap<>();
			HashMap<LoopEntry, ArrayList<Temporary>> invariantTemporaries = new HashMap<>();

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
								addToVariantLocals(variantLocals, vin.var, entry);
								break;
							case Opcodes.IINC:
								IincInsnNode iin = (IincInsnNode) ain;
								addToVariantLocals(variantLocals, iin.var, entry);
								break;
							case Opcodes.IASTORE:
							case Opcodes.AASTORE:
							case Opcodes.LASTORE:
							case Opcodes.DASTORE:
							case Opcodes.FASTORE:
							case Opcodes.SASTORE:
							case Opcodes.CASTORE:
							case Opcodes.BASTORE:
								Temporary t = mi.temporaries.get(ain);
								//t is the arrayref of the store
								//it could be a "premature" Phi tho
								if(t instanceof PhiTemporary){
									t = resolvePrematurePhiTemporary((PhiTemporary)t);
								}
								if(t instanceof ArrayReferenceTemporary){ //i.e. no longer Phi
									ArrayReferenceTemporary art = (ArrayReferenceTemporary) mi.temporaries.get(ain);
									if(art.getDeclaration() instanceof VarInsnNode){
										vin = (VarInsnNode) art.getDeclaration();
										addToVariantLocals(variantLocals, vin.var, entry);
									}else{
										//try to lookup in local variables
										Temporary[] locals = mi.statePerInsn.get(ain).val2;
										for(int i = 0; i < locals.length; i++){
											if(art.equals(locals[i])){
												addToVariantLocals(variantLocals, i, entry);
											}
										}
									}
								}
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
							if(t == null) continue;
							//System.out.println("Instruction " + ain.getIndex() + ": " + Textifier.OPCODES[ain.getOpcode()] + " -> " + t);
							if(isInvariant(mi, variantLocals, entry, t)){
								ArrayList<Temporary> set = invariantTemporaries.get(entry);
								if(set == null){
									set = new ArrayList<>();
									invariantTemporaries.put(entry, set);
								}
								//if(!set.contains(t)){
								set.add(t);
								//}
							}

						}
					}
				}

				if(invariantTemporaries.size() > 0){
					System.out.println(dm.methodNodeToOwnerMap.get(mi.mn).name + "." + mi.mn.name + mi.mn.desc + ":");
					System.out.println("variantLocals = ");
					for(int key : variantLocals.keySet()){
						System.out.println("\tlocal_" + key + " is set in LoopEntry.entry = " + variantLocals.get(key).entry);
					}
					System.out.println("invariantTemporaries = ");
					for(LoopEntry key : invariantTemporaries.keySet()){
						System.out.println("\tEntry to move to = " + key.entry.toString());
						for(Temporary invariant : invariantTemporaries.get(key)){
							System.out.println("\t\tInstruction " + invariant.getDeclaration().getIndex() + " (" + Textifier.OPCODES[invariant.getDeclaration().getOpcode()] + "): " + invariant);
						}
						//+ "; t = " + invariantTemporaries.get(key) + "; at instruction " + invariantTemporaries.get(key));
					}
					
					System.out.println();
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
		}
	}



}
