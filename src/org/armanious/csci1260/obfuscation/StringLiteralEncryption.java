package org.armanious.csci1260.obfuscation;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.util.Random;

import org.armanious.csci1260.DataManager;
import org.armanious.csci1260.MethodInformation;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.TraceClassVisitor;

public class StringLiteralEncryption {
	
	public static void main(String...args) throws IOException{
		ASMifier asmifier = new ASMifier();
		ClassReader cr = new ClassReader(StringLiteralEncryption.class.getName());
		ClassVisitor cv = new TraceClassVisitor(null, asmifier, new PrintWriter(System.out));
		cr.accept(cv, 0);
	}

	public static String decipher(String s){
		final char[] arr = s.toCharArray();
		for(int i = 0; i < arr.length; i++){
			arr[i] = (char)(arr[i]/(i % 12 + 1) - 100);
		}
		return new String(arr);
	}

	private static final double RATE_OF_NEW_CIPHER = 0.150;
	private static final Random random = new Random();

	private static final int MIN_K = Integer.MIN_VALUE;
	private static final int MAX_K = Integer.MAX_VALUE;

	private static final int MIN_J = Integer.MIN_VALUE;
	private static final int MAX_J = Integer.MAX_VALUE;

	private final DataManager dm;

	public StringLiteralEncryption(DataManager dm){
		this.dm = dm;
	}
	
	private static final String decipherDesc = "(Ljava/lang/String;)Ljava/lang/String;";

	private class CipherHandle implements Opcodes {

		private final int k;
		private final int j;

		private String decipherMethodOwner;
		private String decipherMethodName;
		private boolean isOwnerInterface;

		public CipherHandle(int k, int j){
			this.k = k;
			this.j = j;
		}

		public String cipher(String s){
			final char[] arr = s.toCharArray();
			for(int i = 0; i < arr.length; i++){
				arr[i] = (char) ((arr[i] + k)*(i % j + 1));
			}
			return new String(arr);
		}

		public String decipher(String s){
			final char[] arr = s.toCharArray();
			for(int i = 0; i < arr.length; i++){
				arr[i] = (char)(arr[i]/(i % j + 1) - k);
			}
			return new String(arr);
		}
		
		private void insertInstructions(MethodNode mn){
			final InsnList list = new InsnList();

			final LabelNode label10 = new LabelNode(new Label());
			final LabelNode label30 = new LabelNode(new Label());
			
			list.add(new VarInsnNode(ALOAD, 0));
			list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C", false));
			list.add(new VarInsnNode(ASTORE, 1)); //char[] arr = s.toCharArray()
			list.add(new InsnNode(ICONST_0));
			list.add(new VarInsnNode(ISTORE, 2)); //int i = 0
			list.add(new JumpInsnNode(GOTO, label30));
			list.add(label10);
			//list.add(new FrameNode(Opcodes.F_APPEND, 2, new Object[]{"[C", Opcodes.INTEGER}, 0, null));
			list.add(new VarInsnNode(ALOAD, 1));
			list.add(new VarInsnNode(ILOAD, 2));
			list.add(new InsnNode(DUP2)); //ALOAD_1, ILOAD_2 again; stack: arr, i, arr, i
			list.add(new InsnNode(CALOAD)); //arr[i]; stack: arr, i, arr[i]
			list.add(new VarInsnNode(ILOAD, 2));
			if(j >= Byte.MIN_VALUE && j <= Byte.MAX_VALUE){
				list.add(new IntInsnNode(BIPUSH, j));
			}else if(j >= Short.MIN_VALUE && j <= Short.MAX_VALUE){
				list.add(new IntInsnNode(SIPUSH, j));
			}else{
				list.add(new LdcInsnNode(j));
			}
			list.add(new InsnNode(IREM));
			list.add(new InsnNode(ICONST_1));
			list.add(new InsnNode(IADD));
			list.add(new InsnNode(IDIV));
			if(k >= Byte.MIN_VALUE && k <= Byte.MAX_VALUE){
				list.add(new IntInsnNode(BIPUSH, k));
			}else if(k >= Short.MIN_VALUE && k <= Short.MAX_VALUE){
				list.add(new IntInsnNode(SIPUSH, k));
			}else{
				list.add(new LdcInsnNode(k));
			}
			list.add(new InsnNode(ISUB));
			list.add(new InsnNode(I2C));
			list.add(new InsnNode(CASTORE));
			list.add(new IincInsnNode(2, 1));
			list.add(label30);
			//list.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
			list.add(new VarInsnNode(ILOAD, 2));
			list.add(new VarInsnNode(ALOAD, 1));
			list.add(new InsnNode(ARRAYLENGTH));
			list.add(new JumpInsnNode(IF_ICMPLT, label10)); //if(i < arr.length) goto 10
			list.add(new TypeInsnNode(NEW, "java/lang/String"));
			list.add(new InsnNode(DUP));
			list.add(new VarInsnNode(ALOAD, 1));
			list.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V", false));
			list.add(new InsnNode(ARETURN));
			
			mn.maxLocals = 3;
			mn.maxStack = 5;
			
			mn.instructions = list;
		}
		
