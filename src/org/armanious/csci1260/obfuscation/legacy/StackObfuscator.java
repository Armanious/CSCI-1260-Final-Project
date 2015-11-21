package org.armanious.csci1260.obfuscation.legacy;

import java.util.ArrayList;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodNode;

//TODO implement later; first String literal encryption
@Deprecated
public class StackObfuscator {
	
	public void obfuscateStack(ArrayList<ClassNode> classes) {
		for(ClassNode cn : classes){
			for(MethodNode mn : cn.methods){
				if(mn.tryCatchBlocks == null || mn.tryCatchBlocks.size() == 0){
					obfuscateStackInMethodBody(mn);
				}
			}
		}
	}

	public void obfuscateStackInMethodBody(MethodNode mn){
		if(mn.instructions == null || mn.instructions.getFirst() == null) return;
		AbstractInsnNode ain = mn.instructions.getFirst();
		do {
			if(ain.getType() == AbstractInsnNode.JUMP_INSN){
				JumpInsnNode jin = (JumpInsnNode) ain;
				if(jin.label == null){
					System.err.println("huh");
				}else if(jin.label.getPrevious() == null){
					System.err.println("huh Squared: " + (jin.label == mn.instructions.getFirst()));
				}else{
					System.out.println("Jump insn; label preceded by: " + jin.label.getPrevious().getClass().getSimpleName());
				}
				if(jin.label == mn.instructions.getFirst()) continue;
				AbstractInsnNode nodeBeforeJumpLabel = jin.label.getPrevious();
				if(nodeBeforeJumpLabel.getType() == AbstractInsnNode.JUMP_INSN ||
						nodeBeforeJumpLabel.getType() == AbstractInsnNode.INSN){
					InsnList listToInsert = new InsnList();
					//listToInsert.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
					//listToInsert.add(new InsnNode(Opcodes.ACONST_NULL));
					listToInsert.add(new InsnNode(Opcodes.DUP));
					listToInsert.add(new InsnNode(Opcodes.POP));
					//mn.maxStack += 2;
					mn.instructions.insert(nodeBeforeJumpLabel, listToInsert);
				}
				
				/*
				 * if(nodeBeforeJumpLabel.getType() == AbstractInsnNode.JUMP_INSN ||
						nodeBeforeJumpLabel.getType() == AbstractInsnNode.INSN){
					AbstractInsnNode upComingFrameNode = jin.label.getNext();
					while(upComingFrameNode != null){
						if(upComingFrameNode.getType() == AbstractInsnNode.FRAME){
							break;
						}
						upComingFrameNode = upComingFrameNode.getNext();
					}
					AbstractInsnNode toInsertAt = upComingFrameNode != null ? upComingFrameNode : jin.label;
					instructions.insertBefore(upComingFrameNode, new InsnNode(Opcodes.DUP));
				}
				 */
			}


		}while((ain = ain.getNext()) != null);
		/*
		 * wanted usage: 
		 * InsnPatternFinder exploitableJumpPattern = InsnPatternFinder.compilePattern("[JumpInsn-varName][AbstractInsnNode]+[UnconditionalJump|Return-varName2][varName.label]");
		 * InsnPattern[] matches = jumpPattern.findAll(instructions);
		 * for(InsnPattern match : matches){
		 *     AbstractInsnNode toExploit = match.get("varName2");
		 *     toExploit.insertAfter(new InsnNode(Opcodes.POP));
		 *     //insert more fake code
		 * }
		 */
		//   Obfuscated bytecode example:
		//   0: aload_0
		//   1: getfield 8	z/Ab:finally	Ljava/io/InputStream;
		//   4: dup
		//   5: astore_1
		//   6: ifnull +39 -> 45
		//   9: aload_1
		//   10: getstatic 52	java/lang/System:in	Ljava/io/InputStream;
		//   13: if_acmpeq +7 -> 20
		//   16: aload_1
		//   17: invokevirtual 53	java/io/InputStream:close	()V
		//   20: aload_0
		//   21: aload_0
		//   22: aconst_null
		//   23: dup_x1 //before: this, this, null; after: this, null, this, null; maxStack + 1
		//   24: putfield 20	z/Ab:false	Lz/LPt2; //    this.false = null
		//   27: putfield 8	z/Ab:finally	Ljava/io/InputStream;     //this.finally = null
		//   30: return  //stack size is 0
		//   31: pop   // fake instruction
		//   32: astore_1 //fake instruction
		//   33: aload_1 //fake instruction
		//   34: aconst_null //fake instruction
		//   35: aload_0 //fake instruction
		//   36: dup_x1 //fake instruction
		//   37: aconst_null //fake instruction
		//   38: putfield 20	z/Ab:false	Lz/LPt2; //fake instruction
		//   41: putfield 8	z/Ab:finally	Ljava/io/InputStream; //fake instruction
		//   44: athrow //fake instruction=
		//       note: offsets 38 & 41 == offsets 24 & 27
		//   45: return
		// Line number table:
		//   Java source line #247	-> byte code offset #0
		//   Java source line #270	-> byte code offset #6
		//   Java source line #171	-> byte code offset #9
		//   Java source line #223	-> byte code offset #16
		//   Java source line #241	-> byte code offset #20
		//   Java source line #282	-> byte code offset #27
		//   Java source line #308	-> byte code offset #30
		//   Java source line #241	-> byte code offset #32
		//   Java source line #282	-> byte code offset #41
		//   Java source line #10	-> byte code offset #45
		// Local variable table:
		//   start	length	slot	name	signature
		//   0	46	0	a	Ab
		// Exception table:
		//   from	to	target	type
		//   9	20	32	finally

	}

}
