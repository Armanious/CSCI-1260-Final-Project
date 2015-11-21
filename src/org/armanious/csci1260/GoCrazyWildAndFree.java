package org.armanious.csci1260;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class GoCrazyWildAndFree {
	
	private static class FilterClassVisitor extends ClassVisitor {

		private final String main_class;
		
		public FilterClassVisitor(ClassVisitor owner, String main_class) {
			super(Opcodes.ASM5, owner);
			this.main_class = main_class;
		}
		
		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			return new FilterMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions), main_class);
		}
		
	}
	
	private static class FilterMethodVisitor extends MethodVisitor {
		
		private final String main_class;
		
		public FilterMethodVisitor(MethodVisitor owner, String main_class){
			super(Opcodes.ASM5, owner);
			this.main_class = main_class;
		}
		
		@Override
		public void visitLdcInsn(Object cst) {
			if(cst instanceof String && ((String)cst).equals("REPLACE ME WITH NAME OF MAIN CLASS")){
				super.visitLdcInsn(main_class);
			}
			super.visitLdcInsn(cst);
		}
		
	}

	public static void goCrazyWildAndFree(final DataManager dm, String main_class, File output_directory) {
		try {
			main_class = main_class.replace('/', '.'); //convert to binary
			System.err.println(main_class);
			/*ClassWriter cw = new ClassWriter(0);
			ClassReader cr = new ClassReader(forlolz.Crazy.class.getName());
			
			cr.accept(new FilterClassVisitor(cw, main_class), 0);
			
			final byte[] crazyData = cw.toByteArray();*/
			final ClassReader cr = new ClassReader(forlolz.Crazy.class.getName());
			final ClassNode wild = new ClassNode();
			cr.accept(wild, 0);
			for(MethodNode mn : wild.methods){
				if(mn.name.equals("main")){
					for(AbstractInsnNode ain = mn.instructions.getFirst(); ain != null; ain = ain.getNext()){
						if(ain instanceof LdcInsnNode){
							final LdcInsnNode lin = (LdcInsnNode) ain;
							if(lin.cst.equals("REPLACE ME WITH NAME OF MAIN CLASS")){
								lin.cst = main_class;
							}
						}
					}
				}
			}
			final ClassWriter free = new ClassWriter(0);
			wild.accept(free);
			
			final byte[] crazyData = free.toByteArray();
			
			final Manifest manifest = new Manifest(new ByteArrayInputStream(("Manifest-Version: 1.0\n" +
					"Created-By: 1.7.0_06\n"
					+ "Main-Class: forlolz.Crazy\n").getBytes()));
			final File outputFile = new File(output_directory, "obfuscated.jar");
			output_directory.mkdirs();
			outputFile.createNewFile();
			final JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputFile), manifest);
			
			jos.putNextEntry(new ZipEntry("forlolz/Crazy.class"));
			jos.write(crazyData, 0, crazyData.length);
			jos.closeEntry();
			
			jos.putNextEntry(new ZipEntry("dontlookatme"));
			final ZipOutputStream zos = new ZipOutputStream(jos);
			for(ClassNode cn : dm.classes){
				zos.putNextEntry(new ZipEntry(cn.name.replace('/', '.')));
				final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES){
					@Override
					protected String getCommonSuperClass(String type1, String type2) {
						Type superClass = dm.getCommonSuperType(Type.getType(type1), Type.getType(type2));
						if(superClass == null){
							return super.getCommonSuperClass(type1, type2);
						}
						return superClass == null ? super.getCommonSuperClass(type1, type2) : (superClass.getDescriptor().length() == 1 ? superClass.getDescriptor() : superClass.getInternalName());
					}
				};
				cn.accept(cw);
				zos.write(cw.toByteArray());
				zos.closeEntry();
			}
			zos.flush();
			
			jos.closeEntry();
			jos.flush();
			jos.close();
			
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
