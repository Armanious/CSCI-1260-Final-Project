package org.armanious.csci1260.obfuscation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.armanious.csci1260.DataManager;
import org.armanious.csci1260.DataManager.BasicBlock;
import org.armanious.csci1260.DataManager.ConstructorTemporary;
import org.armanious.csci1260.DataManager.MethodInformation;
import org.armanious.csci1260.DataManager.MethodInvocationTemporary;
import org.armanious.csci1260.DataManager.Temporary;
import org.armanious.csci1260.JavaStack;
import org.armanious.csci1260.Tuple;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.Textifier;

public class StackManipulatorBeta implements Opcodes {

	private final DataManager dm;

	public StackManipulatorBeta(DataManager dm){
		this.dm = dm;
	}

	private BasicBlock getBlockInstructionIsIn(AbstractInsnNode a, MethodInformation ctx){
		BasicBlock block;
		do {
			block = ctx.blocks.get(a);
			a = a.getPrevious();
		}while(block == null && a != null);
		return block;
	}

	public void obfuscate(){
		int totalCount = 0;
		for(MethodInformation mi : dm.methodInformations.values()){
			String name = dm.methodNodeToOwnerMap.get(mi.mn).name + "." + mi.mn.name + mi.mn.desc;

			boolean printed = false;
			HashSet<Temporary> tmps = mi.temporaries;
			TreeMap<ArrayList<AbstractInsnNode>, Temporary> goodTargets = new TreeMap<>((a1,a2)->a1.size()-a2.size());
			for(Temporary tmp : tmps){
				ArrayList<AbstractInsnNode> block = tmp.getContiguousBlock();
				if(block != null && block.size() >= 3){
					goodTargets.put(block, tmp);
				}
			}

			@SuppressWarnings("unchecked")
			Entry<ArrayList<AbstractInsnNode>, Temporary>[] entries = (Entry<ArrayList<AbstractInsnNode>, Temporary>[]) goodTargets.entrySet().toArray(new Entry[goodTargets.size()]);
			Set<Entry<ArrayList<AbstractInsnNode>, Temporary>> toRemove = new HashSet<>();

			for(int i = 0; i < entries.length; i++){
				final ArrayList<AbstractInsnNode> list = entries[i].getKey();
				final int start = list.get(0).getIndex();
				final int end = list.get(list.size() - 1).getIndex();
				for(int j = i + 1; j < entries.length; j++){
					final ArrayList<AbstractInsnNode> list2 = entries[j].getKey();
					final int start2 = list2.get(0).getIndex();
					final int end2 = list2.get(list2.size() - 1).getIndex();
					if((start2 >= start && start2 <= end) || (end2 >= start || end2 <= end)){
						toRemove.add(entries[j]);
					}
				}
			}

			for(Entry<ArrayList<AbstractInsnNode>, Temporary> removing : toRemove){
				goodTargets.remove(removing.getKey());
			}

			for(Entry<ArrayList<AbstractInsnNode>, Temporary> target : goodTargets.entrySet()){
				//if(target.getValue() instanceof FieldTemporary){
				if(!printed){
					printed = true;
					System.out.println("\n\n" + name);
				}
				totalCount++;
				System.out.println("Target: " + target.getValue());
				//manipulateStackSwappingTemporaryOperands(mi, target.getValue(), target.getValue().getCriticalTemporaries(), target.getKey());
				System.err.println(target.getValue());
				manipulateStackBeforeAndAfterTemporary(mi, target.getValue(), target.getValue().getCriticalTemporaries(), target.getKey());
				//}
			}

		}

		System.out.println("Total count of targets: " + totalCount);
	}

	private static void add(InsnList list, int...opcodes){
		for(int opcode : opcodes){
			list.add(new InsnNode(opcode));
		}
	}

	int i = 0;
	private static final Random r = new Random();
	private static Tuple<InsnList, Tuple<Integer, Integer>> generateInsertion(){
		//TODO copy random parts of code from other parts of the project?
		final InsnList toReturn = new InsnList();
		final int sizeOfReturn = r.nextInt(3);
		int maxStackIncrement;
		switch(sizeOfReturn){
		case 0:
			add(toReturn, ACONST_NULL, DUP, POP2);
			maxStackIncrement = 2;
			break;
		case 1:
			add(toReturn, ICONST_M1, ICONST_2, IMUL);
			maxStackIncrement = 2;
			break;
		case 2:
			add(toReturn, DCONST_1, DCONST_0, DADD);
			maxStackIncrement = 4;
			break;
		default:
			throw new RuntimeException();
		}
		return new Tuple<>(toReturn, new Tuple<>(sizeOfReturn, maxStackIncrement));
	}

