package org.armanious.csci1260.optimization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Stream;

import org.armanious.csci1260.DataManager;
import org.armanious.csci1260.MethodInformation;
import org.armanious.csci1260.temporaries.ArrayInstanceTemporary;
import org.armanious.csci1260.temporaries.ArrayLengthTemporary;
import org.armanious.csci1260.temporaries.ArrayReferenceTemporary;
import org.armanious.csci1260.temporaries.BinaryOperatorTemporary;
import org.armanious.csci1260.temporaries.CastOperatorTemporary;
import org.armanious.csci1260.temporaries.CompareOperatorTemporary;
import org.armanious.csci1260.temporaries.ConstantTemporary;
import org.armanious.csci1260.temporaries.FieldTemporary;
import org.armanious.csci1260.temporaries.InstanceofOperatorTemporary;
import org.armanious.csci1260.temporaries.MethodInvocationTemporary;
import org.armanious.csci1260.temporaries.NegateOperatorTemporary;
import org.armanious.csci1260.temporaries.ObjectInstanceTemporary;
import org.armanious.csci1260.temporaries.ParameterTemporary;
import org.armanious.csci1260.temporaries.PhiTemporary;
import org.armanious.csci1260.temporaries.Temporary;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

public class ConstantFolder {

	private final DataManager dm;
	private int numConstantsFolded;

	public ConstantFolder(DataManager dm){
		this.dm = dm;
	}

	private final HashMap<Temporary, Object> resolvedCache = new HashMap<>();
	private Object resolve(Temporary t){
		if(t == null) return null;
		Object o = resolvedCache.get(t);
		if(o == null){
			//resolve
			if(t instanceof FieldTemporary){
				o = ((FieldTemporary)t).getValue();
			}else if(t instanceof MethodInvocationTemporary){
				//takes care of InvokeSpecialTemporary as well
				if(!((MethodInvocationTemporary)t).hasSideEffects()){
					//System.out.println("Resolving " + t + " is possible, but we will not for now.");
				}
				o = null;
			}else if(t instanceof ConstantTemporary){
				o = ((ConstantTemporary)t).getValue();
			}else if(t instanceof ParameterTemporary){
				o = null;
			}else if(t instanceof BinaryOperatorTemporary){
				o = resolveBinaryOperatorTemporary((BinaryOperatorTemporary)t);
			}else if(t instanceof NegateOperatorTemporary){
				o = resolveNegateOperatorTemporary((NegateOperatorTemporary)t);
			}else if(t instanceof CastOperatorTemporary){
				o = resolveCastOperatorTemporary((CastOperatorTemporary)t);
			}else if(t instanceof CompareOperatorTemporary){
				o = resolveCompareOperatorTemporary((CompareOperatorTemporary)t);
			}else if(t instanceof ObjectInstanceTemporary){
				o = null;
			}else if(t instanceof ArrayInstanceTemporary){
				o = null;
			}else if(t instanceof ArrayLengthTemporary){
				o = resolveArrayLengthTemporary((ArrayLengthTemporary)t);
			}else if(t instanceof InstanceofOperatorTemporary){
				o = resolveInstanceOfOperatorTemporary((InstanceofOperatorTemporary)t);
			}else if(t instanceof PhiTemporary){
				o = null;
			}
			resolvedCache.put(t, o);
		}
		return o;
	}

