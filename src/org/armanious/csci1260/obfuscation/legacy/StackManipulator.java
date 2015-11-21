package org.armanious.csci1260.obfuscation.legacy;

import java.lang.reflect.Modifier;
import java.util.Random;

import org.armanious.csci1260.obfuscation.ObfuscationManager;
import org.armanious.csci1260.obfuscation.Obfuscator;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

@Deprecated
public class StackManipulator extends Obfuscator implements Opcodes {
	
	public StackManipulator(ObfuscationManager manager) {
		super(manager);
	}
	
	//possible TODO
	 /* private static int getNumArguments(String desc){
		return -1;
	}*/
	
	private static final Random random = new Random();
	private static final double FREQUENCY_OF_METHOD_CALL_STACK_MANIPULATION = .150;

	@Override
	public void obfuscate() {
		for(ClassNode cn : om.getClassNodes()){
			for(MethodNode mn : cn.methods){
				if(Modifier.isAbstract(mn.access)) continue;
				int idx = 0;
				for(AbstractInsnNode ain = mn.instructions.getFirst(); ain != null; ain = ain.getNext(), idx++){
					if(ain.getType() == AbstractInsnNode.METHOD_INSN){
						MethodInsnNode min = (MethodInsnNode) ain;
						if(shouldManipulateMethodCallStack()){
							MethodCallStackManipulatorStrategy strat = getStrategy(min);
							if(strat != null){
								System.err.println("MANIPULATING");
								strat.manipulateMethodCallStack(mn.instructions, min);
							}
						}
					}
				}
				System.out.println();
			}
			System.out.println("\n\n");
		}
	}
	
	private static final MethodCallStackManipulatorStrategy[][] methodCallStackManipulators = new MethodCallStackManipulatorStrategy[5][];
	
