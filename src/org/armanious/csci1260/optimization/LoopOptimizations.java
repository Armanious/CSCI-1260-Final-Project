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
import org.armanious.csci1260.Tuple;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.Textifier;

//make sure you do NOT run LoopOptimizations twice; it will break it
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
		if(t instanceof MethodInvocationTemporary && ((MethodInvocationTemporary)t).hasSideEffects()){
			return false; //assume all methods have side effects for now
		}
		if(t.getContiguousBlockSorted() == null) return false;
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
		int numLoopInvariants = 0;
		for(MethodInformation mi : dm.methodInformations.values()){
			//if(!mi.mn.name.equals("fundamentalLoopTest")) continue;
			//System.out.println("\n" + mi.mn.name);
			int startingNumLoopInvariants = numLoopInvariants;

			HashMap<BasicBlock, LoopEntry> loopRoots = mi.loopRoots;


			for(BasicBlock loopRootEntry : loopRoots.keySet()){

				HashMap<LoopEntry, ArrayList<Temporary>> invariantTemporaries = new HashMap<>();
				HashMap<Integer, LoopEntry> variantLocals = new HashMap<>();
				HashSet<BasicBlock> searched = new HashSet<>();
				
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


				//System.out.println(mi.mn.name + " variantLocals: " + variantLocals);

				searched.clear();
				toAnalyze = toAnalyzeClone;

				//ArrayList<Tuple<Temporary, LoopEntry>> whereToRelocateInvariants = new ArrayList<>();

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
								ArrayList<Temporary> list = invariantTemporaries.get(entry);
								if(list == null){
									list = new ArrayList<>();
									invariantTemporaries.put(entry, list);
								}
								list.add(t);

								/*boolean shouldSet = true;
								Tuple<Temporary, LoopEntry> toReplace = null;
								for(Tuple<Temporary, LoopEntry> v : whereToRelocateInvariants){
									if(v.val1.equals(t)){
										if(entry.contains(v.val2.entry)){
											toReplace = v;
											shouldSet = true;
										}else{
											shouldSet = false;
										}
										break;
									}
								}
								if(shouldSet){
									if(toReplace != null){
										whereToRelocateInvariants.remove(toReplace);
									}
									whereToRelocateInvariants.add(new Tuple<>(t, entry));
								}*/

							}

						}
					}
				}

				/*if(invariantTemporaries.size() > 0){
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
							ArrayList<AbstractInsnNode> block = invariant.getContiguousBlock();
							System.out.println("\t\t\t" + block.get(0).getIndex() + " - " + block.get(block.size() - 1).getIndex());
						}
						//+ "; t = " + invariantTemporaries.get(key) + "; at instruction " + invariantTemporaries.get(key));
					}
					System.out.println(whereToRelocateInvariants);

					System.out.println();
				}*/

				if(invariantTemporaries.size() > 0){

					HashSet<Tuple<Temporary, LoopEntry>> insertionChecker = new HashSet<>();

					ArrayList<Tuple<Temporary, BasicBlock>> invariantRedefinitionLocations = new ArrayList<>();

					for(LoopEntry loop : invariantTemporaries.keySet()){
						ArrayList<Temporary> invariantInThisLoop = invariantTemporaries.get(loop);

						for(int i = 0; i < invariantInThisLoop.size(); i++){
							Temporary invariant = invariantInThisLoop.get(i);

							int indexOfLocalVariable = -1;

							for(int j = 0; j < invariantRedefinitionLocations.size(); j++){
								if(invariant.equals(invariantRedefinitionLocations.get(j).val1)){
									if(loop.contains(invariantRedefinitionLocations.get(j).val2)){
										//if this loop is some parent of the loop currently defining the invariant
										//move the invariant as far up the loop chain as possible
										invariantRedefinitionLocations.set(j, new Tuple<>(invariant, loop.entry));
									}
									indexOfLocalVariable = mi.mn.maxLocals + j;
									break;
								}
							}
							if(indexOfLocalVariable == -1){
								//is not yet defined
								indexOfLocalVariable = mi.mn.maxLocals + invariantRedefinitionLocations.size();
								invariantRedefinitionLocations.add(new Tuple<>(invariant, loop.entry));
							}

							replace(mi.mn.instructions, invariant.getContiguousBlockSorted(), new VarInsnNode(DataManager.getLoadOpcode(invariant.getType()), indexOfLocalVariable));

						}
						
						//make sure we have the temporaries that were first defined still defined first, even if they are invariant
						

						/*for(Temporary invariant : invariantTemporaries.get(loop)){
							ArrayList<AbstractInsnNode> block = invariant.getContiguousBlock();




							Tuple<Temporary, LoopEntry> tuple = null;
							int localVarOffset = -1;
							for(int localVarOffsetPossibility = 0; localVarOffsetPossibility < whereToRelocateInvariants.size(); localVarOffsetPossibility++){
								Tuple<Temporary, LoopEntry> tle = whereToRelocateInvariants.get(localVarOffsetPossibility);
								if(tle.val1.equals(invariant)){
									tuple = tle;
									localVarOffset = localVarOffsetPossibility;
									break;
								}
							}
							if(localVarOffset == -1){
								System.err.println("Invariant with null parent");
								continue;
							}

							int indexOfLocalVariable = mi.mn.maxLocals + localVarOffset;

							if(insertionChecker.add(tuple)){
								BasicBlock entry = tuple.val2.entry;
								AbstractInsnNode lastWhichIsGoto = entry.getLastInsnInBlock();
								System.err.println("Inserting " + tuple.val1 + " at the end of " + entry);
								insertBefore(mi.mn.instructions, lastWhichIsGoto, block, new VarInsnNode(DataManager.getStoreOpcode(invariant.getType()), indexOfLocalVariable),
										new VarInsnNode(DataManager.getLoadOpcode(invariant.getType()), indexOfLocalVariable));

							}else{
								replace(mi.mn.instructions, block, new VarInsnNode(DataManager.getLoadOpcode(invariant.getType()), indexOfLocalVariable));
							}
						//}*/
					};
					
					for(Tuple<Temporary, BasicBlock> toInsertClone : invariantRedefinitionLocations){
						int offsetOfLocalVariable = invariantRedefinitionLocations.indexOf(toInsertClone);
						int indexOfLocalVariable = mi.mn.maxLocals + offsetOfLocalVariable;
						insertBefore(mi.mn.instructions, toInsertClone.val2, toInsertClone.val1.getContiguousBlockSorted(), new VarInsnNode(DataManager.getStoreOpcode(toInsertClone.val1.getType()), indexOfLocalVariable));
					}
					numLoopInvariants += invariantRedefinitionLocations.size();
					mi.mn.maxLocals += invariantRedefinitionLocations.size();
				}

			}

			if(numLoopInvariants > startingNumLoopInvariants){
				mi.recompute(); //recompute at the end of any modifications
			}
		}

		System.out.println("Found and reduced " + numLoopInvariants + " loop invariants.");

	}

	private void insertBefore(InsnList list, BasicBlock endOfWhichBlock, ArrayList<AbstractInsnNode> block, VarInsnNode storeInsn) {
		AbstractInsnNode where = endOfWhichBlock.getLastInsnInBlock();
		switch(where.getOpcode()){
		case Opcodes.IFNONNULL:
		case Opcodes.IFNULL:
		case Opcodes.IFEQ:
		case Opcodes.IFNE:
		case Opcodes.IFGE:
		case Opcodes.IFGT:
		case Opcodes.IFLE:
		case Opcodes.IFLT:
			where = where.getPrevious();
			break;
		case Opcodes.IF_ICMPEQ:
		case Opcodes.IF_ICMPNE:
		case Opcodes.IF_ICMPLT:
		case Opcodes.IF_ICMPGE:
		case Opcodes.IF_ICMPGT:
		case Opcodes.IF_ICMPLE:
		case Opcodes.IF_ACMPEQ:
		case Opcodes.IF_ACMPNE:
			where = where.getPrevious().getPrevious();
			break;
		case Opcodes.GOTO:
			break;
		}
		//list.get(0);
		//System.err.println("Inserting " + block.get(0).getIndex() + " - " + block.get(block.size() - 1).getIndex() + " + " + Textifier.OPCODES[storeInsn.getOpcode()] + " " + storeInsn.var + " before Instruction " + where.getIndex());
		InsnList il = new InsnList();
		for(AbstractInsnNode ain : block){
			//list.remove(ain); they have already been removed
			il.add(ain);
			//System.out.println(Textifier.OPCODES[ain.getOpcode()]);
		}
		il.add(storeInsn);
		list.insertBefore(where, il);
	}
	
	private void insertBeforeLegacy(InsnList list, AbstractInsnNode where, ArrayList<AbstractInsnNode> block, VarInsnNode storeInsn, VarInsnNode loadInsn) {
		list.get(0);
		System.err.println("Inserting " + block.get(0).getIndex() + " - " + block.get(block.size() - 1).getIndex() + " + " + Textifier.OPCODES[storeInsn.getOpcode()] + " " + storeInsn.var + " before Instruction " + where.getIndex());
		list.insertBefore(block.get(0), loadInsn);
		InsnList il = new InsnList();
		for(AbstractInsnNode ain : block){
			list.remove(ain);
			il.add(ain);
		}
		il.add(storeInsn);
		list.insertBefore(where, il);
	}

	private void replace(InsnList list, ArrayList<AbstractInsnNode> block, VarInsnNode varInsnNode) {
		//list.get(0);
		//System.err.println("Replacing " + block.get(0).getIndex() + " - " + block.get(block.size() - 1).getIndex() + " with " + Textifier.OPCODES[varInsnNode.getOpcode()] + " " + varInsnNode.var);
		list.insertBefore(block.get(0), varInsnNode);
		for(AbstractInsnNode ain : block){
			list.remove(ain);
		}
	}


}
