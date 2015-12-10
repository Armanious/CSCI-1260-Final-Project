package org.armanious.csci1260.optimization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

import org.armanious.csci1260.BasicBlock;
import org.armanious.csci1260.DataManager;
import org.armanious.csci1260.LoopEntry;
import org.armanious.csci1260.MethodInformation;
import org.armanious.csci1260.Tuple;
import org.armanious.csci1260.temporaries.ArrayReferenceTemporary;
import org.armanious.csci1260.temporaries.ConstantTemporary;
import org.armanious.csci1260.temporaries.FieldTemporary;
import org.armanious.csci1260.temporaries.MethodInvocationTemporary;
import org.armanious.csci1260.temporaries.ParameterTemporary;
import org.armanious.csci1260.temporaries.PhiTemporary;
import org.armanious.csci1260.temporaries.Temporary;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.VarInsnNode;

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

	private boolean isVarInsnInvariant(HashMap<Integer, LoopEntry> varianceMap, LoopEntry currentEntry, VarInsnNode vin){
		final LoopEntry loop = varianceMap.get(vin.var);
		if(loop != null && (!loop.contains(currentEntry.entry) || loop == currentEntry)){
			return false;
		}
		return true;
	}

	private boolean isInvariant(MethodInformation mi, HashMap<Integer, LoopEntry> varianceMap, LoopEntry currentEntry, Temporary t){
		if(t instanceof PhiTemporary){
			return false;
			//t = resolvePrematurePhiTemporary((PhiTemporary)t);
		}
		if(t instanceof MethodInvocationTemporary && ((MethodInvocationTemporary)t).hasSideEffects()){
			return false;
		}
		if(t instanceof FieldTemporary && ((FieldTemporary)t).isVolatile()){
			return false;
		}
		if(t.getContiguousBlockSorted() == null){
			//System.err.println("Ruling out invariance: " + t + " at instruction " + t.getDeclaration().getIndex() + " (" + Textifier.OPCODES[t.getDeclaration().getOpcode()] + ")");
			return false;
		}
		if((t.getDeclaration().getOpcode() >= Opcodes.IASTORE && t.getDeclaration().getOpcode() <= Opcodes.SASTORE)){// ||
			//(t.getDeclaration().getOpcode() >= Opcodes.ILOAD && t.getDeclaration().getOpcode() <= Opcodes.ALOAD)){
			return false;
		}
		if(t.getDeclaration() instanceof VarInsnNode){
			return isVarInsnInvariant(varianceMap, currentEntry, (VarInsnNode)t.getDeclaration());
		}
		ArrayList<Temporary> criticalTemporaries = t.getCriticalTemporaries();
		if(criticalTemporaries == null || criticalTemporaries.size() == 0) return false;
		for(Temporary critTemp : criticalTemporaries){
			if(!isInvariant(mi, varianceMap, currentEntry, critTemp)){
				return false;
			}
			/*final AbstractInsnNode ain = critTemp.getDeclaration();
			if(ain == null || (ain instanceof VarInsnNode && !isVarInsnInvariant(varianceMap, currentEntry, (VarInsnNode)ain))){
				return false;
			}*/
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
	
	private int localsOffset(ArrayList<Tuple<Temporary, BasicBlock>> invariantRedefinitionLocations, int endIndexExclusive){
		int offset = 0;
		for(int i = 0; i < endIndexExclusive; i++){
			offset += invariantRedefinitionLocations.get(i).val1.getType().getSize();
		}
		return offset;
	}

	public void optimize(){
		int numLoopInvariants = 0;
		for(MethodInformation mi : dm.methodInformations.values()){
			//if(!mi.mn.name.equals("loopTest")) continue;
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
				//toAnalyze.push(loopRoot); what a bug
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
						if(!searched.add(block)) continue; //ensures that we don't reanalyze
						//blocks of inner loops (current block's loopentry's children's blocks)
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
									break;
									//t = resolvePrematurePhiTemporary((PhiTemporary)t);
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
							if(ain.getOpcode() == -1) continue;
							//System.out.println(Textifier.OPCODES[ain.getOpcode()]);
							Temporary t = mi.temporaries.get(ain);
							if(t == null) continue;
							//System.out.println("Instruction " + ain.getIndex() + ": " + Textifier.OPCODES[ain.getOpcode()] + " -> " + t);
							if(t.getDeclaration() instanceof VarInsnNode ||
									t instanceof ConstantTemporary ||
									t instanceof ParameterTemporary) continue;
							if(isInvariant(mi, variantLocals, entry, t)){
								//System.out.println("Instruction " + ain.getIndex() + ": " + Textifier.OPCODES[ain.getOpcode()] + " -> " + t);


								ArrayList<Temporary> list = invariantTemporaries.get(entry);
								if(list == null){
									list = new ArrayList<>();
									invariantTemporaries.put(entry, list);
									list.add(t);
								}else{
									for(Temporary otherInvariant : list){
										if(otherInvariant.getCriticalTemporaries().contains(t)){
											break;
										}
									}
									list.add(t);
									//System.out.println("  Added " + t);
									for(Temporary crit : t.getCriticalTemporaries()){
										if(list.remove(crit)){
											//System.out.println("Removed " + crit);
										}
									}
								}
							}

						}
					}
				}

				//System.out.println("BEFORE: ");
				//printRegion(mi.mn, mi.mn.instructions.getFirst(), mi.mn.instructions.getLast());

				if(invariantTemporaries.size() > 0){

					ArrayList<Tuple<Temporary, BasicBlock>> invariantRedefinitionLocations = new ArrayList<>();

					for(LoopEntry loop : invariantTemporaries.keySet()){
						ArrayList<Temporary> invariantInThisLoop = invariantTemporaries.get(loop);

						for(int i = 0; i < invariantInThisLoop.size(); i++){
							Temporary invariant = invariantInThisLoop.get(i);
							
							int indexOfLocalVariable = -1;
							
							for(int j = 0; j < invariantRedefinitionLocations.size(); j++){
								if(invariant.equals(invariantRedefinitionLocations.get(j).val1)){
									BasicBlock otherLoopsEntry = invariantRedefinitionLocations.get(j).val2;
									if(loop.contains(invariantRedefinitionLocations.get(j).val2)){
										//if this loop is some parent of the loop currently defining the invariant
										//move the invariant as far up the loop chain as possible
										invariantRedefinitionLocations.set(j, new Tuple<>(invariant, loop.entry));
									}else if(loop.parent == mi.loops.get(otherLoopsEntry).parent){
										LoopEntry other = mi.loops.get(otherLoopsEntry);
										if(loop.nthSibling < other.nthSibling){
											//loop executes first
											invariantRedefinitionLocations.set(j, new Tuple<>(invariant, loop.entry));
										}else{
											//other executes first
											//don't need to change it
										}
									}
									
									indexOfLocalVariable = mi.mn.maxLocals + localsOffset(invariantRedefinitionLocations, j);
									break;
								}
							}
							if(indexOfLocalVariable == -1){
								//is not yet defined
								indexOfLocalVariable = mi.mn.maxLocals + localsOffset(invariantRedefinitionLocations, invariantRedefinitionLocations.size());
								invariantRedefinitionLocations.add(new Tuple<>(invariant, loop.entry));
							}
							replace(mi.mn.instructions, invariant.getContiguousBlockSorted(), new VarInsnNode(DataManager.getLoadOpcode(invariant.getType()), indexOfLocalVariable));

						}
					};
					//System.out.println("\nAFTER (BUT BEFORE INSERTIONS):");

					//printRegion(mi.mn, mi.mn.instructions.getFirst(), mi.mn.instructions.getLast());
					//System.out.println();

					for(Tuple<Temporary, BasicBlock> toInsertClone : invariantRedefinitionLocations){
						int offsetOfLocalVariable = localsOffset(invariantRedefinitionLocations, invariantRedefinitionLocations.indexOf(toInsertClone));
						int indexOfLocalVariable = mi.mn.maxLocals + offsetOfLocalVariable;
						System.out.println("Inserting " + toInsertClone.val1 + " into local variable " + indexOfLocalVariable);

						insertBefore(mi.mn.instructions, toInsertClone.val2, toInsertClone.val1.getContiguousBlockSorted(), new VarInsnNode(DataManager.getStoreOpcode(toInsertClone.val1.getType()), indexOfLocalVariable));
						//System.out.println("Loop invariant at " + dm.methodNodeToOwnerMap.get(mi.mn).name + "." + mi.mn.name + mi.mn.desc + ": " + toInsertClone.val1 + ", now stored in " + indexOfLocalVariable + " at the beginning of " + toInsertClone.val2);
					}
					numLoopInvariants += invariantRedefinitionLocations.size();
					mi.mn.maxLocals += localsOffset(invariantRedefinitionLocations, invariantRedefinitionLocations.size());

					//System.out.println("\nAFTER INSERTIONS:");

					//printRegion(mi.mn, mi.mn.instructions.getFirst(), mi.mn.instructions.getLast());;
					//System.out.println("\n\n");
				}

			}

			if(numLoopInvariants > startingNumLoopInvariants){
				mi.recompute(); //recompute at the end of any modifications
			}
		}

		System.out.println("Found and reduced " + numLoopInvariants + " loop invariants.");

	}

	private static void insertBefore(InsnList list, BasicBlock endOfWhichBlock, ArrayList<AbstractInsnNode> block, VarInsnNode storeInsn) {
		AbstractInsnNode where = endOfWhichBlock.getLastInsnInBlock();
		while(where.getOpcode() == -1) where = where.getPrevious();
		//where = where.getPrevious();
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

	//copied to ConstantFolder
	private static void replace(InsnList list, ArrayList<AbstractInsnNode> block, AbstractInsnNode insn) {
		//list.get(0);
		//System.err.println("Replacing " + block.get(0).getIndex() + " - " + block.get(block.size() - 1).getIndex() + " with " + Textifier.OPCODES[insn.getOpcode()] + " " + ((VarInsnNode)insn).var);
		list.insertBefore(block.get(0), insn);
		for(AbstractInsnNode ain : block){
			list.remove(ain);
		}
	}


}
