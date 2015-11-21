package org.armanious.csci1260.obfuscation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;

public class NameObfuscatorBeta extends Obfuscator {

	private final ObfuscatedNameGenerator ong;
	private final boolean preservePackageStructure;

	private final File outputFileForObfuscationMap;

	private final Map<String, String> packageNameRemapping;
	private final Map<String, String> classNameRemapping;
	private final Map<String, String> fieldNameRemapping;
	private final Map<String, String> methodNameRemapping;

	public NameObfuscatorBeta(ObfuscationManager manager,
			String namePattern, int nameLength, boolean preservePackageStructure,
			File outputFileForObfuscationMap) {
		super(manager);
		ong = new ObfuscatedNameGenerator(namePattern, nameLength);
		this.preservePackageStructure = preservePackageStructure;

		this.outputFileForObfuscationMap = outputFileForObfuscationMap;

		packageNameRemapping = new HashMap<>();
		classNameRemapping = new HashMap<>();
		fieldNameRemapping = new HashMap<>();
		methodNameRemapping = new HashMap<>();
	}

	private void populatePackageNameRemapping(){
		ong.reset();
		for(ClassNode cn : om.getClassNodes()){
			int lastIdxOfSlash = cn.name.lastIndexOf('/');
			if(lastIdxOfSlash == -1)
				continue;
			String pkg = cn.name.substring(0, lastIdxOfSlash);
			if(preservePackageStructure){

				String[] parts = pkg.split("/");
				for(int i = 0; i < parts.length; i++){ //name was omitted in the declaration of pkg
					String obfuscatedPackgeSubsection = packageNameRemapping.get(parts[i]);
					if(obfuscatedPackgeSubsection == null){
						obfuscatedPackgeSubsection = ong.getNext();
						packageNameRemapping.put(parts[i], obfuscatedPackgeSubsection);
					}
				}

			}else{
				String obfuscatedPkg = packageNameRemapping.get(pkg);
				if(obfuscatedPkg == null){
					obfuscatedPkg = ong.getNext();
					packageNameRemapping.put(pkg, obfuscatedPkg);
				}
			}
		}
	}

	private void populateClassNameRemapping(){
		ong.reset();
		@SuppressWarnings("unchecked")
		ArrayList<ClassNode> clone = (ArrayList<ClassNode>) om.getClassNodes().clone();
		clone.sort((c1, c2) -> c1.name.length() - c2.name.length());
		//this ensures we visit outermost classes first (because an inner class's
		//name's length will ALWAYS be greater than the outer class's name's length)
		for(ClassNode cn : clone){
			String className = cn.name;
			String obfuscatedClassName = classNameRemapping.get(className);
			if(obfuscatedClassName == null){
				obfuscatedClassName = ong.getNext();
				classNameRemapping.put(className, obfuscatedClassName);
				//include package name; key includes package, value does NOT
			}
			if(cn.innerClasses.size() > 0){
				for(int i = 0; i < cn.innerClasses.size(); i++){
					InnerClassNode icn = cn.innerClasses.get(i);
					if(icn.outerName != null && icn.outerName.startsWith(obfuscatePackage(icn.outerName))){
						//System.err.println(icn.name);
						//checks to make sure inner class isn't language-specified; for some reason,
						//Java seems to implicitly subclass certain classes (such as MapEntry<K,V>)
						//even if the programmer doesn't define them; skip these classes.
						//This checks if there is a difference between the "obfuscated" package and the regular one;
						//if there IS a difference, it's a user-defined inner class; if there is NOT a difference,
						//skip it because it's Java's
						continue;
					}
					String obfuscatedInnerClassName = classNameRemapping.get(icn.name);
					if(obfuscatedInnerClassName == null && !obfuscatePackage(icn.name).equals(icn.name)){
						obfuscatedInnerClassName = obfuscatedClassName + "$" + ong.getNext();
						classNameRemapping.put(icn.name, obfuscatedInnerClassName);
					}
				}
			}
		}
	}

