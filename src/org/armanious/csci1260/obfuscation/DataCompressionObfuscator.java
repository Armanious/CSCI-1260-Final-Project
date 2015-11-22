package org.armanious.csci1260.obfuscation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.armanious.csci1260.DataManager;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class DataCompressionObfuscator {
	
	private static final String CLASS_LOADER_NAME;
	static {
		String name = null;
		try {
			name = Class.forName("CustomClassLoader").getName();
			//we have to do it indirectly because it is impossible to import classes from the default package
			//but we need to keep it in the default package because this will be in the jar file that we output
		} catch (ClassNotFoundException e) {}
		CLASS_LOADER_NAME = name;
	}
	
	public static void compressDataAndOutputJarFile(final DataManager dm, String main_class, File output_directory) {
		try {
			main_class = main_class.replace('/', '.'); //convert to binary
			/*ClassWriter cw = new ClassWriter(0);
			ClassReader cr = new ClassReader(forlolz.Crazy.class.getName());
			
			cr.accept(new FilterClassVisitor(cw, main_class), 0);
			
			final byte[] crazyData = cw.toByteArray();*/
			final ClassReader cr = new ClassReader(CLASS_LOADER_NAME);
			final ClassNode classLoaderCn = new ClassNode();
			cr.accept(classLoaderCn, 0);
			for(MethodNode mn : classLoaderCn.methods){
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
			
			//TODO: Run StackManipulator and NameObfuscator obfuscations on this class itself
			
			final ClassWriter free = new ClassWriter(0);
			classLoaderCn.accept(free);
			
			final byte[] data = free.toByteArray();
			
			final Manifest manifest = new Manifest(new ByteArrayInputStream(("Manifest-Version: 1.0\n" +
					"Created-By: 1.7.0_06\n"
					+ "Main-Class: " + CLASS_LOADER_NAME + "\n").getBytes()));
			final File outputFile = new File(output_directory, "obfuscated.jar");
			output_directory.mkdirs();
			outputFile.createNewFile();
			final JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputFile), manifest);
			
			jos.putNextEntry(new ZipEntry(classLoaderCn.name + ".class"));
			jos.write(data, 0, data.length);
			jos.closeEntry();
			
			jos.putNextEntry(new ZipEntry("inner"));
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