	private void manipulateStackBeforeAndAfterTemporary(MethodInformation mi, Temporary t, ArrayList<Temporary> criticalTemporaries, ArrayList<AbstractInsnNode> block){
		//guaranteed that: block contains all critical temporaries, contains t, and is contiguous
				//unknown bug: block.last != t.declaration; fix: use block.last instead of t.declaration
					//when inserting instructions to restore stack
		// that criticalTemporaries is sorted in the order they are pushed onto the stack
		// that block is sorted in the order the instructions are executed
		
		final Tuple<InsnList, Tuple<Integer, Integer>> toInsertBefore = generateInsertion();
		final int sizeOfResult = t.getType().getSize();
		
		final InsnList toAddAfter = new InsnList();
		final int strategy = toInsertBefore.val2.val1 * 10 + sizeOfResult;
		//sizeOfReturn * 10 + sizeOfResult; sizeOfReturn is first digit; result is second
		

		System.err.println("Stack at end of t: " + mi.statePerInsn.get(t.getDeclaration()).val1);
		System.err.println("\n");
		
		
		switch(strategy){
		case 0://00
		case 1://01
		case 2://02
			break;
		case 10:
			add(toAddAfter, POP);
			break;
		case 11:
			add(toAddAfter, DUP_X1, POP2);
			break;
		case 12:
			add(toAddAfter, DUP2_X1, POP2, POP);
			break;
		case 20:
			add(toAddAfter, POP2);
			break;
		case 21:
			add(toAddAfter, DUP_X2, POP, POP2);
			break;
		case 22:
			add(toAddAfter, DUP2_X2, POP2, POP2);
			break;
		}
		
		final JavaStack stackBefore = mi.statePerInsn.get(block.get(0)).val1;
		if(toInsertBefore.val2.val2 > mi.mn.maxStack - stackBefore.size()){
			mi.mn.maxStack += toInsertBefore.val2.val2 - (mi.mn.maxStack - stackBefore.size());
		}
		
		final JavaStack stackAfter = mi.statePerInsn.get(block.get(block.size() - 1)).val1;
		if(toInsertBefore.val2.val2 > mi.mn.maxStack - stackAfter.size()){
			mi.mn.maxStack += toInsertBefore.val2.val2 - (mi.mn.maxStack - stackBefore.size());
		}
		
		mi.mn.instructions.insertBefore(block.get(0), toInsertBefore.val1);
		mi.mn.instructions.insert(block.get(block.size() - 1), toAddAfter);
		
		System.err.println("Max stack: " + mi.mn.maxStack);
	}
	
	private int sizeBetweenInclusive(ArrayList<Temporary> critTemporaries, int f, int s){
		int first = f < s ? f : s;
		int second = f < s ? s : f;
		
		int size = 0;
		
		for(int i = first; i <= second; i++){
			size += critTemporaries.get(i).getType().getSize();
		}
		
		return size;
	}
	