	private void populateFieldNameRemapping(){
		for(ClassNode cn : om.getClassNodes()){
			ong.reset();

			String superName = cn.superName;
			while(superName != null){
				ClassNode superClassNode = null;
				for(ClassNode nextSuper : om.getClassNodes()){
					if(nextSuper.name.equals(superName)){
						superClassNode = nextSuper;
						break;
					}
				}
				if(superClassNode == null) break;

				for(FieldNode superFn : superClassNode.fields){
					if(!Modifier.isStatic(superFn.access) && !Modifier.isPrivate(superFn.access)){
						fieldNameRemapping.put(cn.name + "." + superFn.name, superClassNode.name + "." + superFn.name);
					}
				}

				superName = superClassNode.superName;
			}


			for(FieldNode fn : cn.fields){
				String fieldUniqueMapKey = cn.name + "." + fn.name;
				String obfuscatedFieldName = fieldNameRemapping.get(fieldUniqueMapKey);
				if(obfuscatedFieldName == null){
					obfuscatedFieldName = ong.getNext();
					fieldNameRemapping.put(fieldUniqueMapKey, obfuscatedFieldName);
				}
			}

		}

		boolean changed = true;
		while(changed){
			changed = false;
			for(String fieldNodeUniqueId : fieldNameRemapping.keySet()){
				String obfuscatedFieldNode = fieldNameRemapping.get(fieldNodeUniqueId);
				if(obfuscatedFieldNode.indexOf('.') != -1){
					fieldNameRemapping.put(fieldNodeUniqueId, fieldNameRemapping.get(obfuscatedFieldNode));
					changed = true;
				}
			}
		}

	}

	private void populateMethodNameRemapping(){
		for(ClassNode cn : om.getClassNodes()){
			ong.reset();

			String[] toSearchSupers = cn.interfaces.toArray(new String[cn.interfaces.size() + 1]);
			toSearchSupers[toSearchSupers.length - 1] = cn.superName;
			Arrays.sort(toSearchSupers); //for binary search later
			otherOuter: for(String superName : toSearchSupers){
				outer: while(superName != null){
					for(ClassNode superCnPossibility : om.getClassNodes()){
						if(superName.equals(superCnPossibility.name)){


							for(MethodNode superMn : superCnPossibility.methods){

								if(!superMn.name.equals("<init>") && !superMn.name.equals("<clinit>") && !Modifier.isPrivate(superMn.access) && !Modifier.isStatic(superMn.access)){

									//if(cn.methods.stream().anyMatch((mn)->mn.name.equals(superMn.name))){
									methodNameRemapping.put(cn.name + "." + superMn.name, superCnPossibility.name + "." + superMn.name);
									//including a '.' in the value indicates that it is a reference to another method name
									//}

								}
							}
							superName = superCnPossibility.superName;
							continue outer;
						}
					}
					//could not find superName in ClassNodes, check via reflection
					try{
						Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass(superName.replace('/', '.'));
						if(clazz == null) break;
						for(Method m : clazz.getDeclaredMethods()){
							if(!Modifier.isPrivate(m.getModifiers()) && !Modifier.isStatic(m.getModifiers())){
								//if(cn.methods.stream().anyMatch((mn)->mn.name.equals(m.getName()))){
								methodNameRemapping.put(cn.name + "." + m.getName(), m.getName());
								//}
								//not including a '.' in the value means it is treated as definite in the algorithm
							}
						}
						if(clazz.getSuperclass() == null) break;
						superName = clazz.getSuperclass().getName().replace('.', '/');
					}catch(ReflectiveOperationException e){
						continue otherOuter;
					}
				}
			}
			for(MethodNode mn : cn.methods){
				final String methodUniqueMapKey = cn.name + "." + mn.name;
				if(mn.name.equals("<init>") || mn.name.equals("<clinit>")){
					methodNameRemapping.put(methodUniqueMapKey, mn.name);
					continue; //don't "obfuscate" the initializer names
				}
				if(mn.name.equals("main") && mn.desc.equals("([Ljava/lang/String;)V") && Modifier.isStatic(mn.access) && Modifier.isPublic(mn.access)){
					methodNameRemapping.put(methodUniqueMapKey, mn.name);
					continue; //if it's the entry point, we want to preserve it
				}
				String obfuscatedMethodName = methodNameRemapping.get(methodUniqueMapKey);
				if(obfuscatedMethodName == null){
					obfuscatedMethodName = ong.getNext();
					methodNameRemapping.put(methodUniqueMapKey, obfuscatedMethodName);
				}

			}
		}

		boolean madeChange = true;
		while(madeChange){
			//TODO very miniscule chance of method name collisions is possible at this point
			//even if there is a collision, there's an even smaller chance that the two methods
			//will have the same parameters
			//If that's the case, though, that's a problem; we need to fix it.
			madeChange = false;
			for(String key : methodNameRemapping.keySet()){
				String obfuscatedName = methodNameRemapping.get(key);
				if(obfuscatedName.indexOf('.') != -1){ //overriding something
					methodNameRemapping.put(key, methodNameRemapping.get(obfuscatedName));
					madeChange = true;
				}
			}
		}
	}

