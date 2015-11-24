package org.armanious.csci1260.optimization;

import java.lang.reflect.Array;
import java.util.HashMap;

import org.armanious.csci1260.DataManager;
import org.armanious.csci1260.DataManager.ArrayInstanceTemporary;
import org.armanious.csci1260.DataManager.ArrayLengthOperator;
import org.armanious.csci1260.DataManager.ArrayReferenceTemporary;
import org.armanious.csci1260.DataManager.BinaryOperatorTemporary;
import org.armanious.csci1260.DataManager.CastOperatorTemporary;
import org.armanious.csci1260.DataManager.CompareOperatorTemporary;
import org.armanious.csci1260.DataManager.ConstantTemporary;
import org.armanious.csci1260.DataManager.FieldTemporary;
import org.armanious.csci1260.DataManager.InstanceofOperatorTemporary;
import org.armanious.csci1260.DataManager.InvokeSpecialTemporary;
import org.armanious.csci1260.DataManager.MethodInformation;
import org.armanious.csci1260.DataManager.MethodInvocationTemporary;
import org.armanious.csci1260.DataManager.NegateOperatorTemporary;
import org.armanious.csci1260.DataManager.ObjectInstanceTemporary;
import org.armanious.csci1260.DataManager.ParameterTemporary;
import org.armanious.csci1260.DataManager.PhiTemporary;
import org.armanious.csci1260.DataManager.Temporary;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

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
		RESOLVER_MAP.put(ArrayReferenceTemporary.class, (ArrayReferenceTemporary t) -> Array.get(resolve(t.arrayRef), ((Number)resolve(t.index)).intValue()));
		RESOLVER_MAP.put(BinaryOperatorTemporary.class, (BinaryOperatorTemporary t) -> {
			Object lhs = resolve(t.getLeftHandSideTemporary());
			Object rhs = resolve(t.getRightHandSideTemporary());
			Object result = null;
			if(t.type == Type.DOUBLE_TYPE){
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
				throw new RuntimeException("Could not resolve BinaryOperatorTemporary: " + t);
			}
			return result;
		});
		//NegateOperatorTemporary, CastOperatorTemporary, CompareOperatorTemporary,
		//ObjectInstanceTemporary, ArrayInstanceTemporary, ArrayLengthTemporary,
		//InstanceofTemporary, PhiTemporary
		RESOLVER_MAP.put(NegateOperatorTemporary.class, (NegateOperatorTemporary t) -> {
			Object val = resolve(t.getOperand());
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
		RESOLVER_MAP.put(CastOperatorTemporary.class, (CastOperatorTemporary t) -> resolve(t.getOperand()));
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
		RESOLVER_MAP.put(ArrayLengthOperator.class, (ArrayLengthOperator t) -> {
			System.err.println("Warning: ArrayLengthOperator resolution not implemented.");
			Temporary arrayRef = t.arrayRef;
			if(!(arrayRef instanceof ArrayInstanceTemporary)){
				return null;
			}
			return null;
		});
		RESOLVER_MAP.put(InstanceofOperatorTemporary.class, (InstanceofOperatorTemporary t) -> {
			Type objectType = t.objectRef.getType();
			Type toCheck = t.toCheck;
			System.err.println("Warning: InstanceofOperatorTemporary does not check against full class hierarchy.");
			return objectType.equals(toCheck);
		});
		RESOLVER_MAP.put(PhiTemporary.class, (t) ->{
			throw new RuntimeException("I don't know how you called me: " + t.getClass());
		});
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object resolve(Temporary t){
		return ((Resolver)RESOLVER_MAP.get(t.getClass())).resolve(t);
	}

	private final DataManager dm;

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
		for(ClassNode cn : dm.classes){
			for(MethodNode mn : cn.methods){
				MethodInformation mi = dm.methodInformations.get(mn);
				if(mi == null) continue;

				final int size = mi.temporaries.size();
				final Temporary[] arr = mi.temporaries.values().toArray(new Temporary[size]);

				for(int i = 0; i < size; i++){
					final Temporary T = arr[i];
					if(T instanceof ConstantTemporary || T.getConstancy() != Temporary.CONSTANT) continue;
					System.out.println(T + " == " + resolve(T));
				}
			}
		}
	}

}