	private Object resolveBinaryOperatorTemporary(BinaryOperatorTemporary t){
		Object lhs = resolve(t.getLeftHandSideTemporary());
		Object rhs = resolve(t.getRightHandSideTemporary());
		if(lhs == null || rhs == null){
			return null;
		}
		Object result = null;
		if(t.type == Type.DOUBLE_TYPE){
			double l = ((Number)lhs).doubleValue();
			double r = ((Number)rhs).doubleValue();
			switch(t.getArithmeticType()){
			case BinaryOperatorTemporary.ADD:
				result = l + r;
				break;
			case BinaryOperatorTemporary.SUB:
				result = l - r;
				break;
			case BinaryOperatorTemporary.MUL:
				result = l * r;
				break;
			case BinaryOperatorTemporary.DIV:
				result = l / r;
				break;
			}
		}else if(t.type == Type.FLOAT_TYPE){
			float l = ((Number)lhs).floatValue();
			float r = ((Number)rhs).floatValue();
			switch(t.getArithmeticType()){
			case BinaryOperatorTemporary.ADD:
				result = l + r;
				break;
			case BinaryOperatorTemporary.SUB:
				result = l - r;
				break;
			case BinaryOperatorTemporary.MUL:
				result = l * r;
				break;
			case BinaryOperatorTemporary.DIV:
				result = l / r;
				break;
			}
		}else if(t.type == Type.INT_TYPE || t.type == Type.BYTE_TYPE || t.type == Type.SHORT_TYPE || t.type == Type.CHAR_TYPE || t.type == Type.BOOLEAN_TYPE){
			int l = ((Number) lhs).intValue();
			int r = ((Number) rhs).intValue();
			switch(t.getArithmeticType()){
			case BinaryOperatorTemporary.ADD:
				result = l + r;
				break;
			case BinaryOperatorTemporary.SUB:
				result = l - r;
				break;
			case BinaryOperatorTemporary.MUL:
				result = l * r;
				break;
			case BinaryOperatorTemporary.DIV:
				result = l / r;
				break;
			case BinaryOperatorTemporary.REM:
				result = l % r;
				break;
			case BinaryOperatorTemporary.SHL:
				result = l << r;
				break;
			case BinaryOperatorTemporary.SHR:
				result = l >> r;
				break;
			case BinaryOperatorTemporary.USHR:
				result = l >>> r;
				break;
			case BinaryOperatorTemporary.AND:
				result = l & r;
				break;
			case BinaryOperatorTemporary.OR:
				result = l | r;
				break;
			case BinaryOperatorTemporary.XOR:
				result = l ^ r;
				break;
			}
		}else if(t.type == Type.LONG_TYPE){
			long l = ((Number)lhs).longValue();
			long r = ((Number)rhs).longValue();
			switch(t.getArithmeticType()){
			case BinaryOperatorTemporary.ADD:
				result = l + r;
				break;
			case BinaryOperatorTemporary.SUB:
				result = l - r;
				break;
			case BinaryOperatorTemporary.MUL:
				result = l * r;
				break;
			case BinaryOperatorTemporary.DIV:
				result = l / r;
				break;
			case BinaryOperatorTemporary.REM:
				result = l % r;
				break;
			case BinaryOperatorTemporary.SHL:
				result = l << r;
				break;
			case BinaryOperatorTemporary.SHR:
				result = l >> r;
				break;
			case BinaryOperatorTemporary.USHR:
				result = l >>> r;
				break;
			case BinaryOperatorTemporary.AND:
				result = l & r;
				break;
			case BinaryOperatorTemporary.OR:
				result = l | r;
				break;
			case BinaryOperatorTemporary.XOR:
				result = l ^ r;
				break;
			}
		}
		if(result == null){
			throw new RuntimeException("Could not resolve BinaryOperatorTemporary: " + t);
		}
		return result;
	}

	private Object resolveNegateOperatorTemporary(NegateOperatorTemporary t){
		Object val = resolve(t.getOperand());
		if(val == null) return null;
		switch(t.getType().getSort()){
		case Type.INT:
			return -(Integer)val;
		case Type.LONG:
			return -(Long)val;				
		case Type.DOUBLE:
			return -(Double)val;
		case Type.FLOAT:
			return -(Float)val;
		default:
			throw new RuntimeException("Could not resolve NegateOperatorTemporary: " + t);
		}
	}

	private Object resolveCastOperatorTemporary(CastOperatorTemporary t){
		Number val = (Number) resolve(t.getOperand());
		if(val == null) return null;
		switch(t.getDeclaration().getOpcode()){
		case Opcodes.L2I:
		case Opcodes.F2I:
		case Opcodes.D2I:
			return val.intValue();
		case Opcodes.I2L:
		case Opcodes.F2L:
		case Opcodes.D2L:
			return val.longValue();
		case Opcodes.I2F:
		case Opcodes.L2F:
		case Opcodes.D2F:
			return val.floatValue();
		case Opcodes.I2D:
		case Opcodes.L2D:
		case Opcodes.F2D:
			return val.doubleValue();
		case Opcodes.I2B:
			return val.byteValue();
		case Opcodes.I2C:
			return (char)(val.intValue());
		case Opcodes.I2S:
			return val.shortValue();
		default:
			throw new IllegalArgumentException("Unknown primitive cast opcode: " + t.getDeclaration().getOpcode());
		}
	}