	private static final int[][][][][] STACK_RESTORATION_OPS = {
			//size == 0
			{},
			
			//size == 1
			{},
			
			//size == 2
			{
				//S1 == 0
				{},
				//S1 == 1
				{
					//S2 == 0
					{},
					
					//S2 == 1
					{
						//variation 1 on (2,1,1)
						{SWAP},
						//variation 2 on (2,1,1)
						{DUP_X1, POP}
					}
				},
			},
			
			//size == 3
			{
				//S1 == 0
				{},
				//S1 == 1
				{
					//S2 == 0
					{},
					//S2 == 1
					{
						//MID == 1
						{SWAP, DUP2_X2, POP2},
						{SWAP, DUP2_X2, POP, POP},
						{DUP_X1, POP, DUP2_X2, POP2},
						{DUP_X1, POP, DUP2_X2, POP, POP},
						
						{DUP_X2, POP, SWAP},
						{DUP_X2, POP, DUP_X1, POP}
					},
					//S2 == 2
					{
						{DUP2_X1, POP2, POP}
					}
				},
				//S1 == 2
				{
					//S2 == 0
					{},
					//S2 == 1
					{
						{DUP_X2, POP, POP2}
					}
				},
			},
			
			//size == 4
			{
				//S1 == 0
				{},
				//S1 == 1
				{
					//S2 == 0
					{},
					//S2 == 1
					null, //(4, 1, 1) is special cased
					//S2 == 2
					{
						//MID == 1
						{DUP2_X2, POP2, SWAP},
						{DUP2_X2, POP2, DUP_X1, POP}
					},
				},
				//S1 == 2
				{
					//S2 == 0
					{},
					//S2 == 1
					{
						{SWAP, DUP2_X2, POP2},
						{SWAP, DUP2_X2, POP, POP},
						{DUP_X1, POP, DUP2_X2, POP2},
						{DUP_X1, POP, DUP2_X2, POP, POP}
					},
					//S2 == 2
					{
						{DUP2_X2, POP2}
					}
				}
			},
	};
	
	private int getMaxStackIncrement(int[] ops){
		int max = 0;
		int cur = 0;
		for(int op : ops){
			switch(op){
			case POP:
				cur--;
				break;
			case POP2:
				cur -= 2;
				break;
			case DUP:
			case DUP_X1:
			case DUP_X2:
				cur++;
				break;
			case DUP2_X1:
			case DUP2_X2:
			case DUP2:
				cur += 2;
			case SWAP:
			default:
			}
			if(cur > max){
				max = cur;
			}
		}
		return max;
	}
	
	//copied from RedundantComputationRemover
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
	
	private static String blockToText(ArrayList<AbstractInsnNode> block){
		final StringBuilder sb = new StringBuilder();
		for(AbstractInsnNode ain : block){
			sb.append(ain.getIndex()).append(", ");
		}
		return sb.substring(0, sb.length() - 2);
	}
	
	private static void swap(List<AbstractInsnNode> L1, List<AbstractInsnNode> L2){
		final AbstractInsnNode BL1 = L1.get(0).getPrevious();
		final AbstractInsnNode AL1 = L1.get(L1.size() - 1).getNext();
		final AbstractInsnNode BL2 = L2.get(0).getPrevious();
		final AbstractInsnNode AL2 = L2.get(L2.size() - 1).getNext();

		if(AL1 == L2.get(0)){
			//contiguous
			L2.get(0).setPrev(BL1);
			BL1.setNext(L2.get(0));
			
			L1.get(L1.size() - 1).setNext(AL2);
			AL2.setPrev(L1.get(L1.size() - 1));
			
			L2.get(L2.size() - 1).setNext(L1.get(0));
			L1.get(0).setPrev(L2.get(L2.size() - 1));

		}else{
			L1.get(0).setPrev(BL2);
			BL2.setNext(L1.get(0));
			L1.get(L1.size() - 1).setNext(AL2);
			AL2.setPrev(L1.get(L1.size() - 1));

			L2.get(0).setPrev(BL1);
			BL1.setNext(L2.get(0));
			L2.get(L2.size() - 1).setNext(AL1);
			AL1.setPrev(L2.get(L2.size() - 1));
		}
	}
	
