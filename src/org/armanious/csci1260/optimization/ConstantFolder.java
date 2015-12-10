package org.armanious.csci1260.optimization;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

import org.armanious.csci1260.DataManager;
import org.armanious.csci1260.MethodInformation;
import org.armanious.csci1260.Tuple;
import org.armanious.csci1260.temporaries.ArrayInstanceTemporary;
import org.armanious.csci1260.temporaries.ArrayLengthTemporary;
import org.armanious.csci1260.temporaries.ArrayReferenceTemporary;
import org.armanious.csci1260.temporaries.BinaryOperatorTemporary;
import org.armanious.csci1260.temporaries.CastOperatorTemporary;
import org.armanious.csci1260.temporaries.CompareOperatorTemporary;
import org.armanious.csci1260.temporaries.ConstantTemporary;
import org.armanious.csci1260.temporaries.FieldTemporary;
import org.armanious.csci1260.temporaries.InstanceofOperatorTemporary;
import org.armanious.csci1260.temporaries.InvokeSpecialTemporary;
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

	private static interface Resolver<T extends Temporary> {
		Object resolve(T constantTemporary);
	}

	private static final HashMap<Class<? extends Temporary>, Resolver<? extends Temporary>> RESOLVER_MAP = new HashMap<>();
	static {
		//FieldTemporary, MethodInvocationTemporary, InvokeSpecialTemporary, ConstantTemporary
		//ParameterTemporary, ArrayReferenceTemporary, BinaryOperatorTemporary,
		//NegateOperatorTemporary, CastOperatorTemporary, CompareOperatorTemporary,
		//ObjectInstanceTemporary, ArrayInstanceTemporary, ArrayLengthTemporary,
		//InstanceofTemporary, PhiTemporary

		//guaranteed that if the Resolver is called, the temporary argument is constant
		RESOLVER_MAP.put(FieldTemporary.class, (FieldTemporary t) -> t.getValue());
		RESOLVER_MAP.put(MethodInvocationTemporary.class, (MethodInvocationTemporary t) -> {
			if(!t.hasSideEffects()){
				System.out.println("Resolving " + t + " is possible, but we will not for now.");
			}
			return null;
		});
		RESOLVER_MAP.put(InvokeSpecialTemporary.class, (InvokeSpecialTemporary t) -> null);
		RESOLVER_MAP.put(ConstantTemporary.class, (ConstantTemporary t) -> t.getValue());
		RESOLVER_MAP.put(ParameterTemporary.class, (ParameterTemporary t) -> null);
		RESOLVER_MAP.put(ArrayReferenceTemporary.class, (ArrayReferenceTemporary t) -> {
			final Object arrayRef = resolve(t.arrayRef);
			final Object index = resolve(t.index);
			return (arrayRef != null && index != null) ? Array.get(arrayRef, ((Number)index).intValue()) : null;
		});
		RESOLVER_MAP.put(BinaryOperatorTemporary.class, (BinaryOperatorTemporary t) -> {
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
		});
		//NegateOperatorTemporary, CastOperatorTemporary, CompareOperatorTemporary,
		//ObjectInstanceTemporary, ArrayInstanceTemporary, ArrayLengthTemporary,
		//InstanceofTemporary, PhiTemporary
		RESOLVER_MAP.put(NegateOperatorTemporary.class, (NegateOperatorTemporary t) -> {
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
		});
		RESOLVER_MAP.put(CastOperatorTemporary.class, (CastOperatorTemporary t) -> {
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
		});
		RESOLVER_MAP.put(CompareOperatorTemporary.class, (CompareOperatorTemporary t) -> {
			Object lhs = resolve(t.lhs);
			Object rhs = t.rhs == null ? null : resolve(t.rhs);
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
		});
		//ObjectInstanceTemporary, ArrayInstanceTemporary, ArrayLengthTemporary,
		//InstanceofTemporary, PhiTemporary
		RESOLVER_MAP.put(ObjectInstanceTemporary.class, (ObjectInstanceTemporary t) -> null);
		RESOLVER_MAP.put(ArrayInstanceTemporary.class, (ArrayInstanceTemporary t) -> null);
		RESOLVER_MAP.put(ArrayLengthTemporary.class, (ArrayLengthTemporary t) -> {
			//System.err.println("Warning: ArrayLengthOperator resolution not implemented: " + t);
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
		});
		RESOLVER_MAP.put(InstanceofOperatorTemporary.class, (InstanceofOperatorTemporary t) -> {
			Type objectType = t.objectRef.getType();
			Type toCheck = t.toCheck;
			System.err.println("Warning: InstanceofOperatorTemporary does not check against full class hierarchy.");
			return objectType.equals(toCheck);
		});
		RESOLVER_MAP.put(PhiTemporary.class, (t) -> {
			throw new RuntimeException("I don't know how you called me: " + t.getClass());
		});
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object resolve(Temporary t){
		return ((Resolver)RESOLVER_MAP.get(t.getClass())).resolve(t);
	}

	private final DataManager dm;
	private int numConstantsFolded;

	public ConstantFolder(DataManager dm){
		this.dm = dm;
	}

	/*private void fold(MethodNode mn, BinaryOperatorTemporary t){
		Object lhs = resolve(t.getLeftHandSideTemporary());
		if(lhs == null) return;
		Object rhs = resolve(t.getRightHandSideTemporary());
		if(rhs == null) return;

		InsnList toInsert = new InsnList();
		toInsert.add(new InsnNode(Opcodes.POP2)); //pop the two instrctions on top

		Object result = null;

		if(t.type == Type.DOUBLE_TYPE){
			toInsert.add(new InsnNode(Opcodes.POP2));
			double l = (Double) lhs;
			double r = (Double) rhs;
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
			float l = (Float) lhs;
			float r = (Float) rhs;
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
			toInsert.add(new InsnNode(Opcodes.POP2));
			long l = (Long) lhs;
			long r = (Long) rhs;
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
			mn.instructions.get(0);
			System.err.println(dm.methodNodeToOwnerMap.get(mn).name + "." + mn.name + mn.desc + "\n\tInstruction " + t.getDeclaration().getIndex());
			throw new RuntimeException();
		}

		toInsert.add(new LdcInsnNode(result));

		mn.instructions.insertBefore(t.getDeclaration(), toInsert);
		mn.instructions.remove(t.getDeclaration());

		System.out.println("Replaced " + t + " with " + result);
	}*/

	public void optimize(){
		for(MethodInformation mi : dm.methodInformations.values()){
			//if(!mi.mn.name.equals("fundamentalLoopTest")) continue;
			final ArrayList<Tuple<ArrayList<AbstractInsnNode>, Object>> validTargets = new ArrayList<>();
			//ordered (and will be sorted) ArrayList of (block, value)
			//we don't need a reference to the temporary; we just know that
			//the instructions contained within block will always evaluate to value
			for(Temporary T : mi.temporaries.values()){
				if(T instanceof ConstantTemporary || 
						//we can implement the below classes if we have a fake execution environment
						//we will not do that for now; it is a possible extension of the program
						//in the future
						T instanceof FieldTemporary ||
						T instanceof MethodInvocationTemporary || 
						T instanceof ObjectInstanceTemporary || 
						//T instanceof ArrayReferenceTemporary || 
						//T instanceof ArrayInstanceTemporary || 
						T.getConstancy() != Temporary.CONSTANT) continue;

				ArrayList<AbstractInsnNode> block = T.getContiguousBlockSorted();
				if(block == null) continue;

				int blockStart = block.get(0).getIndex();
				int blockEnd = block.get(block.size() - 1).getIndex();

				Object resolved = resolve(T);
				if(resolved == null) continue;
				boolean addNew = true;
				for(int i = 0; i < validTargets.size(); i++){
					int storedBlockStart = validTargets.get(i).val1.get(0).getIndex();
					int storedBlockEnd = validTargets.get(i).val1.get(validTargets.get(i).val1.size() - 1).getIndex();
					if(storedBlockStart > blockEnd || blockStart > storedBlockEnd){
						//no intersection; add as we should
						//addNew = true
					}else if(storedBlockStart >= blockStart && storedBlockEnd <= blockEnd){
						//block2 is smaller (theoretically can be equal, but we will never
						//have a Temporary with identical instruction lists
						//replace block2 with block
						//System.out.println("REPLACING; " + T + " ==> " + "Instructions " + block.get(0).getIndex() + " - " + block.get(block.size() - 1).getIndex() + " = " + resolved);
						validTargets.set(i, new Tuple<>(block, resolved));
						addNew = false;
						break;
					}else if(blockStart >= storedBlockStart && blockEnd <= storedBlockEnd){
						//block2 is greater than the block we are evaluating
						//do nothing
						addNew = false;
					}else{
						System.out.println("Currently stored: " + storedBlockStart + " - " + storedBlockEnd);
						System.out.println("Comparing against: " + blockStart + " - " + blockEnd);
						System.out.println();
					}
				}
				if(addNew){
					//System.out.println(T + " ==> " + "Instructions " + block.get(0).getIndex() + " - " + block.get(block.size() - 1).getIndex() + " = " + resolved);

					validTargets.add(new Tuple<>(block, resolved));
				}
			}
			if(validTargets.size() > 0){
				for(Tuple<ArrayList<AbstractInsnNode>, Object> target : validTargets){
					//System.out.println("Instructions " + target.val1.get(0).getIndex() + " - " + target.val1.get(target.val1.size() - 1).getIndex() + " = " + target.val2);
					replace(mi.mn.instructions, target.val1, getConstantInstruction(target.val2));
					//System.err.println(mi.mn.instructions.size());
				}
				numConstantsFolded += validTargets.size();
				//System.out.println("Folded " + validTargets.size() + " constants in " + dm.methodNodeToOwnerMap.get(mn).name + "." + mn.name + mn.desc);
				mi.recompute();
			}

		}
		System.out.println("Folded " + numConstantsFolded + " constants.");
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

	//copied from LoopOptimizations
	private static void replace(InsnList list, ArrayList<AbstractInsnNode> block, AbstractInsnNode insn) {
		//list.get(0);
		//System.err.println("Replacing " + block.get(0).getIndex() + " - " + block.get(block.size() - 1).getIndex() + " with " + Textifier.OPCODES[varInsnNode.getOpcode()]);
		list.insertBefore(block.get(0), insn);
		for(AbstractInsnNode ain : block){
			list.remove(ain);
		}
	}

}
