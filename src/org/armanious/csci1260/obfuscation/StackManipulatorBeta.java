package org.armanious.csci1260.obfuscation;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import org.armanious.csci1260.DataManager;
import org.armanious.csci1260.temporaries.InvokeSpecialTemporary;
import org.armanious.csci1260.temporaries.Temporary;
import org.armanious.csci1260.JavaStack;
import org.armanious.csci1260.MethodInformation;
import org.armanious.csci1260.Tuple;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

public class StackManipulatorBeta implements Opcodes {

	private static final double RATE_OF_STACK_MANIPULATION = 0.2500;

	private final DataManager dm;

	public StackManipulatorBeta(DataManager dm){
		this.dm = dm;
	}

	public void obfuscate(){
		int count = 0;
		int possibilityCount = 0;
		for(MethodInformation mi : dm.methodInformations.values()){
			/*final Temporary[] tmps = mi.temporaries.values().toArray(new Temporary[mi.temporaries.size()]);
			@SuppressWarnings("unchecked") //sigh type erasure
			final ArrayList<AbstractInsnNode>[] blocks = (ArrayList<AbstractInsnNode>[]) new ArrayList[tmps.length];
			final Comparator<ArrayList<AbstractInsnNode>> descendingComparator = (A,B)->(B == null ? -1 : B.size()) - (A == null ? -1 : A.size());
			
			for(int i = 0; i < tmps.length; i++){
				final Temporary tmp = tmps[i];
				final ArrayList<AbstractInsnNode> block = tmp.getContiguousBlockSorted();
				if(tmp.getDeclaration() == null || block == null) continue;

				int index = Arrays.binarySearch(blocks, block, descendingComparator);
				if(index < 0) index = ~index;

				System.arraycopy(blocks, index, blocks, index + 1, blocks.length - index - 1);
				blocks[index] = block;
				System.arraycopy(tmps, index, tmps, index + 1, tmps.length - index - 1);
				tmps[index] = tmp;
			}

			for(int i = 0; i < blocks.length; i++){
				final ArrayList<AbstractInsnNode> block = blocks[i];
				if(block == null) continue;

				final int start = block.get(0).getIndex();
				final int end = block.get(block.size() - 1).getIndex();

				for(int j = i + 1; j < blocks.length; j++){
					final ArrayList<AbstractInsnNode> list2 = blocks[j];
					if(list2 == null) continue;

					final int start2 = list2.get(0).getIndex();
					final int end2 = list2.get(list2.size() - 1).getIndex();

					if((start2 >= start && start2 <= end) || (end2 >= start && end2 <= end)){
						//make sure they are non-overlapping; i.e. select the temporaries with
						//the largest size of contiguous instructions and remove the temporaries
						//that interfere with it (may or may not be an operand)

						blocks[j] = null;
						tmps[j] = null;
						//allowing such overlap breaks the LinkedList because there will
						// **many** circular connections of ~3 instructions
					}
				}
			}*/
			
			ArrayList<Temporary> targets = new ArrayList<>();
			for(Temporary t : mi.temporaries.values()){
				if(t.getDeclaration() == null) continue;
				ArrayList<AbstractInsnNode> block = t.getContiguousBlockSorted();
				if(block == null){
					continue;
				}
				if(block.size() < 3){
					continue;
				}
				if(block.get(0).getIndex() < 0){
					continue;
				}
				if(targets.contains(t)) continue;
				ListIterator<Temporary> targetsIter = targets.listIterator();
				boolean addNew = true;
				while(targetsIter.hasNext()){
					if(targetsIter.next().getCriticalTemporaries().contains(t)){
						addNew = false;
						break;
					}
				}
				if(addNew){
					int start = block.get(0).getIndex();
					int end = block.get(block.size() - 1).getIndex();
					ListIterator<Temporary> iter = targets.listIterator();
					while(iter.hasNext()){
						ArrayList<AbstractInsnNode> block2 = iter.next().getContiguousBlockSorted();
						int start2 = block2.get(0).getIndex();
						int end2 = block2.get(block2.size() - 1).getIndex();
						if(start2 >= start && end2 <= end){
							iter.remove();
						}
					}
					targets.add(t);
				}
			}
			
			for(int i = 0; i < targets.size(); i++){
				final Temporary tmp = targets.get(i);
				final ArrayList<AbstractInsnNode> block = targets.get(i).getContiguousBlockSorted();
				
				final ArrayList<Temporary> criticalTemporaries = tmp.getCriticalTemporaries();
				if(r.nextDouble() <= RATE_OF_STACK_MANIPULATION){
					count++;
					manipulateStackBeforeAndAfterTemporary(mi, tmp, criticalTemporaries, block);
				}
				if(r.nextDouble() <= RATE_OF_STACK_MANIPULATION){
					count++;
					manipulateStackSwappingTemporaryOperands(mi, tmp, criticalTemporaries, block);
				}
				possibilityCount += 2;
				//NOTE: if you will do both: you must do the insertion one before the swapping one
				//otherwise, it corrupts the stack
			}

		}

		System.out.println("Manipulated the stack in " + count + " out of " + possibilityCount + " posssible locations. "
				+ "(" + ((int)(count * 10000.0 / possibilityCount))/100.0 + "%)");

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
		final int sizeOfReturn = 1;//r.nextInt(3);
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
		//sizeOfReturn * 10 + sizeOfResult; sizeOfReturn is first digit; result is second digit

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
	//	System.out.println("\n\nBefore:");
		//printInstructions(mi);
		mi.mn.instructions.insertBefore(block.get(0), toInsertBefore.val1);
		mi.mn.instructions.insert(block.get(block.size() - 1), toAddAfter);
	//	System.out.println("\nAfter:");
	//	printInstructions(mi);
	//	System.out.println("\n\n");
	}
	