	private String obfuscatePackage(String fullInternalName){
		final String[] parts = fullInternalName.split("/");
		final StringBuilder sb = new StringBuilder();
		for(int i = 0; i < parts.length - 1; i++){ //leave last part out because that's the actual class name
			String obfuscatedPart = packageNameRemapping.get(parts[i]);
			if(obfuscatedPart == null){
				return fullInternalName.substring(0, fullInternalName.lastIndexOf('/') + 1);
			}
			sb.append(obfuscatedPart).append('/');
		}
		return sb.toString();
	}

	private String obfuscateMethodFieldDesc(String desc){
		if(desc.startsWith("[") && desc.length() > 11 && desc.substring(10).contains("[")){
			System.err.println("walk");
		}
		final StringBuilder obfuscatedDesc = new StringBuilder(desc);
		int idxOfNextObject = 0;
		int endIdx = 0;
		while(true){
			idxOfNextObject = obfuscatedDesc.indexOf("L", endIdx);
			if(idxOfNextObject == -1)
				break;
			endIdx = obfuscatedDesc.indexOf(";", idxOfNextObject);
			if(endIdx == -1){
				//System.err.println("Invalid desc: " + desc + " (current derivation: " + obfuscatedDesc.toString() + ")");
				break; //it's actually possible to have something like java/lang/Long which is obviously correct
			}
			String internalName = obfuscatedDesc.substring(idxOfNextObject + 1, endIdx); //don't include the 'L'
			String obfuscatedInternalName = classNameRemapping.get(internalName);
			if(obfuscatedInternalName != null){
				String completedObfuscatedInternalName = obfuscatePackage(internalName) + obfuscatedInternalName;
				obfuscatedDesc.replace(idxOfNextObject + 1, endIdx, completedObfuscatedInternalName);
				int deltaIdx = completedObfuscatedInternalName.length() - (endIdx - (idxOfNextObject + 1));
				idxOfNextObject += deltaIdx;
				endIdx += deltaIdx;
			}
		}
		return obfuscatedDesc.toString();
	}

	private void fillObfuscationMaps(){
		populatePackageNameRemapping();
		populateClassNameRemapping();
		populateFieldNameRemapping();
		populateMethodNameRemapping();
	}
	
