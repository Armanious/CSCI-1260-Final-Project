package org.armanious.csci1260.optimization;

import java.util.ArrayList;

import org.armanious.csci1260.DataManager;
import org.armanious.csci1260.DataManager.BinaryOperatorTemporary;
import org.armanious.csci1260.DataManager.ConstantTemporary;
import org.armanious.csci1260.DataManager.MethodInformation;
import org.armanious.csci1260.DataManager.Temporary;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class ConstantFolder {

	private final DataManager dm;

	public ConstantFolder(DataManager dm){
		this.dm = dm;
	}

	private Object resolve(Temporary tmp){
		if(tmp instanceof ConstantTemporary){
			return ((ConstantTemporary)tmp).getValue();
		}
		return null;
	}

	private void fold(MethodNode mn, BinaryOperatorTemporary t){
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
		}else if(t.type == Type.INT_TYPE){
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
	}

	public void optimize(){
		for(ClassNode cn : dm.classes){
			for(MethodNode mn : cn.methods){
				MethodInformation mi = dm.methodInformations.get(mn);
				if(mi == null) continue;

				final int size = mi.temporaries.size();
				final Temporary[] arr = mi.temporaries.values().toArray(new Temporary[size]);

				for(int i = 0; i < size; i++){
					final Temporary T = arr[i];
					for(int j = i + 1; j < size; j++){
						final Temporary S = arr[j];
						if(T.equals(S)){
							ArrayList<AbstractInsnNode> block = T.getContiguousBlockSorted();
							if(block != null && block.size() >= 3){
								System.err.println(cn.name + "." + mn.name + mn.desc + "\n\tInstruction " + T.getDeclaration().getIndex() + " == Instruction " + S.getDeclaration().getIndex() + 
										"\n\t" + T + " == " + S);
							}
						}
					}
				}
			}
		}
	}

}