	private Object resolveCompareOperatorTemporary(CompareOperatorTemporary t){
		Object lhs = resolve(t.lhs);
		Object rhs = resolve(t.rhs);
		if(lhs == null || rhs == null) return null;
		Object result = null;
		switch(t.opcode){
		case Opcodes.LCMP:
			if(((Long)lhs).longValue() == ((Long)rhs).longValue()){
				result = 0;
			}else if(((Long)lhs).longValue() > (((Long)rhs).longValue())){
				result = 1;
			}else if(((Long)lhs).longValue() < (((Long)rhs).longValue())){
				result = -1;
			}
			break;
		case Opcodes.FCMPL:
		case Opcodes.FCMPG:
			if(((Float)lhs).floatValue() == ((Float)rhs).floatValue()){
				result = 0;
			}else if(((Float)lhs).floatValue() > ((Float)rhs).floatValue()){
				result = 1;
			}else if(((Float)lhs).floatValue() < ((Float)rhs).floatValue()){
				result = -1;
			}else{
				result = t.opcode == Opcodes.FCMPL ? -1 : 1;
			}
			break;
		case Opcodes.DCMPL:
		case Opcodes.DCMPG:
			if(((Double)lhs).doubleValue() == ((Double)rhs).doubleValue()){
				result = 0;
			}else if(((Double)lhs).doubleValue() > ((Double)rhs).doubleValue()){
				result = 1;
			}else if(((Double)lhs).doubleValue() < ((Double)rhs).doubleValue()){
				result = -1;
			}else{
				result = t.opcode == Opcodes.DCMPL ? -1 : 1;
			}
			break;
		default:
			break;
		}
		if(result == null){
			throw new RuntimeException("Could not resolve CompareOperatorTemporary: " + t);
		}
		return result;
	}

	private Object resolveArrayLengthTemporary(ArrayLengthTemporary t){
		Temporary arrayInstance = t.arrayRef;
		int dimensionToGetLengthOf = 0;
		while(!(arrayInstance instanceof ArrayInstanceTemporary)){
			if(!(arrayInstance instanceof ArrayReferenceTemporary)){
				return null;
			}
			arrayInstance = ((ArrayReferenceTemporary)arrayInstance).arrayRef;
			dimensionToGetLengthOf++;
		}
		Temporary length = ((ArrayInstanceTemporary)arrayInstance).dimensionCounts[dimensionToGetLengthOf];
		return resolve(length);
	}

	private Object resolveInstanceOfOperatorTemporary(InstanceofOperatorTemporary t){
		Type objectType = t.objectRef.getType();
		Type toCheck = t.toCheck;
		return toCheck == objectType;//(dm.getCommonSuperType(objectType, toCheck) == toCheck) ? 1 : 0;
	}

	private boolean canFold(Temporary markedConstant){
		ArrayList<AbstractInsnNode> block = markedConstant.getContiguousBlockSorted();
		return markedConstant.getType() != Type.VOID_TYPE && block != null && block.size() > 1 && resolve(markedConstant) != null;
	}

	private final HashMap<Integer, Integer> noOverlapCache = new HashMap<>();
	private boolean noOverlap(Temporary temporary){
		boolean noOverlap = true;
		ArrayList<AbstractInsnNode> block = temporary.getContiguousBlockSorted();
		int idx1 = block.get(0).getIndex();
		int idx2 = block.get(block.size() - 1).getIndex();
		if(idx1 == -1 || idx2 == -1) return false;
		for(Integer key : noOverlapCache.keySet()){
			Integer val = noOverlapCache.get(key);
			if((idx1 >= key && idx1 <= val) || (idx2 >= key && idx2 <= val)){
				noOverlap = false;
				break;
			}
		}
		return noOverlap;
	}

	private boolean optimize(MethodInformation mi){
		resolvedCache.clear();
		noOverlapCache.clear();
		mi.mn.instructions.get(0);
		//clear caches and generate method instruction index to properly remove overlaps

		Stream<Temporary> constantTemporaries = mi.temporaries.values().stream().filter((t) -> t.getConstancy() == Temporary.CONSTANT);
		//simply get all the temporaries marked as CONSTANT
		
		Stream<Temporary> foldable = constantTemporaries.filter(this::canFold);
		//filter the ones that are "foldable": has a contiguousBlock of length > 1, is not of Type VOID
		//and maps to a constant value through the resolution scheme
		
		Stream<Temporary> longestFirst = foldable.sorted((t1,t2) -> t2.getContiguousBlockSorted().size() - t1.getContiguousBlockSorted().size());
		//orders the stream from greatest length block to shortest length block
		//this ensures we will be folding the most general form of each temporary; doing so enables
		//folding complex expressions in a simple way using the preexisting framework
		
		Stream<Temporary> noOverlap = longestFirst.filter(this::noOverlap);
		//KEY: calls noOverlap in descending order so that we select the most general Temporaries
		//removes any overlapping Temporaries
		
		int countConstantsFoldedBefore = numConstantsFolded;	
		noOverlap.forEach((t) -> foldConstant(mi, t));
		//for each eligible temporary, simply fold the constant
		
		//Very readable!
		
		return numConstantsFolded > countConstantsFoldedBefore;
	}

