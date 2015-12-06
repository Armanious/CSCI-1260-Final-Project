package org.armanious.csci1260.obfuscation.todo;

import java.util.ArrayList;
import java.util.Random;

import org.armanious.csci1260.DataManager;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

public class ExcecptionBlockObfuscator {
	
	private static final Random r = new Random();
	
	private final DataManager dm;
	
	public ExcecptionBlockObfuscator(DataManager dm){
		this.dm = dm;
	}
	
	private static final int NON_OVERLAPPING = 0;
	private static final int LATTER_WITHIN_FORMER = 1;
	private static final int FORMER_WITHIN_LATTER = 2;
	private static final int ALREADY_ILLEGAL_OVERLAP = 3;
	
	private static int getRelation(TryCatchBlockNode tcbn1, TryCatchBlockNode tcbn2){
		final int start1 = tcbn1.start.getIndex();
		final int end1 = tcbn1.end.getIndex();
		final int start2 = tcbn2.start.getIndex();
		final int end2 = tcbn2.end.getIndex();
		
		if(end1 < start2 || end2 < start1){
			return NON_OVERLAPPING;
		}else if(start1 < start2 && end1 > end2){
			return LATTER_WITHIN_FORMER;
		}else if(start2 < start1 && end2 > end1){
			return FORMER_WITHIN_LATTER;
		}
		return ALREADY_ILLEGAL_OVERLAP;
	}
	
	
	private static LabelNode generateRandomLabelNode(InsnList insns, AbstractInsnNode l1, AbstractInsnNode l2){
		int toSkip = r.nextInt(l2.getIndex() - l1.getIndex());
		AbstractInsnNode ain = l1;
		while(toSkip-- > 0){
			ain = ain.getNext();
		}
		final LabelNode ln = new LabelNode(new Label());
		insns.insertBefore(ain, ln);
		return ln;
	}
	
	private int countModified = 0;
	
	public void obfuscate(){
		for(ClassNode cn : dm.classes){
			for(MethodNode mn : cn.methods){
				if(mn.tryCatchBlocks != null && mn.tryCatchBlocks.size() >= 2){
					
					ArrayList<Integer> possibilities = new ArrayList<>();
					for(int i = 0; i < mn.tryCatchBlocks.size(); i++){
						possibilities.add(i);
					}
					int indexToMessWith1 = r.nextInt(possibilities.size());
					TryCatchBlockNode tcbn1 = mn.tryCatchBlocks.get(possibilities.remove(indexToMessWith1));

					int indexToMessWith2 = r.nextInt(possibilities.size());
					TryCatchBlockNode tcbn2 = mn.tryCatchBlocks.get(possibilities.remove(indexToMessWith2));
					
					switch(getRelation(tcbn1, tcbn2)){
					case NON_OVERLAPPING:
						if(tcbn1.end.getIndex() < tcbn2.start.getIndex()){
							//tcbn1 is before tcbn2
							tcbn1.end = generateRandomLabelNode(mn.instructions, tcbn2.start, tcbn2.end);
						}else{
							//tcbn2 is before tcbn1
							tcbn2.end = generateRandomLabelNode(mn.instructions, tcbn1.start, tcbn1.end);
						}
						break;
					case FORMER_WITHIN_LATTER:
						TryCatchBlockNode tmp = tcbn1;
						tcbn1 = tcbn2;
						tcbn2 = tmp;
						//swap and falltrhough; less repeated code
					case LATTER_WITHIN_FORMER:
						//s1        s1
						//  s2         s2
						//  e2 ==>  e1
						//e1           e2
						tcbn2.end = generateRandomLabelNode(mn.instructions, tcbn1.start, mn.instructions.getLast());
						break;
					case ALREADY_ILLEGAL_OVERLAP:
					default:
						break;
					}
					
					countModified++;
					
				}
			}
		}
		System.out.println("Obfuscated the exception handler blocks in " + countModified + " methods.");
	}

}