	static {
		//no stack size, there IS NO manipulation to do specific to method calls
		methodCallStackManipulators[0] = new MethodCallStackManipulatorStrategy[0];
		
		//size of stack = 1
		methodCallStackManipulators[1] = new MethodCallStackManipulatorStrategy[]{
				new MethodCallStackManipulatorStrategy(){
					public void manipulateMethodCallStack(InsnList insns, MethodInsnNode min) {
						insns.insertBefore(min, new InsnNode(DUP));
						InsnList afterInsns = new InsnList();
						Type retType = Type.getReturnType(min.desc);
						switch(retType.getSort()){
						case Type.VOID:
							/*
							 * aload_0
							 * dup
							 * invokevirtual; no return
							 * pop
							 */
							afterInsns.add(new InsnNode(POP));
							break;
						case Type.DOUBLE:
						case Type.LONG:
							/*
							 * aload_0
							 * dup
							 * invokevirtual; 2 stack words returned and used
							 * dup2_x1
							 * pop2
							 * pop
							 */
							if(min.getNext().getOpcode() == POP2){
								insns.insert(min.getNext(), new InsnNode(POP));
								//pop our dup after the double/long is popped
							}else{
								afterInsns.insert(new InsnNode(DUP2_X1));
								afterInsns.insert(new InsnNode(POP2));
								afterInsns.insert(new InsnNode(POP));
							}
							break;
						default:
							/*
							 * aload_0
							 * dup
							 * invokevirtual; 1 stack word returned and used
							 * dup_x1
							 * pop
							 */
							if(min.getNext().getOpcode() == POP){
								insns.remove(min.getNext());
								insns.insert(min, new InsnNode(POP2));
							}else{
								afterInsns.insert(new InsnNode(DUP_X1));
								afterInsns.insert(new InsnNode(POP));
							}
							break;
						}
						insns.insert(min, afterInsns);
					}
				}
		};
		
		//size of stack = 2
		methodCallStackManipulators[2] = new MethodCallStackManipulatorStrategy[]{
				//gonna duplicate and pop 2 elements
				new MethodCallStackManipulatorStrategy(){
					public void manipulateMethodCallStack(InsnList insns, MethodInsnNode min) {
						insns.insertBefore(min, new InsnNode(DUP2));
						InsnList afterInsns = new InsnList();
						Type retType = Type.getReturnType(min.desc);
						switch(retType.getSort()){
						case Type.VOID:
							/*
							 * aload_0
							 * iload_1
							 * dup2
							 * invokevirtual; no return
							 * pop2
							 */
							afterInsns.add(new InsnNode(POP2));
							break;
						case Type.DOUBLE:
						case Type.LONG:
							/*
							 *aload_0
							 *iload_1
							 *dup2
							 *invokevirtual; return stack size 2
							 *dup2_x2 (copy them below our dups)
							 *pop2 (pop the original return result)
							 *pop2 (pop our dups)
							 */
							if(min.getNext().getOpcode() == POP2){
								afterInsns.insert(new InsnNode(POP2));
							}else{
								afterInsns.insert(new InsnNode(DUP2_X2));
								afterInsns.insert(new InsnNode(POP2));
								afterInsns.insert(new InsnNode(POP2));
							}
							break;
						default:
							/*
							 *aload_0
							 *iload_1
							 *dup2
							 *invokevirtual; return stack size 1
							 *dup_x2 (copy them below our dups)
							 *pop (pop the original return result)
							 *pop2 (pop our dups)
							 */
							if(min.getNext().getOpcode() == POP){
								insns.insert(min, new InsnNode(POP2));
							}else{
								afterInsns.insert(new InsnNode(DUP_X2));
								afterInsns.insert(new InsnNode(POP));
								afterInsns.insert(new InsnNode(POP2));
							}
							break;
						}
						insns.insert(min, afterInsns);
					}
				}
				,
				//gonna duplicate and pop 2nd element
				new MethodCallStackManipulatorStrategy(){
					public void manipulateMethodCallStack(InsnList insns, MethodInsnNode min) {
						insns.insertBefore(min, new InsnNode(DUP_X1));
						InsnList afterInsns = new InsnList();
						Type retType = Type.getReturnType(min.desc);
						switch(retType.getSort()){
						case Type.VOID:
							/*
							 * aload_0 [this]
							 * iload_1 [this, int]
							 * dup_x1 [int, this, int]
							 * invokevirtual; no return [int]
							 * pop []
							 */
							afterInsns.add(new InsnNode(POP));
							break;
						case Type.DOUBLE:
						case Type.LONG:
							/*
							 * aload_0 [this]
							 * iload_1 [this, int]
							 * dup_x1 [int, this, int]
							 * invokevirtual; 2 stack words returned and used [int, RET1, RET2]
							 * dup2_x1 [RET1, RET2, int, RET1, RET2]
							 * pop2 [RET1, RET2, int]
							 * pop [RET1, RET2]
							 */
							if(min.getNext().getOpcode() == POP2){
								insns.insert(min.getNext(), new InsnNode(POP));
								//pop our dup after the double/long is popped
							}else{
								afterInsns.insert(new InsnNode(DUP2_X1));
								afterInsns.insert(new InsnNode(POP2));
								afterInsns.insert(new InsnNode(POP));
							}
							break;
						default:
							/*
							 * aload_0 [this]
							 * iload_1 [this, int]
							 * dup_x1 [int, this, int]
							 * invokevirtual; 1 stack word returned and used [int, RET]
							 * dup_x1 [RET, int, RET]
							 * pop2 [RET]
							 */
							if(min.getNext().getOpcode() == POP){
								insns.remove(min.getNext());
								insns.insert(min, new InsnNode(POP2));
							}else{
								afterInsns.insert(new InsnNode(DUP_X1));
								afterInsns.insert(new InsnNode(POP));
							}
							break;
						}
						insns.insert(min, afterInsns);
					}
				}
		};
		
		//size of stack = 3
		methodCallStackManipulators[3] = new MethodCallStackManipulatorStrategy[0];
		

		//size of stack = 4 and above
		methodCallStackManipulators[4] = new MethodCallStackManipulatorStrategy[0];
	}
	
	private static <T> T selectRandom(T[] arr){
		if(arr == null || arr.length == 0) return null;
		return arr[random.nextInt(arr.length)];
	}
	
	private static MethodCallStackManipulatorStrategy getStrategy(MethodInsnNode methodCallNode){
		Type[] types = Type.getArgumentTypes(methodCallNode.desc);
		int stackLengthForCall = 0;
		for(Type type : types){
			stackLengthForCall++;
			if(type == Type.DOUBLE_TYPE || type == Type.LONG_TYPE){
				stackLengthForCall++; //doubles and longs take 2 words on stack
			}
		}
		if(methodCallNode.getOpcode() == INVOKEINTERFACE ||
				methodCallNode.getOpcode() == INVOKESPECIAL ||
				methodCallNode.getOpcode() == INVOKEVIRTUAL){
			stackLengthForCall++; //for the object reference at bottom of stack
		}
		
		return selectRandom(methodCallStackManipulators[Math.min(4, stackLengthForCall)]);
	}
	
	private static boolean shouldManipulateMethodCallStack(){
		return random.nextDouble() < FREQUENCY_OF_METHOD_CALL_STACK_MANIPULATION;
	}
	
	private static interface MethodCallStackManipulatorStrategy {
		
		void manipulateMethodCallStack(InsnList insns, MethodInsnNode methodCallInsnNode);
		
	}

}