	@Override
	public void obfuscate() {
		fillObfuscationMaps();

		for(ClassNode cn : om.getClassNodes()){
			String deobfuscatedName = cn.name; //for use in fieldNameRemapping and methodNameRemapping
			String obfuscatedPackage = obfuscatePackage(cn.name);
			cn.name = obfuscatedPackage + classNameRemapping.get(cn.name);
			cn.signature = null;
			cn.sourceDebug = null;
			cn.sourceFile = null;

			if(cn.outerClass != null){
				String obfuscatedOuter = classNameRemapping.get(cn.outerClass);
				if(obfuscatedOuter != null){
					cn.outerClass = obfuscatePackage(cn.outerClass) + obfuscatedOuter;
				}
			}

			String obfuscatedSuper = classNameRemapping.get(cn.superName);
			if(obfuscatedSuper != null){
				cn.superName = obfuscatePackage(cn.superName) + obfuscatedSuper;
			}

			ListIterator<String> interfacesIter = cn.interfaces.listIterator();
			while(interfacesIter.hasNext()){
				String iface = interfacesIter.next();
				String obfuscatedIface = classNameRemapping.get(iface);
				if(obfuscatedIface != null){
					interfacesIter.set(obfuscatePackage(iface) + obfuscatedIface);
				}
			}

			for(InnerClassNode icn : cn.innerClasses){
				if(icn.outerName != null && icn.outerName.startsWith(obfuscatePackage(icn.outerName))){
					continue;
				}
				icn.name = obfuscatedPackage + classNameRemapping.get(icn.name);
				if(icn.outerName != null || icn.innerName != null){
					int last$idx = icn.name.lastIndexOf('$');
					icn.outerName = icn.name.substring(0, last$idx);
					icn.innerName = icn.name.substring(last$idx + 1);
				}
			}
			for(FieldNode fn : cn.fields){
				fn.name = fieldNameRemapping.get(deobfuscatedName + "." + fn.name);
				fn.desc = obfuscateMethodFieldDesc(fn.desc);
				fn.signature = null;
			}			
			for(MethodNode mn : cn.methods){
				if(mn.localVariables != null){
					for(LocalVariableNode lvn : mn.localVariables){
						lvn.desc = obfuscateMethodFieldDesc(lvn.desc);
						lvn.name = String.valueOf((char)0xFEFF);
						lvn.signature = null;
					}
				}
				mn.signature = null;
				mn.name = methodNameRemapping.get(deobfuscatedName + "." + mn.name);
				mn.desc = obfuscateMethodFieldDesc(mn.desc);

				ListIterator<String> exceptionsIter = mn.exceptions.listIterator();
				while(exceptionsIter.hasNext()){
					String exception = exceptionsIter.next();
					String obfuscatedException = classNameRemapping.get(exception);
					if(obfuscatedException != null){
						exceptionsIter.set(obfuscatePackage(exception) + obfuscatedException);
					}
				}

				for(TryCatchBlockNode tcbn : mn.tryCatchBlocks){
					String obfuscatedType = classNameRemapping.get(tcbn.type);
					if(obfuscatedType != null){
						tcbn.type = obfuscatePackage(tcbn.type) + obfuscatedType;
					}
				}

				for(AbstractInsnNode ain = mn.instructions.getFirst(); ain != null; ain = ain.getNext()){

					switch(ain.getType()){
					case AbstractInsnNode.FIELD_INSN:
						FieldInsnNode fin = (FieldInsnNode)ain;

						String obfuscatedName = fieldNameRemapping.get(fin.owner + "." + fin.name);
						if(obfuscatedName != null){
							fin.name = obfuscatedName;
						}

						fin.desc = obfuscateMethodFieldDesc(fin.desc);

						String obfuscatedOwner = classNameRemapping.get(fin.owner);
						if(obfuscatedOwner != null){
							fin.owner = obfuscatePackage(fin.owner) + obfuscatedOwner;
						}

						break;
					case AbstractInsnNode.METHOD_INSN:
						MethodInsnNode min = (MethodInsnNode)ain;

						if(min.owner.charAt(0) == '['){
							//[Lorg/armanious/csci1260/Temporary; .clone(); for example
							min.owner = obfuscateMethodFieldDesc(min.owner);
						}else{
							obfuscatedName = methodNameRemapping.get(min.owner + "." + min.name);
							if(obfuscatedName != null){
								min.name = obfuscatedName;
							}
						}
						
						min.desc = obfuscateMethodFieldDesc(min.desc);

						obfuscatedOwner = classNameRemapping.get(min.owner);
						if(obfuscatedOwner != null){
							min.owner = obfuscatePackage(min.owner) + obfuscatedOwner;
						}

						break;
					case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
						InvokeDynamicInsnNode idin = (InvokeDynamicInsnNode) ain;

						idin.desc = obfuscateMethodFieldDesc(idin.desc);
						for(int i = 0; i < idin.bsmArgs.length; i++){
							if(idin.bsmArgs[i] instanceof Type){
								Type t = (Type) idin.bsmArgs[i];
								idin.bsmArgs[i] = Type.getType(obfuscateMethodFieldDesc(t.getDescriptor()));
							}else if(idin.bsmArgs[i] instanceof Handle){
								Handle h = (Handle) idin.bsmArgs[i];
								String newDesc = obfuscateMethodFieldDesc(h.getDesc());
								String newOwner = obfuscatePackage(h.getOwner()) + classNameRemapping.get(h.getOwner());
								String newName = methodNameRemapping.get(h.getOwner() + "." + h.getName());
								idin.bsmArgs[i] = new Handle(h.getTag(), newOwner, newName, newDesc);
							}
						}


						break;
					case AbstractInsnNode.TYPE_INSN:
						TypeInsnNode tin = (TypeInsnNode) ain;
						//just internal name if it's not an array
						//[L--internal name--; if it's an array
						if(tin.desc.charAt(0) == '['){
							tin.desc = obfuscateMethodFieldDesc(tin.desc);
						}else{
							String obfuscatedInternalName = classNameRemapping.get(tin.desc);
							if(obfuscatedInternalName != null){
								tin.desc = obfuscatePackage(tin.desc) + obfuscatedInternalName;
							}
						}
						break;
					case AbstractInsnNode.LDC_INSN:
						LdcInsnNode lin = (LdcInsnNode) ain;
						//TODO
						break;
					case AbstractInsnNode.FRAME:
						FrameNode fn = (FrameNode) ain;
						if(fn.local != null){
							ListIterator<Object> iter = fn.local.listIterator();
							while(iter.hasNext()){
								Object next = iter.next();
								if(next != null && next instanceof String){
									String s = (String) next;
									if(s.charAt(0) == '['){
										iter.set(obfuscateMethodFieldDesc(s));
									}else{
										String obfuscated = classNameRemapping.get(s);
										if(obfuscated != null){
											iter.set(obfuscatePackage(s) + obfuscated);
										}
									}
								}
							}
						}
						if(fn.stack != null){
							ListIterator<Object> iter = fn.stack.listIterator();
							while(iter.hasNext()){
								Object next = iter.next();
								if(next != null && next instanceof String){
									String s = (String) next;
									if(s.charAt(0) == '['){
										iter.set(obfuscateMethodFieldDesc(s));
									}else{
										String obfuscated = classNameRemapping.get(s);
										if(obfuscated != null){
											iter.set(obfuscatePackage(s) + obfuscated);
										}
									}
								}
							}
						}
						break;
					case AbstractInsnNode.LINE:
						LineNumberNode lnn = (LineNumberNode) ain;
						lnn.line = random.nextInt(10000);
						break;
					case AbstractInsnNode.MULTIANEWARRAY_INSN:
						MultiANewArrayInsnNode manain = (MultiANewArrayInsnNode) ain;
						manain.desc = obfuscateMethodFieldDesc(manain.desc);
						break;
					default:
						break;
					}
				}
			}
		}

		outputObfuscationMapToFile();
	}