		public MethodInsnNode getDecipherMethodInstruction() {
			if(decipherMethodOwner == null){
				ClassNode cn;
				while(true){
					cn = dm.classes.get(random.nextInt(dm.classes.size()));
					if(cn.outerClass == null && 
							(Opcodes.ACC_PUBLIC & cn.access) != 0 &&
							(Opcodes.ACC_ENUM & cn.access) == 0){
						//make sure it's not an inner class, and make sure the class is accessible
						break;
					}
				}
				isOwnerInterface = (cn.access & Opcodes.ACC_INTERFACE) != 0;
				String name;
				while(true){
					name = "peekaboo" + random.nextInt();
					if(cn.getMethodNode(name, decipherDesc) == null){
						break;
					}
				}
				decipherMethodOwner = cn.name;
				decipherMethodName = name;
				final MethodNode insertedMethodNode = (MethodNode) cn.visitMethod(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, name, decipherDesc, null, null);
				
				insertInstructions(insertedMethodNode);
				
				dm.methodNodeToOwnerMap.put(insertedMethodNode, cn);
				dm.methodInformations.put(insertedMethodNode, new MethodInformation(dm, insertedMethodNode));
			}
			return new MethodInsnNode(Opcodes.INVOKESTATIC, decipherMethodOwner, decipherMethodName, decipherDesc, isOwnerInterface);
		}

	}

	private CipherHandle prev = null;

	private CipherHandle getNextCipher(){
		if(prev == null || Math.random() <= RATE_OF_NEW_CIPHER){
			int k;
			while((k = MIN_K + (int) (random.nextDouble() * ((double)MAX_K - MIN_K))) == 0);
			int j;
			while((j = MIN_J + (int) (random.nextDouble() * ((double)MAX_J - MIN_J))) == 0);
			prev = new CipherHandle(k, j);
			numCiphers++;
		}
		return prev;
	}
	
	private int numCiphers = 0;
	private int numEncrypted = 0;
	
	public void obfuscate(){
		for(ClassNode cn : dm.classes){
			final MethodNode[] methods = cn.methods.toArray(new MethodNode[cn.methods.size()]);
			//to avoid ConcurrentModificationException if the selected class to add the decipher
			//method just happens to be equal to cn as well, we convert the methods to an array
			long last = System.currentTimeMillis();
			for(MethodNode mn : methods){
				if(Modifier.isAbstract(mn.access) || Modifier.isNative(mn.access)) continue;
				boolean forceRetry = false;
				for(AbstractInsnNode ain = mn.instructions.getFirst(); ain != null; ain = (forceRetry ? ain : ain.getNext())){
					if(System.currentTimeMillis() - last >= 500){
						System.err.println("DBUG");
					}
					if(ain instanceof LdcInsnNode){
						forceRetry = false;
						final LdcInsnNode lin = (LdcInsnNode) ain;
						if(lin.cst instanceof String){
							CipherHandle handle = getNextCipher();
							String ciphered = handle.cipher((String)lin.cst);
							String deciphered = handle.decipher(ciphered);
							if(!lin.cst.equals(deciphered)){
								//System.err.println("Cipher algorithm failed; retrying...: " + lin.cst);
								//sometimes it fails; instead of wasting time thinking of
								//strict limitations and relations to the k and j values,
								//just retry until you get one that works...
								prev = null; //forces creation of new cipher next time
								forceRetry = true;
								numCiphers--; //it didn't work; don't count it
								//ain = ain.getPrevious(); //rewind one instruction
								//cant do ain.getPrevious because sometimes the LDC instruction
								//is the first in the block
								continue; //try again
							}
							lin.cst = ciphered;
							numEncrypted++;
							mn.instructions.insert(lin, handle.getDecipherMethodInstruction());
						}
					}
				}
				last = System.currentTimeMillis();
			}
		}
		
		System.out.println("Encrypted " + numEncrypted + " string literals using " + numCiphers + " different ciphers.");
		
	}

}