	private void printInstructions(MethodInformation mn){
		System.out.println(dm.methodNodeToOwnerMap.get(mn.mn).name + "." + mn.mn.name + mn.mn.desc);
		Textifier t = new Textifier();
		mn.mn.accept(new TraceMethodVisitor(t));
		for(int i = 0; i < t.text.size(); i++){
			System.out.print(i + ": " + t.text.get(i));
		}
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
						{SWAP, DUP2_X1, POP2},
						{SWAP, DUP2_X1, POP, POP},
						{DUP_X1, POP, DUP2_X1, POP2},
						{DUP_X1, POP, DUP2_X1, POP, POP},

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
		if(t instanceof InvokeSpecialTemporary) return;

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

		ArrayList<Integer> possibilities = new ArrayList<>();
		for(int i = 0; i < criticalTemporaries.size(); i++){
			possibilities.add(i);
		}
		ListIterator<Integer> iter = possibilities.listIterator();
		while(iter.hasNext()){
			int next = iter.next();
			if(criticalTemporaries.get(next).getDeclaration() == null || criticalTemporaries.get(next).getContiguousBlockSorted() == null){
				iter.remove();
			}
		}
		if(possibilities.size() == 0){
			System.err.println("Can't manipulate stack on " + dm.methodNodeToOwnerMap.get(mi.mn).name + "." + mi.mn.name + mi.mn.desc);
			return;
		}
		int firstIndex = possibilities.get(r.nextInt(possibilities.size()));
		Temporary first = criticalTemporaries.get(firstIndex);
		ArrayList<AbstractInsnNode> F = first.getContiguousBlockSorted();
		
		
		int secondIndex; //index of second operand to swap
		Temporary second;
		ArrayList<AbstractInsnNode> S;
		
		int sizeBetweenInclusive;

		possibilities.clear();
		for(int i = -4; i <= 4; i++){
			if(i != 0) possibilities.add(i);
		}
		iter = possibilities.listIterator();
		while(iter.hasNext()){
			int possibility = iter.next();
			if(firstIndex + possibility < 0 || firstIndex + possibility >= criticalTemporaries.size()){
				iter.remove();
				continue;
			}
			if(sizeBetweenInclusive(criticalTemporaries, firstIndex, firstIndex + possibility) > 4){
				iter.remove();
				continue;
			}
			if(criticalTemporaries.get(firstIndex + possibility).getDeclaration() == null || criticalTemporaries.get(firstIndex + possibility).getContiguousBlockSorted() == null){
				iter.remove();
				continue;
			}
			//size MUST be less than or equal to 4
			for(int i = Math.min(firstIndex, firstIndex + possibility); i < Math.max(firstIndex, firstIndex + possibility); i++){
				int op = criticalTemporaries.get(i).getDeclaration().getOpcode();
				switch(op){
				case Opcodes.SWAP:
				case Opcodes.DUP:
				case Opcodes.DUP2:
				case Opcodes.DUP_X1:
				case Opcodes.DUP_X2:
				case Opcodes.DUP2_X1:
				case Opcodes.DUP2_X2:
					iter.remove();
					break;
				default:
					continue;
				}
			}
		}
		if(possibilities.size() == 0){
			System.err.println("Can't manipulate stack on " + dm.methodNodeToOwnerMap.get(mi.mn).name + "." + mi.mn.name + mi.mn.desc);
			return;
		}
		secondIndex = firstIndex + possibilities.get(r.nextInt(possibilities.size()));
		second = criticalTemporaries.get(secondIndex);
		S = second.getContiguousBlockSorted();
		sizeBetweenInclusive = sizeBetweenInclusive(criticalTemporaries, firstIndex, secondIndex);

		if(firstIndex > secondIndex){
			int tmp = firstIndex;
			firstIndex = secondIndex;
			secondIndex = tmp;
			
			Temporary TMP = first;
			first = second;
			second = TMP;
			
			ArrayList<AbstractInsnNode> temp = F;
			F = S;
			S = temp;
		}

		swap(F, S);
		
		//insert instructions to swap at runtime before declaration
		int[][] toRandomize = STACK_RESTORATION_OPS[sizeBetweenInclusive][first.getType().getSize()][second.getType().getSize()];
		if(toRandomize == null){
			if(criticalTemporaries.get(firstIndex + 1).getType().getSize() == 2){
				//1 MID, size 2
				final InsnList toAdd = new InsnList();
				add(toAdd, DUP_X2, POP, DUP2_X2, POP2, SWAP);
				toAdd.add(new VarInsnNode(DataManager.getStoreOpcode(criticalTemporaries.get(firstIndex + 1).getType()), mi.mn.maxLocals));
				add(toAdd, DUP_X2, POP);
				toAdd.add(new VarInsnNode(DataManager.getLoadOpcode(criticalTemporaries.get(firstIndex + 1).getType()), mi.mn.maxLocals));
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
	}

}