	private static AbstractInsnNode getConstantInstruction(Object val){
		if(val instanceof Integer || val instanceof Character){
			int number = (Integer) val;
			switch(number){
			case -1:
				return new InsnNode(Opcodes.ICONST_M1);
			case 0:
				return new InsnNode(Opcodes.ICONST_0);
			case 1:
				return new InsnNode(Opcodes.ICONST_1);
			case 2:
				return new InsnNode(Opcodes.ICONST_2);
			case 3:
				return new InsnNode(Opcodes.ICONST_3);
			case 4:
				return new InsnNode(Opcodes.ICONST_4);
			case 5:
				return new InsnNode(Opcodes.ICONST_5);
			default:
				if(number >= Byte.MIN_VALUE && number <= Byte.MAX_VALUE){
					return new IntInsnNode(Opcodes.BIPUSH, number);
				}else if(number >= Short.MIN_VALUE && number <= Short.MAX_VALUE){
					return new IntInsnNode(Opcodes.SIPUSH, number);
				}else{
					return new LdcInsnNode(number);
				}
			}
		}else if(val instanceof Long){
			long number = (Long)val;
			if(number == 0){
				return new InsnNode(Opcodes.LCONST_0);
			}else if(number == 1){
				return new InsnNode(Opcodes.LCONST_1);
			}else{
				return new LdcInsnNode(number);
			}
		}else if(val instanceof Float){
			float number = (Float) val;
			if(number == 0F){
				return new InsnNode(Opcodes.FCONST_0);
			}else if(number == 1F){
				return new InsnNode(Opcodes.FCONST_1);
			}else if(number == 2F){
				return new InsnNode(Opcodes.FCONST_2);
			}else{
				return new LdcInsnNode(number);
			}
		}else if(val instanceof Double){
			double number = (Double) val;
			if(number == 0D){
				return new InsnNode(Opcodes.DCONST_0);
			}else if(number == 1D){
				return new InsnNode(Opcodes.DCONST_1);
			}else{
				return new LdcInsnNode(number);
			}
		}else if(val instanceof Short){
			short number = (Short) val;
			switch(number){
			case -1:
				return new InsnNode(Opcodes.ICONST_M1);
			case 0:
				return new InsnNode(Opcodes.ICONST_0);
			case 1:
				return new InsnNode(Opcodes.ICONST_1);
			case 2:
				return new InsnNode(Opcodes.ICONST_2);
			case 3:
				return new InsnNode(Opcodes.ICONST_3);
			case 4:
				return new InsnNode(Opcodes.ICONST_4);
			case 5:
				return new InsnNode(Opcodes.ICONST_5);
			default:
				if(number >= Byte.MIN_VALUE && number <= Byte.MAX_VALUE){
					return new IntInsnNode(Opcodes.BIPUSH, number);
				}else{
					return new IntInsnNode(Opcodes.SIPUSH, number);
				}
			}
		}else if(val instanceof Byte){
			byte number = (Byte)val;
			switch(number){
			case -1:
				return new InsnNode(Opcodes.ICONST_M1);
			case 0:
				return new InsnNode(Opcodes.ICONST_0);
			case 1:
				return new InsnNode(Opcodes.ICONST_1);
			case 2:
				return new InsnNode(Opcodes.ICONST_2);
			case 3:
				return new InsnNode(Opcodes.ICONST_3);
			case 4:
				return new InsnNode(Opcodes.ICONST_4);
			case 5:
				return new InsnNode(Opcodes.ICONST_5);
			default:
				return new IntInsnNode(Opcodes.BIPUSH, number);
			}
		}else if(val instanceof Boolean){
			return new InsnNode((Boolean)val ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
		}else if(val instanceof String){
			return new LdcInsnNode(val);
		}
		throw new RuntimeException("Unknown type of constant: " + val.getClass().getSimpleName());
	}

	private void foldConstant(MethodInformation mi, Temporary t){
		numConstantsFolded++;
		replace(mi.mn.instructions, t.getContiguousBlockSorted(), getConstantInstruction(resolve(t)));
	}

	//copied from LoopOptimizations
	private static void replace(InsnList list, ArrayList<AbstractInsnNode> block, AbstractInsnNode insn) {
		//list.get(0);
		//System.err.println("Replacing " + block.get(0).getIndex() + " - " + block.get(block.size() - 1).getIndex() + " with " + Textifier.OPCODES[varInsnNode.getOpcode()]);
		list.insertBefore(block.get(0), insn);
		for(AbstractInsnNode ain : block){
			list.remove(ain);
		}
	}

	public void optimize(){
		for(MethodInformation mi : dm.methodInformations.values()){
			if(optimize(mi)){
				mi.recompute();
			}
		}
		System.out.println("Folded " + numConstantsFolded + " constants.");
	}

}