	private void manipulateStackSwappingTemporaryOperands(MethodInformation mi, Temporary t, ArrayList<Temporary> criticalTemporaries, ArrayList<AbstractInsnNode> block){

		
		if(t instanceof ConstructorTemporary) return;
		
		int numUnique = 0;
		outer: for(int i = 0; i < criticalTemporaries.size(); i++){
			for(int j = i + 1; j < criticalTemporaries.size(); j++){
				if(criticalTemporaries.get(i) == criticalTemporaries.get(j)){
					continue outer; //shallow equals; if i == j skip once; numUnique will
					//still be incremented when i reaches the value that j currently holds
				}
			}
			numUnique++;
		}
		if(numUnique <= 1) return;
		
		//at least 2 unique operands
		int firstIndex = r.nextInt(criticalTemporaries.size()); //index of first operand to swap
		int secondIndex; //index of second operand to swap
		Temporary first = criticalTemporaries.get(firstIndex);
		Temporary second;
		int sizeBetweenInclusive;
		while(true){
			secondIndex = r.nextInt(criticalTemporaries.size()); //more efficient way? oh well...
			if(secondIndex == firstIndex) continue;
			second = criticalTemporaries.get(secondIndex);
			if(first.getType().getSize() == 2 && second.getType().getSize() == 2){
				System.err.println("debug");
			}
			sizeBetweenInclusive = sizeBetweenInclusive(criticalTemporaries, firstIndex, secondIndex);
			if(sizeBetweenInclusive > 4) continue;
			break;
		}
		//size MUST be less than or equal to 4
		
		if(firstIndex > secondIndex){
			int tmp = firstIndex;
			Temporary TMP = first;
			firstIndex = secondIndex;
			first = second;
			secondIndex = tmp;
			second = TMP;
		}
		

		final ArrayList<AbstractInsnNode> F = first.getContiguousBlock();
		final ArrayList<AbstractInsnNode> S = second.getContiguousBlock();
		
		swap(F, S);
		
		//insert instructions to swap at runtime before declaration
		System.out.println("(" + sizeBetweenInclusive + ", " + first.getType().getSize() + ", " + second.getType().getSize() + ")");
		int[][] toRandomize = STACK_RESTORATION_OPS[sizeBetweenInclusive][first.getType().getSize()][second.getType().getSize()];
		if(toRandomize == null){
			if(criticalTemporaries.get(firstIndex + 1).getType().getSize() == 2){
				//1 MID, size 2
				final InsnList toAdd = new InsnList();
				add(toAdd, DUP_X2, POP, DUP2_X2, POP2, SWAP);
				toAdd.add(new VarInsnNode(getStoreOpcode(criticalTemporaries.get(firstIndex + 1).getType()), mi.mn.maxLocals));
				add(toAdd, DUP_X2, POP);
				toAdd.add(new VarInsnNode(getLoadOpcode(criticalTemporaries.get(firstIndex + 1).getType()), mi.mn.maxLocals));
				mi.mn.maxLocals++;
				mi.mn.maxStack += 2; //precomputed
				mi.mn.instructions.insert(F.get(F.size() - 1), toAdd);
			}else if(criticalTemporaries.get(firstIndex + 1).getType().getSize() == 1 &&
					criticalTemporaries.get(firstIndex + 2).getType().getSize() == 1){
				//2 MIDS, each size 1
				InsnList toAdd = new InsnList();
				if(r.nextBoolean()){
					toAdd.add(new InsnNode(SWAP));
				}else{
					toAdd.add(new InsnNode(DUP_X1));
					toAdd.add(new InsnNode(POP));
				}
				mi.mn.instructions.insertBefore(F.get(0), toAdd);		
				
				toAdd = new InsnList();
				if(r.nextBoolean()){
					toAdd.add(new InsnNode(SWAP));
				}else{
					toAdd.add(new InsnNode(DUP_X1));
					toAdd.add(new InsnNode(POP));
				}
				add(toAdd, DUP2_X2, POP2);
				if(r.nextBoolean()){
					toAdd.add(new InsnNode(SWAP));
				}else{
					toAdd.add(new InsnNode(DUP_X1));
					toAdd.add(new InsnNode(POP));
				}
				mi.mn.instructions.insert(F.get(F.size() - 1), toAdd);
				mi.mn.maxStack += 2; //precomputed
			}else{
				throw new RuntimeException("Cannot determing size of middle between stack indices " + firstIndex + " and " + secondIndex);
			}
			//special case: (4, 1, 1)
		}else{
			final InsnList toAdd = new InsnList();
			int[] selectedRandom = toRandomize[r.nextInt(toRandomize.length)];
			add(toAdd, selectedRandom);
			mi.mn.maxStack += getMaxStackIncrement(selectedRandom);
			mi.mn.instructions.insert(F.get(F.size() - 1), toAdd); //add after F; look at remember above
		}
		System.err.println("BOOGALOGA");
	}

