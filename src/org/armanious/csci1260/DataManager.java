package org.armanious.csci1260;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.armanious.csci1260.temporaries.ConstantTemporary;
import org.armanious.csci1260.temporaries.Temporary;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class DataManager {

	//copied from RedundantComputationRemover
	public static int getStoreOpcode(Type t){
		if(t == Type.BOOLEAN_TYPE ||
				t == Type.INT_TYPE ||
				t == Type.BYTE_TYPE ||
				t == Type.CHAR_TYPE ||
				t == Type.SHORT_TYPE){
			return Opcodes.ISTORE;
		}else if(t == Type.DOUBLE_TYPE){
			return Opcodes.DSTORE;
		}else if(t == Type.FLOAT_TYPE){
			return Opcodes.FSTORE;
		}else if(t == Type.LONG_TYPE){
			return Opcodes.LSTORE;
		}
		return Opcodes.ASTORE;
	}

	public static int getLoadOpcode(Type t){
		if(t == Type.BOOLEAN_TYPE ||
				t == Type.INT_TYPE ||
				t == Type.BYTE_TYPE ||
				t == Type.CHAR_TYPE ||
				t == Type.SHORT_TYPE){
			return Opcodes.ILOAD;
		}else if(t == Type.DOUBLE_TYPE){
			return Opcodes.DLOAD;
		}else if(t == Type.FLOAT_TYPE){
			return Opcodes.FLOAD;
		}else if(t == Type.LONG_TYPE){
			return Opcodes.LLOAD;
		}
		return Opcodes.ALOAD;
	}

	public Type getCommonSuperType(Type t1, Type t2){
		if(t1.equals(t2)){
			return t1;
		}else if(t1.getSort() <= Type.INT && t2.getSort() <= Type.INT){
			switch(Math.max(t1.getSort(), t2.getSort())){
			case Type.BOOLEAN:
				return Type.BOOLEAN_TYPE;
			case Type.CHAR:
				return Type.CHAR_TYPE;
			case Type.BYTE:
				return Type.BYTE_TYPE;
			case Type.SHORT:
				return Type.SHORT_TYPE;
			case Type.INT:
				return Type.INT_TYPE;
			}
		}
		if(t1.getSort() != t2.getSort() || 
				t1.getSort() == Type.ARRAY){
			return Type.getType(Object.class);
		}
		if(t1.getSort() == Type.OBJECT || t1.getSort() == Type.METHOD){ //t1.sort == t2.sort == Type.OBJECT
			ClassHierarchyTreeNode chtn1 = getClassHierarchyTreeNode(t1.getInternalName());
			ClassHierarchyTreeNode chtn2 = getClassHierarchyTreeNode(t2.getInternalName());
			if(chtn1 == null || chtn2 == null){
				return Type.getType(Object.class);
			}
			do {
				chtn2 = getClassHierarchyTreeNode(t2.getInternalName());
				do {
					if(chtn1 == chtn2){
						return Type.getType(chtn1.name);
					}
				}while((chtn2 = chtn2.superNode) != null);
			} while((chtn1 = chtn1.superNode) != null);
		}
		return null;
	}

	public Type getCommonSuperType(Temporary[] temps){
		Type t = null;
		for(int i = 0; i < temps.length; i++){
			if(temps[i] instanceof ConstantTemporary && ((ConstantTemporary)temps[i]).getValue() == null){
				continue;
			}
			if(t == null){
				if(temps[i] != null){
					t = temps[i].getType();
					continue;
				}
			}
			if(temps[i] != null){
				t = getCommonSuperType(t, temps[i].getType());
			}
		}
		return t;
	}

	

	/*public static class LocalVariableReference extends Temporary {

		private final Temporary value;

		public LocalVariableReference(AbstractInsnNode declaration, Temporary value) {
			super(declaration, value.type);
			this.value = value;
		}

		@Override
		public AbstractInsnNode getContiguousBlockStart() {
			return getDeclaration();
		}

		@Override
		public boolean equals(Object o) {
			return value.equals(o);
		}

		@Override
		public String toString() {
			return value.toString();
		}

	}*/

	public static class ClassHierarchyTreeNode {

		public static final HashMap<String, ClassHierarchyTreeNode> GLOBAL = new HashMap<>();

		public final String name;
		public final ClassHierarchyTreeNode superNode;
		public Set<ClassHierarchyTreeNode> implementedInterfaceNodes;
		public Set<ClassHierarchyTreeNode> subclassNodes;

		public ClassHierarchyTreeNode(String name, ClassHierarchyTreeNode parent){
			this.name = name;
			superNode = parent;
			if(superNode != null){
				if(superNode.subclassNodes == null)
					superNode.subclassNodes = new HashSet<>();
				superNode.subclassNodes.add(this);
			}
			GLOBAL.put(name, this);
		}

		public void addInterfaceNode(ClassHierarchyTreeNode interfaceNode){
			if(implementedInterfaceNodes == null)
				implementedInterfaceNodes = new HashSet<>();
			implementedInterfaceNodes.add(interfaceNode);
			if(interfaceNode.subclassNodes == null)
				interfaceNode.subclassNodes = new HashSet<>();
			interfaceNode.subclassNodes.add(this);
		}

		@Override
		public int hashCode() {
			return name.hashCode();
			//we want "java/lang/Object" to effective equal a ClassHierarchyTreeNode representing it
		}

	}

	public static class MethodCallGraphNode {

		public final MethodNode mn;
		public final Set<MethodCallGraphNode> successors = new HashSet<>();
		public final Set<MethodCallGraphNode> predecessors = new HashSet<>();

		public MethodCallGraphNode(MethodNode mn){
			this.mn = mn;
		}

		public void addSuccessor(MethodCallGraphNode destMcgn) {
			successors.add(destMcgn);
			destMcgn.predecessors.add(this);
		}

	}

	public static class ForeignExecutableCallGraphNode extends MethodCallGraphNode {

		public final Executable e; //either a Method or Constructor

		public ForeignExecutableCallGraphNode(Executable e){
			super(null);
			this.e = e;
		}

	}

	static final Level DEFAULT_LOG_LEVEL = Level.FINE;
	static final String TO_DEBUG = null;// "test/hi/Hello.loopTest()V";

	static final Logger log = Logger.getLogger("DataManager");
	static {
		log.setLevel(DEFAULT_LOG_LEVEL);
		log.addHandler(new Handler(){
			@Override
			public void publish(LogRecord record) {
				if(log.getLevel().intValue() <= record.getLevel().intValue()){
					if(record.getLevel().intValue() >= Level.SEVERE.intValue()){
						//System.err.println(record.getLoggerName() + ": " + record.getMessage());
					}else{
						System.out.println(record.getLoggerName() + ": " + record.getMessage());
					}
				}
			}
			@Override
			public void flush() {
				System.out.flush();
				System.err.flush();
			}
			@Override
			public void close() throws SecurityException {}
		});
	}

	//TODO collections, math
	//for the purpose of this project, it is sufficient to predetermine whether or not
	//only a limited number of Java library methods are side-effect free
	public static final String[] METHODS_KNOWN_NO_SIDE_EFFECTS = {
			"java/lang/Class.getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
			"java/lang/Class.getDeclaredMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
			"java/lang/Class.getField(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Field;",
			"java/lang/Class.getDeclaredField(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Field;",
			"java/lang/Class.forName(Ljava/lang/String;)Ljava/lang/Class;",
			"java/lang/Class.forName(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;",
			"java/util/ArrayList.get(I)Ljava/lang/Object;",
			"java/util/ArrayList.size()I",
			"java/lang/Math.max(II)I",
			"java/lang/Math.min(II)I",
	};
	static {
		Arrays.sort(METHODS_KNOWN_NO_SIDE_EFFECTS);
	}

	public final ArrayList<ClassNode> classes;

	public final HashMap<String, ClassNode> nameToClassNodeMap = new HashMap<>();
	public final ClassHierarchyTreeNode root = new ClassHierarchyTreeNode("java/lang/Object", null);
	public final HashMap<FieldNode, ClassNode> fieldNodeToOwnerMap = new HashMap<>();
	public final HashMap<MethodNode, ClassNode> methodNodeToOwnerMap = new HashMap<>();
	public final HashMap<FieldNode, Set<Tuple<MethodNode, FieldInsnNode>>> fieldsModifiedByMethodsMap = new HashMap<>();
	public final HashMap<Object, MethodCallGraphNode> methodCallGraph = new HashMap<>();
	//key is either MethodNode or Executable, the latter of which can be a Method or Constructor (reflection)
	public final Map<MethodNode, MethodInformation> methodInformations = new ConcurrentHashMap<>();

	public DataManager(ArrayList<ClassNode> classes){
		this.classes = classes;
		gatherInformation();
	}

	private void reset(){
		nameToClassNodeMap.clear();
		if(root.subclassNodes != null)
			root.subclassNodes.clear();
		fieldNodeToOwnerMap.clear();
		methodNodeToOwnerMap.clear();
		fieldsModifiedByMethodsMap.clear();
		methodCallGraph.clear();
		methodInformations.clear();
	}

	public ClassNode getClassNode(String name){
		return nameToClassNodeMap.get(name);
	}

	public ClassHierarchyTreeNode getClassHierarchyTreeNode(String name){
		return ClassHierarchyTreeNode.GLOBAL.get(name);
	}

	ClassHierarchyTreeNode defineClassHierarchyTreeNode(String className){
		ClassHierarchyTreeNode node = ClassHierarchyTreeNode.GLOBAL.get(className);
		if(node == null){
			ClassNode cn = getClassNode(className);
			if(cn != null){
				ClassHierarchyTreeNode parent = defineClassHierarchyTreeNode(cn.superName);
				if(parent != null){
					node = new ClassHierarchyTreeNode(cn.name, parent);
					//node automatically added to GLOBAL in constructor
					if(cn.interfaces != null && cn.interfaces.size() > 0){
						for(String interfaceName : cn.interfaces){
							ClassHierarchyTreeNode chtn = defineClassHierarchyTreeNode(interfaceName);
							if(chtn != null){
								node.addInterfaceNode(chtn);
							}
						}
					}
				}
			}else{
				try {
					Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass(className.replace('/', '.'));
					if(clazz.isInterface()){
						node = new ClassHierarchyTreeNode(className, root);
					}else{
						node = new ClassHierarchyTreeNode(className, defineClassHierarchyTreeNode(clazz.getSuperclass().getName().replace('.', '/')));
					}
					for(Class<?> interfaceClazz : clazz.getInterfaces()){
						node.addInterfaceNode(defineClassHierarchyTreeNode(interfaceClazz.getName().replace('.', '/')));
					}
				} catch (ReflectiveOperationException e) {
					System.err.println("Warning: cannot place " + className + " correctly in class hierarchy.");
				}
				//use reflection
			}
		}
		return node;
	}

	private MethodCallGraphNode defineMethodCallGraphNode(MethodNode mn){
		MethodCallGraphNode mcgn = methodCallGraph.get(mn);
		if(mcgn == null){
			mcgn = new MethodCallGraphNode(mn);
			methodCallGraph.put(mn, mcgn);
		}
		return mcgn;
	}

	private MethodCallGraphNode defineForeignMethodCallGraphNode(Executable methodOrConstructor){
		MethodCallGraphNode mcgn = methodCallGraph.get(methodOrConstructor);
		if(mcgn == null){
			mcgn = new ForeignExecutableCallGraphNode(methodOrConstructor);
			methodCallGraph.put(methodOrConstructor, mcgn);
		}
		return mcgn;
	}
	
	private void defineMethodCallGraph(){
		for(ClassNode cn : classes){
			for(MethodNode mn : cn.methods){
				MethodCallGraphNode mcgn = defineMethodCallGraphNode(mn);
				if(mcgn.mn.instructions != null && mcgn.mn.instructions.size() > 0){
					for(AbstractInsnNode ain = mcgn.mn.instructions.getFirst(); ain != null; ain = ain.getNext()){
						if(ain.getType() == AbstractInsnNode.METHOD_INSN){
							MethodInsnNode min = (MethodInsnNode) ain;
							if(min.owner.charAt(0) == '['){
								//array clone; we don't need to analyze really
								continue;
							}

							ClassNode destOwner = nameToClassNodeMap.get(min.owner);
							MethodCallGraphNode destMcgn = null;
							if(destOwner == null){
								try {
									Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass(min.owner.replace('/', '.'));
									Type[] expectedTypes = Type.getArgumentTypes(min.desc);

									if(min.name.equals("<init>")){
										outer: for(Constructor<?> c : clazz.getDeclaredConstructors()){
											Class<?>[] paramTypes = c.getParameterTypes();
											if(paramTypes.length != expectedTypes.length) continue;

											for(int i = 0; i < paramTypes.length; i++){
												String a = paramTypes[i].getName().replace('.', '/');
												String b = 
														(expectedTypes[i].getSort() == Type.OBJECT || expectedTypes[i].getSort() == Type.ARRAY)
														? expectedTypes[i].getInternalName() : expectedTypes[i].getClassName();
														//?  L + something + ;            :     int or byte etc
														if(!a.equals(b))
															continue outer;
											}
											destMcgn = defineForeignMethodCallGraphNode(c);
											break;
										}
									}else{
										search: do{
											for(Method m : clazz.getDeclaredMethods()){
												if(m.getName().equals(min.name)){
													if(Arrays.equals(Type.getArgumentTypes(m), expectedTypes)){
														//shallow comparison is sufficient
														destMcgn = defineForeignMethodCallGraphNode(m);
														break search;
													}
												}
											}
											for(Class<?> interfaceClazz : clazz.getInterfaces()){
												do{
													for(Method m : interfaceClazz.getDeclaredMethods()){
														if(m.getName().equals(min.name)){
															if(Arrays.equals(Type.getArgumentTypes(m), expectedTypes)){
																//shallow comparison is sufficient
																destMcgn = defineForeignMethodCallGraphNode(m);
																break search;
															}
														}
													}
												}while((interfaceClazz = interfaceClazz.getSuperclass()) != null);
											}
										}while((clazz = clazz.getSuperclass()) != null);
									}
								} catch (ClassNotFoundException e) {
									destMcgn = null;
								}
							}else{
								MethodNode destMn = nameToClassNodeMap.get(min.owner).getMethodNode(min.name, min.desc);
								destMcgn = defineMethodCallGraphNode(destMn);
							}
							if(destMcgn == null){
								System.err.println("Error adding " + min.owner + "#" + min.name + min.desc + " to method call graph.");
								continue;
							}
							mcgn.addSuccessor(destMcgn);
						}
					}
				}
			}
		}
	}

	private void defineFieldsModifiedByMethodsMap(){
		for(ClassNode modifyingCn : classes){
			for(MethodNode modifyingMn : modifyingCn.methods){
				if(modifyingMn.instructions != null && modifyingMn.instructions.size() > 0){
					for(AbstractInsnNode ain = modifyingMn.instructions.getFirst(); ain != null; ain = ain.getNext()){
						if(ain.getOpcode() == Opcodes.PUTFIELD ||
								ain.getOpcode() == Opcodes.PUTSTATIC){
							FieldInsnNode fin = (FieldInsnNode) ain;
							ClassNode cn = nameToClassNodeMap.get(fin.owner);
							if(cn != null){
								FieldNode fn = cn.getFieldNode(fin.name);

								Set<Tuple<MethodNode, FieldInsnNode>> modifierSet = fieldsModifiedByMethodsMap.get(fn);
								if(modifierSet == null){
									modifierSet = new HashSet<>();
									fieldsModifiedByMethodsMap.put(fn, modifierSet);
								}
								modifierSet.add(new Tuple<>(modifyingMn, fin));
							}
						}
					}
				}
			}
		}
	}
	
	public void gatherInformation(){
		reset();
		final long start = System.currentTimeMillis();

		for(ClassNode cn : classes){
			nameToClassNodeMap.put(cn.name, cn);
		}

		for(ClassNode cn : classes){
			defineClassHierarchyTreeNode(cn.name);
		}

		for(ClassNode cn : classes){
			for(FieldNode fn : cn.fields){
				fieldNodeToOwnerMap.put(fn, cn);
			}
			for(MethodNode mn : cn.methods){
				methodNodeToOwnerMap.put(mn, cn);
			}
		}

		defineFieldsModifiedByMethodsMap();

		defineMethodCallGraph();
		
		for(ClassNode cn : classes){
			for(MethodNode mn : cn.methods){
				if(Modifier.isNative(mn.access) || Modifier.isAbstract(mn.access)){
					continue;
				}
				methodInformations.put(mn, new MethodInformation(this, mn));
			}
		}
		

		final long end = System.currentTimeMillis();
		System.out.println("Gathered all relevant information in " + (end - start) + " ms.");

	}

}