	private static final Random random = new Random();

	public void outputObfuscationMapToFile() {
		if(outputFileForObfuscationMap == null) return;
		if(!outputFileForObfuscationMap.exists()){
			outputFileForObfuscationMap.getParentFile().mkdirs();
		}else{
			outputFileForObfuscationMap.delete();
		}
		try(final BufferedWriter bw = new BufferedWriter(new FileWriter(outputFileForObfuscationMap))){
			for(String deobfuscatedClassName : classNameRemapping.keySet()){
				String obfuscatedPackage = obfuscatePackage(deobfuscatedClassName);
				String obfuscatedClassNameNoPackage = classNameRemapping.get(deobfuscatedClassName);
				bw.write(deobfuscatedClassName + " --> " + obfuscatedPackage + obfuscatedClassNameNoPackage);
				for(String deobfuscatedFieldReference : fieldNameRemapping.keySet()){
					if(deobfuscatedFieldReference.substring(0, deobfuscatedFieldReference.indexOf('.')).equals(deobfuscatedClassName)){
						bw.newLine();
						bw.write("    ");
						String deobfuscatedFieldName = deobfuscatedFieldReference.substring(deobfuscatedFieldReference.indexOf('.') + 1);
						String obfuscatedFieldName = fieldNameRemapping.get(deobfuscatedFieldReference);
						bw.write("Field " + deobfuscatedFieldName + " --> " + obfuscatedFieldName);
					}
				}
				for(String deobfuscatedMethodReference : methodNameRemapping.keySet()){
					if(deobfuscatedMethodReference.substring(0, deobfuscatedMethodReference.indexOf('.')).equals(deobfuscatedClassName)){
						bw.newLine();
						bw.write("    ");
						String deobfuscatedFieldName = deobfuscatedMethodReference.substring(deobfuscatedMethodReference.indexOf('.') + 1);
						String obfuscatedFieldName = methodNameRemapping.get(deobfuscatedMethodReference);
						bw.write("Method " + deobfuscatedFieldName + " --> " + obfuscatedFieldName);
					}
				}
				bw.newLine();
				bw.newLine();
			}
			bw.flush();
		}catch(IOException e){
			e.printStackTrace();
			System.err.println("Was not able to save obfuscation map to file " + outputFileForObfuscationMap);
		}
	}

}