	public void obfuscate_poop(){
		//TODO
		for(MethodInformation mi : dm.methodInformations.values()){
			TreeMap<ArrayList<AbstractInsnNode>, Temporary> tmps = new TreeMap<>((list1, list2) -> list1.size() - list2.size());
			for(Temporary t : mi.temporaries){
				ArrayList<AbstractInsnNode> blocks = t.getContiguousBlock();
				if(blocks != null){
					tmps.put(blocks, t);
				}
			}
			//Map<Temporary, ArrayList<AbstractInsnNode>> tmps = mi.temporaries.stream().collect(Collectors.toMap(item -> item, Temporary::getContiguousBlock));
			for(Entry<ArrayList<AbstractInsnNode>, Temporary> entry : tmps.entrySet()){
				if(entry.getValue() instanceof DataManager.MethodInvocationTemporary){
					if((Object)entry.getValue() != mi){
						continue;
					}
					MethodInvocationTemporary mit = (MethodInvocationTemporary) entry.getValue();
					Temporary[] args = mit.getArgs();
					if(args.length == 2){
						for(AbstractInsnNode ain : entry.getKey()){
							System.err.println(Textifier.OPCODES[ain.getOpcode()]);
						}

						Temporary first = args[0];
						Temporary second = args[1];

						int swapOpcode = Opcodes.SWAP; //TODO handle doubles and longs

						ArrayList<AbstractInsnNode> F = first.getContiguousBlock();
						ArrayList<AbstractInsnNode> S = second.getContiguousBlock();

						if(F.size() > 1 && S.size() > 1){
							// -->F[]<-->S[]<-->CALL<--
							// tmp = F[first].prev
							// F[first].prev = S[last]
							// S[last].next = F[first]
							// F[last].next = CALL[first]
							// S[first].prev = tmp
							// CALL[first].prev = F[last]
							// tmp.next = S[first]
							AbstractInsnNode tmp = F.get(0).getPrevious();
							F.get(0).setPrev(S.get(S.size() - 1));
							S.get(S.size() - 1).setNext(F.get(0));
							F.get(F.size() - 1).setNext(entry.getValue().getDeclaration());
							S.get(0).setPrev(tmp);
							entry.getValue().getDeclaration().setPrev(F.get(F.size() - 1));
							tmp.setNext(S.get(0));
						}else if((F.size() == 0 || F.size() == 1) && S.size() > 1){
							AbstractInsnNode f = S.get(0).getPrevious();
							// -->f<-->S[]<-->CALL<--
							// //f = S[first].prev
							// S[first].prev = f.prev
							// S[last].next = f
							// f.prev.next = S[first]
							// f.prev = S[last]
							// f.next = CALL
							// CALL.prev = f
							S.get(0).setPrev(f.getPrevious());
							S.get(S.size() - 1).setNext(f);
							if(f.getPrevious() != null)
								f.getPrevious().setNext(S.get(0));
							f.setPrev(S.get(0));
							f.setNext(entry.getValue().getDeclaration());
							entry.getValue().getDeclaration().setPrev(f);
						}else if((S.size() == 0 || S.size() == 1) && F.size() > 1){
							AbstractInsnNode s = F.get(F.size() - 1).getNext();
							// -->F[]<-->S<-->CALL<--
							// //s = F[last].next
							// s.prev = F[first].prev
							// s.next = F[first]
							// F[first].prev.next = s
							// F[first].prev = s
							// F[last].next = CALL
							// CALL.prev = F[last]
							s.setPrev(F.get(0).getPrevious());
							s.setNext(F.get(0));
							F.get(0).getPrevious().setNext(s);
							F.get(0).setPrev(s);
							F.get(F.size() - 1).setNext(entry.getValue().getDeclaration());
							entry.getValue().getDeclaration().setPrev(F.get(F.size() - 1));
						}else if((F.size() == 0 || F.size() == 1) && (S.size() == 0 || S.size() == 1)){
							// -->F<-->S<-->CALL<--
							// s = CALL.prev
							// f = s.prev
							// s.prev = f.prev
							// s.next = f
							// f.prev = s
							// f.next = CALL
							AbstractInsnNode s = entry.getValue().getDeclaration().getPrevious();
							AbstractInsnNode f = s.getPrevious();
							s.setPrev(f.getPrevious());
							s.setNext(f);
							f.setPrev(s);
							f.setNext(entry.getValue().getDeclaration());
						}
						//swapped

						mi.mn.instructions.insertBefore(entry.getValue().getDeclaration(), new InsnNode(swapOpcode));


						System.err.println(dm.methodNodeToOwnerMap.get(mi.mn).name + "." + mi.mn.name + mi.mn.desc);
						System.err.println(entry.getValue());

					}
				}
			}
		}
	}

}
