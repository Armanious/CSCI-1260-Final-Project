package org.armanious.csci1260;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

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

	public static abstract class Temporary {

		public static int numTemporaries = 0;
		public static List<Temporary> GLOBAL_TEMPORARIES = Collections.synchronizedList(new ArrayList<Temporary>());

		public static final int NOT_CONSTANT = -1;
		public static final int CONSTANCY_UNKNOWN = 0;
		public static final int CONSTANT = 1;

		private Temporary parentTemporary = null;

		public final int index;
		public final Type type;

		private AbstractInsnNode declaration;

		public Map<AbstractInsnNode, MethodNode> references; 

		private boolean overrideConstancy = false;
		private int forcedConstancy = Integer.MAX_VALUE;

		public Temporary(AbstractInsnNode declaration, Type type){
			this.declaration = declaration;
			this.type = type;
			index = numTemporaries++;
			GLOBAL_TEMPORARIES.add(this);
		}

		int getConstancyInternal(){
			return CONSTANCY_UNKNOWN;
		}

		public final int getConstancy(){
			return parentTemporary != null ? parentTemporary.getConstancy() : (overrideConstancy ? forcedConstancy : getConstancyInternal());
		}

		public final void forceConstancy(int forcedConstancy){
			if(parentTemporary == null){
				overrideConstancy = true;
				this.forcedConstancy = forcedConstancy;
			}else{
				parentTemporary.forceConstancy(forcedConstancy);
			}
		}

		protected static int mergeConstancy(Temporary...others){
			int constancy = others[0].getConstancy();
			if(constancy == NOT_CONSTANT){
				return constancy;
			}
			for(int i = 1; i < others.length; i++){
				int otherConstancy = others[i].getConstancy(); //can be an expensive operation
				if(otherConstancy < constancy){
					constancy = otherConstancy;
					if(constancy == NOT_CONSTANT){
						break;
					}
				}
			}
			return constancy;
		}

		public AbstractInsnNode getDeclaration(){
			return declaration;
		}

		protected abstract void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list);

		protected void addCriticalTemporariesToList(ArrayList<Temporary> list){

		}

		public ArrayList<Temporary> getCriticalTemporaries(){
			ArrayList<Temporary> t = new ArrayList<Temporary>();
			addCriticalTemporariesToList(t);
			t.sort((t1,t2) -> {
				return (t1.getDeclaration() == null ? Integer.MAX_VALUE : t1.getDeclaration().getIndex())
						- (t2.getDeclaration() == null ? Integer.MAX_VALUE : t2.getDeclaration().getIndex());
			});
			return t;
		}

		protected abstract Temporary clone();

		public final Temporary cloneOnInstruction(AbstractInsnNode ain){
			Temporary t = clone();
			t.parentTemporary = parentTemporary == null ? this : parentTemporary;
			t.declaration = ain;
			return t;
		}

		public final ArrayList<AbstractInsnNode> getContiguousBlockSortedDebug(){
			final ArrayList<AbstractInsnNode> list = new ArrayList<>();
			addRelevantInstructionsToListSorted(list);
			System.err.println(getDeclaration().getOpcode() >= Opcodes.ILOAD && getDeclaration().getOpcode() <= Opcodes.ALOAD);
			System.out.println(toString() + " uses instructions: " + list.get(0).getIndex() + " to " + list.get(list.size() - 1).getIndex());
			return null;
		}

		public final ArrayList<AbstractInsnNode> getContiguousBlockSorted(){
			final ArrayList<AbstractInsnNode> list = new ArrayList<>();
			if(getDeclaration() != null && getDeclaration().getOpcode() >= Opcodes.ILOAD && getDeclaration().getOpcode() <= Opcodes.ALOAD){
				//cloned instruction with only a load; i.e. we are stored in a local variable
				//we don't need to go through the other checks
				list.add(getDeclaration());
				return list;
			}
			addRelevantInstructionsToListSorted(list);
			if(list.contains(null)){
				return null;
			}
			//list.sort((a1, a2) -> a1.getIndex() - a2.getIndex());
			int fingerIndex = 0;
			for(AbstractInsnNode ain = list.get(0); ain != null && ain != list.get(list.size() - 1).getNext(); ain = ain.getNext()){
				if(list.get(fingerIndex) != ain){
					//somethings interupting the list; only special cases allowed:
					switch(ain.getOpcode()){ //the one interrupting the list
					case Opcodes.DUP:
					case Opcodes.DUP2:
					case Opcodes.DUP2_X1:
					case Opcodes.DUP2_X2:
					case Opcodes.DUP_X1:
					case Opcodes.DUP_X2:
					case Opcodes.SWAP:
					case Opcodes.POP:
					case Opcodes.POP2:
						continue;
					default:
						return null;
					}
				}else{
					fingerIndex++; //match, increment in list
				}
			}
			return list;
		}

		public void addReference(AbstractInsnNode insn, MethodNode mn){
			if(references == null){
				references = new HashMap<>();
			}
			references.put(insn, mn);
		}

		public final Type getType(){
			return type;
		}

		public abstract boolean equals(Object o);

	}

	public static class FieldTemporary extends Temporary {

		private final FieldTemporary parent;
		private final boolean isConstant;
		private final Temporary objectRef;
		private final String owner;
		private final String name;

		private final Temporary value;
		private final boolean isVolatile;

		private int numWrites;

		//loading from LocalVariable from basic clone()
		private FieldTemporary(FieldTemporary parent, Temporary value, Type type, boolean isVolatile){
			super(null, type);
			this.parent = parent;
			isConstant = false;
			objectRef = null;
			owner = null;
			name = null;
			this.value = value;
			this.isVolatile = isVolatile;
		}

		private FieldTemporary(FieldTemporary parent, AbstractInsnNode insn, MethodNode mn, Temporary value, boolean isVolatile){
			super(insn, (insn.getOpcode() == Opcodes.GETFIELD || insn.getOpcode() == Opcodes.GETSTATIC) ? parent.getType() : Type.VOID_TYPE);
			this.parent = parent;
			isConstant = false;
			objectRef = null;
			owner = null;
			name = null;
			this.value = value;
			this.isVolatile = isVolatile;

			if(getType() != Type.VOID_TYPE){
				parent.numWrites++;
			}else{
				parent.addReference(insn, mn);
			}
		}

		public FieldTemporary(boolean isConstant, Temporary objectRef, String owner, String name, Type type, boolean isVolatile){
			super(null, type);
			this.parent = null;
			this.isConstant = isConstant;
			this.objectRef = objectRef;
			this.owner = owner;
			this.name = name;
			this.value = null;
			this.isVolatile = isVolatile;
		}

		public int getConstancyInternal() {
			return parent == null ? (isVolatile ? NOT_CONSTANT : (isConstant ? CONSTANT : (numWrites <= 1 ? CONSTANT : NOT_CONSTANT))) : parent.getConstancyInternal();
		}

		private String getOwner(){
			return parent == null ? owner : parent.getOwner();
		}

		private String getName(){
			return parent == null ? name : parent.getName();
		}

		private Temporary getObjectRef(){
			return parent == null ? objectRef : parent.getObjectRef();
		}

		public Temporary getValue(){
			return value;
		}

		@Override
		public boolean equals(Object o) {
			if(this == o) return true;
			if(!(o instanceof FieldTemporary)) return false;
			FieldTemporary ft = (FieldTemporary) o;
			if(!getOwner().equals(ft.getOwner()) || !getName().equals(ft.getName())){
				return false;
			}
			if(getValue() != ft.getValue() && (getValue() == null || ft.getValue() == null || !getValue().equals(ft.getValue()))){
				return false;
			}
			return (getObjectRef() == null) ? (ft.getObjectRef() == null) : (getObjectRef().equals(ft.getObjectRef()));
		}

		public String toString(){
			return (getObjectRef() == null ? getOwner() : getObjectRef().toString()) + "." + getName() + (getType() == Type.VOID_TYPE ? (" = " + getValue()) : "");
		}

		@Override
		protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
			if(getObjectRef() != null){
				getObjectRef().addRelevantInstructionsToListSorted(list);
			}
			if(getValue() != null){
				getValue().addRelevantInstructionsToListSorted(list);
			}
			list.add(getDeclaration());
		}

		@Override
		protected void addCriticalTemporariesToList(ArrayList<Temporary> list) {
			if(getValue() != null){
				list.add(getValue());
				//getValue().addCriticalTemporariesToList(list);
			}
			if(getObjectRef() != null){
				list.add(getObjectRef());
				//getObjectRef().addCriticalTemporariesToList(list);
			}
		}

		public boolean isVolatile() {
			return isVolatile;
		}

		@Override
		protected Temporary clone() {
			return new FieldTemporary(parent, value, getType(), isVolatile);
		}

		public FieldTemporary cloneSpecialCase(AbstractInsnNode ain, MethodNode mn, Temporary value){
			return new FieldTemporary(this, ain, mn, value, isVolatile);
		}

	}

	//NOT static because we need access to the enclosing DataManager class to determine side effects
	public class MethodInvocationTemporary extends Temporary {

		final Temporary[] args;
		public final String owner;
		public final String name;
		public final String desc;
		final boolean isStatic;

		private boolean calculatedSideEffects;
		private boolean hasSideEffects;

		public MethodInvocationTemporary(boolean isStatic, Temporary[] args, String owner, String name, String desc, Type returnType){
			super(null, returnType);
			this.args = args;
			this.owner = owner;
			this.name = name;
			this.isStatic = isStatic;
			this.desc = desc;
		}

		public MethodInvocationTemporary(AbstractInsnNode decl, Temporary[] args, String owner, String name, String desc, Type returnType){
			super(decl, returnType);
			this.args = args;
			this.owner = owner;
			this.name = name;
			this.desc = desc;
			isStatic = decl.getOpcode() == Opcodes.INVOKESTATIC || decl.getOpcode() == Opcodes.INVOKEDYNAMIC;
			for(Temporary arg : args){
				arg.addReference(decl, null);
			}
		}

		public boolean hasSideEffects(){
			if(!calculatedSideEffects){

				ClassNode cn = DataManager.this.getClassNode(owner);
				if(cn == null){
					//not a user-specified method, might be library
					String s = owner + "." + name + desc;
					hasSideEffects = Arrays.binarySearch(KNOWN_NO_SIDE_EFFECTS, s) < 0;
					calculatedSideEffects = true;
					return hasSideEffects;
				}
				MethodNode mn = cn.getMethodNode(name, desc);
				if(mn == null){
					return true;
				}
				MethodInformation mi = DataManager.this.methodInformations.get(mn);
				if(mi == null){
					return true;
				}
				hasSideEffects = mi.hasSideEffects();
				calculatedSideEffects = true;
			}
			return hasSideEffects;
		}

		public Temporary[] getArgs(){
			return args;
		}

		@Override
		public int getConstancyInternal() {
			return hasSideEffects() ? NOT_CONSTANT : mergeConstancy(args);
		}

		@Override
		public boolean equals(Object o) {
			if(this == o) return true;
			if(!(o instanceof MethodInvocationTemporary)) return false;
			MethodInvocationTemporary mit = (MethodInvocationTemporary) o;
			if(!owner.equals(mit.owner) || !name.equals(mit.name) || args.length != mit.args.length){
				return false;
			}
			for(int i = 0; i < args.length; i++){
				if(!args[i].equals(mit.args[i])){
					return false;
				}
			}
			return true;
		}

		public String toString(){
			if(isStatic){
				return owner + "." + name + Arrays.toString(args).replace('[', '(').replace(']', ')');
			}else{
				return args[0].toString() + "." + name + Arrays.toString(Arrays.copyOfRange(args, 1, args.length)).replace('[', '(').replace(']', ')');
			}
			//return "M()" + type.toString();
		}

		@Override
		protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
			for(int i = args.length - 1; i >= 0; i--){
				args[i].addRelevantInstructionsToListSorted(list);
			}
			list.add(getDeclaration());
		}

		@Override
		protected void addCriticalTemporariesToList(ArrayList<Temporary> list) {
			for(Temporary t : args){
				list.add(t);
				//t.addCriticalTemporariesToList(list);
			}
		}

		@Override
		protected Temporary clone() {
			return new MethodInvocationTemporary(isStatic, args.clone(), owner, name, desc, getType());
		}

	}

	public class InvokeSpecialTemporary extends MethodInvocationTemporary {


		public InvokeSpecialTemporary(AbstractInsnNode insn, Temporary[] args, String name, String desc) {
			//args will include the ObjectInstance Temporary
			/*
			 * new X  //X
			 * dup  //X, X
			 * aload 1 //X, X, a1
			 * invokespecial X //POPS ALL 3; pushed Constructor(X)
			 * astore 2 //a2 = Constructor(X) empty stack
			 */
			super(insn, args, args[0].getType().getInternalName(), name, desc, args[0].getType());
		}

		@Override
		public String toString() {
			return "new " + owner + Arrays.toString(Arrays.copyOfRange(args, 2, args.length)).replace('[', '(').replace(']', ')');
		}

		@Override
		protected Temporary clone() {
			return new InvokeSpecialTemporary(getDeclaration(), args, name, desc);
		}

	}

	public static class ConstantTemporary extends Temporary {

		private Object value;

		public ConstantTemporary(AbstractInsnNode decl, Object value, Type type){
			super(decl, type);
			this.value = value;
		}

		public void setValue(Object newValue){
			if(!(getDeclaration() instanceof LdcInsnNode)){
				System.err.println("Warning: cannot set value of cloned ConstantTemporary.");
				return;
			}
			((LdcInsnNode)getDeclaration()).cst = newValue;
			value = newValue;
		}

		@Override
		public int getConstancyInternal() {
			return CONSTANT;
		}

		@Override
		public boolean equals(Object o) {
			if(this == o) return true;
			if(!(o instanceof ConstantTemporary)) return false;
			Object otherValue = ((ConstantTemporary)o).value;
			if(value == otherValue) return true;
			if(value == null || otherValue == null) return false;
			return value.equals(otherValue);
		}

		public String toString(){
			if(value instanceof String){
				return "\"" + ((String)value) + "\"";
			}
			return value == null ? "ConstantNull" : String.valueOf(value);
		}

		public Object getValue(){
			return value;
		}

		@Override
		protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
			list.add(getDeclaration());
		}

		@Override
		protected Temporary clone() {
			return new ConstantTemporary(getDeclaration(), value, type);
		}

	}

	public static class ParameterTemporary extends Temporary {

		private final MethodNode owner;
		private final int index;

		private ParameterTemporary(AbstractInsnNode varInsnNode, MethodNode owner, int index, Type type){
			super(varInsnNode, type);
			this.owner = owner;
			this.index = index;
		}

		public ParameterTemporary(MethodNode owner, int index, Type type){
			this(null, owner, index, type);
		}

		@Override
		public boolean equals(Object o) {
			if(this == o) return true;
			if(!(o instanceof ParameterTemporary)) return false;
			ParameterTemporary pt = (ParameterTemporary) o;
			return pt.owner == owner && pt.index == index;
		}

		public String toString(){
			return index == -1 ? "this" : ("local_" + index);//(type.toString() + " (Param " + index + ")");
		}

		@Override
		protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
			list.add(getDeclaration());
		}

		@Override
		protected Temporary clone() {
			return new ParameterTemporary(owner, index, type);
		}

	}

	public static class ArrayReferenceTemporary extends Temporary {

		private static Type computeArrayDereferenceType(Temporary arrayRef){
			return Type.getType(arrayRef.getType().getDescriptor().substring(1));
		}

		public final Temporary arrayRef;
		public final Temporary index;

		public ArrayReferenceTemporary(AbstractInsnNode decl, Temporary arrayRef, Temporary index) {
			super(decl, computeArrayDereferenceType(arrayRef));
			if(arrayRef instanceof ConstantTemporary && ((ConstantTemporary)arrayRef).value == null){
				System.err.println("bpp");
			}
			this.arrayRef = arrayRef;
			this.index = index;
			arrayRef.addReference(decl, null);
		}

		@Override
		public int getConstancyInternal() {
			return mergeConstancy(arrayRef, index);
		}

		@Override
		public boolean equals(Object o) {
			if(this == o) return true;
			if(!(o instanceof ArrayReferenceTemporary)) return false;
			ArrayReferenceTemporary art = (ArrayReferenceTemporary) o;
			return art.arrayRef.equals(arrayRef) && art.index.equals(index);
		}

		public String toString(){
			return arrayRef.toString() + "[" + index.toString() + "]";
		}

		@Override
		protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
			arrayRef.addRelevantInstructionsToListSorted(list);
			index.addRelevantInstructionsToListSorted(list);
			list.add(getDeclaration());
		}

		@Override
		protected void addCriticalTemporariesToList(ArrayList<Temporary> list) {
			list.add(arrayRef);
			//arrayRef.addCriticalTemporariesToList(list);
			list.add(index);
			//index.addCriticalTemporariesToList(list);
		}
		
		//int[][][] arr = new int[100][200][300];
		//arr.length = 100
		//arr[i].length = 200
		//arr[i][j].length = 300;
		//we are handling arr[i].length, with arrayRef = arr (ArrayInstance), index = i
		//and arr[i][j].length with arrayRef = arr[i] (ArrayReference), index = j
		public Temporary attemptGetReferencedArrayLength(){
			Temporary arrayInstance = arrayRef;
			int dimensionToGetLengthOf = 0;
			while(!(arrayInstance instanceof ArrayInstanceTemporary)){
				arrayInstance = ((ArrayReferenceTemporary)arrayInstance).arrayRef;
				dimensionToGetLengthOf++;
			}
			return ((ArrayInstanceTemporary)arrayInstance).dimensionCounts[dimensionToGetLengthOf];
		}

		@Override
		protected Temporary clone() {
			return new ArrayReferenceTemporary(getDeclaration(), arrayRef, index);
		}

	}

	public static class BinaryOperatorTemporary extends Temporary {

		private static final String[] TEXT_REP = {"+","-","*","/","%","<<",
				">>",">>>","&","|","^"};

		public static final int ADD = 0;
		public static final int SUB = 1;
		public static final int MUL = 2;
		public static final int DIV = 3;
		public static final int REM = 4;
		public static final int SHL = 5;
		public static final int SHR = 6;
		public static final int USHR = 7;
		public static final int AND = 8;
		public static final int OR = 9;
		public static final int XOR = 10;

		private final Temporary rhs;
		private final Temporary lhs;
		private final int arithmeticType;


		private static Type determineType(Temporary lhst, Temporary rhst){
			Type lhs = lhst.getType();
			Type rhs = rhst.getType();
			if(lhs == rhs){
				return lhs;
			}else if(lhs == Type.BYTE_TYPE || lhs == Type.SHORT_TYPE || lhs == Type.CHAR_TYPE || lhs == Type.BOOLEAN_TYPE){
				return rhs;
			}else if(rhs == Type.BYTE_TYPE || rhs == Type.SHORT_TYPE || rhs == Type.CHAR_TYPE || rhs == Type.BOOLEAN_TYPE){
				return lhs;
			}else if(lhs == Type.LONG_TYPE && rhs == Type.INT_TYPE){
				//LSHL, LSHR, LUSHR
				return rhs;
			}
			lhst.getType();
			((PhiTemporary)lhst).debugType();
			rhst.getType();
			new RuntimeException(lhs + ", " + rhs).printStackTrace();
			throw new RuntimeException();
		}

		public BinaryOperatorTemporary(AbstractInsnNode decl, Temporary rhs, Temporary lhs, int arithmeticType) {
			super(decl, determineType(lhs, rhs));
			this.rhs = rhs;
			this.lhs = lhs;
			this.arithmeticType = arithmeticType;

			rhs.addReference(decl, null);
			lhs.addReference(decl, null);
		}

		public int getArithmeticType(){
			return arithmeticType;
		}

		public Temporary getLeftHandSideTemporary(){
			return lhs;
		}

		public Temporary getRightHandSideTemporary(){
			return rhs;
		}

		@Override
		public int getConstancyInternal() {
			return mergeConstancy(rhs, lhs);
		}

		@Override
		public boolean equals(Object o) {
			if(this == o) return true;
			if(!(o instanceof BinaryOperatorTemporary)) return false;
			BinaryOperatorTemporary at = (BinaryOperatorTemporary) o;
			if(rhs.equals(at.rhs) && lhs.equals(at.lhs)) return true;
			if(arithmeticType == ADD || arithmeticType == MUL){
				if(rhs.equals(at.lhs) && lhs.equals(at.rhs)){
					return true;
				}
			}
			return false;
		}

		public String toString(){
			return "(" + lhs.toString() + TEXT_REP[arithmeticType] + rhs.toString() + ")";
		}

		@Override
		protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
			lhs.addRelevantInstructionsToListSorted(list);
			rhs.addRelevantInstructionsToListSorted(list);
			list.add(getDeclaration());
		}

		@Override
		protected void addCriticalTemporariesToList(ArrayList<Temporary> list) {
			list.add(lhs);
			//lhs.addCriticalTemporariesToList(list);
			list.add(rhs);
			//rhs.addCriticalTemporariesToList(list);
		}

		@Override
		protected Temporary clone() {
			return new BinaryOperatorTemporary(getDeclaration(), rhs, lhs, arithmeticType);
		}

	}

	public static class NegateOperatorTemporary extends Temporary {

		private final Temporary tmp;

		public NegateOperatorTemporary(AbstractInsnNode decl, Temporary tmp){
			super(decl, tmp.getType());
			this.tmp = tmp;
			tmp.addReference(decl, null);
		}

		@Override
		public int getConstancyInternal() {
			return tmp.getConstancyInternal();
		}

		@Override
		public boolean equals(Object o) {
			if(this == o) return true;
			if(!(o instanceof NegateOperatorTemporary)) return false;
			NegateOperatorTemporary not = (NegateOperatorTemporary) o;
			return tmp.equals(not.tmp);
		}

		public String toString(){
			return "-" + tmp.toString();
		}

		public Temporary getOperand(){
			return tmp;
		}

		@Override
		protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
			tmp.addRelevantInstructionsToListSorted(list);
			list.add(getDeclaration());
		}

		@Override
		protected void addCriticalTemporariesToList(ArrayList<Temporary> list) {
			list.add(tmp);
			//tmp.addCriticalTemporariesToList(list);
		}

		@Override
		protected Temporary clone() {
			return new NegateOperatorTemporary(getDeclaration(), tmp);
		}

	}

	public static class CastOperatorTemporary extends Temporary {

		private static Type getResultType(int opcode){
			switch(opcode){
			case Opcodes.L2I:
			case Opcodes.F2I:
			case Opcodes.D2I:
				return Type.INT_TYPE;
			case Opcodes.I2L:
			case Opcodes.F2L:
			case Opcodes.D2L:
				return Type.LONG_TYPE;
			case Opcodes.I2F:
			case Opcodes.L2F:
			case Opcodes.D2F:
				return Type.FLOAT_TYPE;
			case Opcodes.I2D:
			case Opcodes.L2D:
			case Opcodes.F2D:
				return Type.DOUBLE_TYPE;
			case Opcodes.I2B:
				return Type.BYTE_TYPE;
			case Opcodes.I2C:
				return Type.CHAR_TYPE;
			case Opcodes.I2S:
				return Type.SHORT_TYPE;
			}
			throw new IllegalArgumentException("Cast opcode: " + opcode);
		}

		private final Temporary tmp;
		private final int opcode;

		public CastOperatorTemporary(AbstractInsnNode decl, Temporary tmp, int opcode){
			super(decl, getResultType(opcode));
			this.tmp = tmp;
			this.opcode = opcode;
			tmp.addReference(decl, null);
		}

		public Temporary getOperand(){
			return tmp;
		}

		@Override
		public int getConstancyInternal() {
			return tmp.getConstancyInternal();
		}

		@Override
		public boolean equals(Object o) {
			if(this == o) return true;
			if(!(o instanceof CastOperatorTemporary)) return false;
			CastOperatorTemporary ct = (CastOperatorTemporary) o;
			return getType().equals(ct.getType()) && tmp.equals(ct.tmp);
		}

		public String toString(){
			return "((" + getType().toString() + ") " + tmp.toString() + ")";
		}

		@Override
		protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
			tmp.addRelevantInstructionsToListSorted(list);
			list.add(getDeclaration());
		}

		@Override
		protected void addCriticalTemporariesToList(ArrayList<Temporary> list) {
			list.add(tmp);
			//tmp.addCriticalTemporariesToList(list);
		}

		@Override
		protected Temporary clone() {
			return new CastOperatorTemporary(getDeclaration(), tmp, opcode);
		}

	}

	public static class CompareOperatorTemporary extends Temporary {

		public final Temporary rhs;
		public final Temporary lhs;
		public final int opcode;

		public CompareOperatorTemporary(AbstractInsnNode decl, Temporary lhs, Temporary rhs, int opcode) {
			super(decl, Type.INT_TYPE);
			this.rhs = rhs;
			this.lhs = lhs;
			this.opcode = opcode;
			if(rhs != null){
				rhs.addReference(decl, null);
			}
			lhs.addReference(decl, null);
		}

		public int getConstancyInternal() {
			return rhs == null ? lhs.getConstancyInternal() : mergeConstancy(rhs, lhs);
		}

		@Override
		public boolean equals(Object o) {
			if(this == o) return true;
			if(!(o instanceof CompareOperatorTemporary)) return false;
			CompareOperatorTemporary cot = (CompareOperatorTemporary) o;
			return opcode == cot.opcode && rhs.equals(cot.rhs) && 
					(lhs == null ? cot.lhs == null : lhs.equals(cot.lhs));
		}

		public String toString(){
			return rhs == null ? ("CMP(" + lhs.toString() + ")") : (lhs.toString() + " CMP(" + opcode + ") " + rhs.toString());
		}

		@Override
		protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
			lhs.addRelevantInstructionsToListSorted(list);
			if(rhs != null){
				rhs.addRelevantInstructionsToListSorted(list);
			}
			list.add(getDeclaration());
		}

		@Override
		protected void addCriticalTemporariesToList(ArrayList<Temporary> list) {
			list.add(lhs);
			//lhs.addCriticalTemporariesToList(list);
			if(rhs != null){
				list.add(rhs);
			}
			//rhs.addCriticalTemporariesToList(list);
		}

		@Override
		protected Temporary clone() {
			return new CompareOperatorTemporary(getDeclaration(), lhs, rhs, opcode);
		}

	}

	public static class ObjectInstanceTemporary extends Temporary {

		private static HashMap<Object, Boolean> isDupped = new HashMap<>();

		private final Object fakeValue;

		private ObjectInstanceTemporary(AbstractInsnNode decl, Type type, Object val){
			super(decl, type);
			this.fakeValue = val;
			isDupped.put(fakeValue, false);
		}

		public ObjectInstanceTemporary(AbstractInsnNode decl, Type type){
			this(decl, type, new Object());
		}

		@Override
		public int getConstancyInternal() {
			return CONSTANT;
		}

		public boolean isDupped(){
			return isDupped.get(fakeValue);
		}

		public void setIsDupped(boolean newIsDupped){
			isDupped.put(fakeValue, newIsDupped);
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof ObjectInstanceTemporary && (((ObjectInstanceTemporary)o).fakeValue == fakeValue);
		}

		public String toString(){
			return type.toString() + "@" + fakeValue.hashCode();
		}

		@Override
		protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
			list.add(getDeclaration());
		}

		@Override
		protected Temporary clone() {
			return new ObjectInstanceTemporary(getDeclaration(), type, fakeValue);
		}

	}

	public static class ArrayInstanceTemporary extends Temporary {

		private final Object fakeValue;
		public final Temporary[] dimensionCounts;

		private static Type createArrayType(Type type, int numDimensions){
			final StringBuilder sb = new StringBuilder();
			for(int i = 0; i < numDimensions; i++){
				sb.append('[');
			}
			sb.append(type.getDescriptor());
			return Type.getType(sb.toString());
		}

		private ArrayInstanceTemporary(Object fakeValue, AbstractInsnNode decl, Type type, Temporary...dimensionCounts){
			super(decl, dimensionCounts.length > 1 ? type : createArrayType(type, dimensionCounts.length));//createArrayType(type, dimensionCounts.length));
			this.dimensionCounts = dimensionCounts;
			for(Temporary t : dimensionCounts){
				t.addReference(decl, null);
			}
			this.fakeValue = fakeValue;
		}

		public ArrayInstanceTemporary(AbstractInsnNode decl, Type type, Temporary...dimensionCounts){
			this(new Object(), decl, type, dimensionCounts);
		}

		@Override
		public int getConstancyInternal() {
			return mergeConstancy(dimensionCounts);
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof ArrayInstanceTemporary && (((ArrayInstanceTemporary)o).fakeValue == fakeValue);
		}

		public String toString(){
			StringBuilder sb = new StringBuilder("new ").append(type.toString());
			for(Temporary dim : dimensionCounts){
				sb.append('[').append(dim.toString()).append(']');
			}
			return sb.toString();
		}

		@Override
		protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
			if(getDeclaration().getOpcode() == Opcodes.NEWARRAY || getDeclaration().getOpcode() == Opcodes.ANEWARRAY || getDeclaration().getOpcode() == Opcodes.MULTIANEWARRAY){
				for(int i = dimensionCounts.length - 1; i >= 0; i--){
					dimensionCounts[i].addRelevantInstructionsToListSorted(list);
				}
			}
			list.add(getDeclaration());
		}

		@Override
		protected void addCriticalTemporariesToList(ArrayList<Temporary> list) {
			for(Temporary t : dimensionCounts){
				list.add(t);
				//t.addCriticalTemporariesToList(list);
			}
		}

		@Override
		protected Temporary clone() {
			return new ArrayInstanceTemporary(fakeValue, getDeclaration(), getType(), dimensionCounts);
		}

		public Temporary getLengthTemporary() {
			return dimensionCounts[0];
		}

	}

	public static class ArrayLengthOperator extends Temporary {

		public final Temporary arrayRef;

		public ArrayLengthOperator(AbstractInsnNode decl, Temporary arrayRef){
			super(decl, Type.INT_TYPE);
			this.arrayRef = arrayRef;
			arrayRef.addReference(decl, null);
		}

		@Override
		public int getConstancyInternal() {
			return arrayRef.getConstancyInternal();
		}

		@Override
		public boolean equals(Object o) {
			if(this == o) return true;
			if(!(o instanceof ArrayLengthOperator))	return false;
			ArrayLengthOperator alo = (ArrayLengthOperator) o;
			return alo.arrayRef.equals(arrayRef);
		}

		public String toString(){
			return arrayRef.toString() + ".length";
		}

		@Override
		protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
			arrayRef.addRelevantInstructionsToListSorted(list);
			list.add(getDeclaration());
		}

		@Override
		protected void addCriticalTemporariesToList(ArrayList<Temporary> list) {
			list.add(arrayRef);
			//arrayRef.addCriticalTemporariesToList(list);
		}

		@Override
		protected Temporary clone() {
			return new ArrayLengthOperator(getDeclaration(), arrayRef);
		}

	}

	public static class InstanceofOperatorTemporary extends Temporary {

		public final Temporary objectRef;
		public final Type toCheck;

		public InstanceofOperatorTemporary(AbstractInsnNode decl, Temporary objectRef, Type toCheck){
			super(decl, Type.INT_TYPE);
			this.objectRef = objectRef;
			this.toCheck = toCheck;
			objectRef.addReference(decl, null);
		}

		public int getConstancyInternal() {
			return objectRef.getConstancyInternal();
		};

		@Override
		public boolean equals(Object o) {
			if(this == o) return true;
			if(!(o instanceof InstanceofOperatorTemporary))	return false;
			InstanceofOperatorTemporary iot = (InstanceofOperatorTemporary) o;
			return toCheck.equals(iot.toCheck) && objectRef.equals(iot.objectRef);
		}

		public String toString(){
			return objectRef.toString() + " instanceof " + toCheck.toString();
		}

		@Override
		protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
			objectRef.addRelevantInstructionsToListSorted(list);
			list.add(getDeclaration());
		}

		@Override
		protected void addCriticalTemporariesToList(ArrayList<Temporary> list) {
			list.add(objectRef);
			//objectRef.addCriticalTemporariesToList(list);
		}

		@Override
		protected Temporary clone() {
			return new InstanceofOperatorTemporary(getDeclaration(), objectRef, toCheck);
		}

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

	private Type getCommonSuperType(Temporary[] temps){
		Type t = null;
		for(int i = 0; i < temps.length; i++){
			if(temps[i] instanceof ConstantTemporary && ((ConstantTemporary)temps[i]).value == null){
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

	public class PhiTemporary extends Temporary {

		public final Temporary[] mergedTemporaries;
		public final int index;

		public PhiTemporary(Temporary[] toMerge, int index, Type knownType) {
			super(null, knownType);
			this.mergedTemporaries = toMerge;
			this.index = index;
		}

		public void debugType() {
			Type t = getCommonSuperType(mergedTemporaries);
		}

		public PhiTemporary(Temporary[] toMerge, int index) {
			this(toMerge, index, getCommonSuperType(toMerge));
		}

		@Override
		public int getConstancyInternal() {
			return NOT_CONSTANT;
		}

		@Override
		public boolean equals(Object o) {
			if(o == this) return true;
			if(!(o instanceof PhiTemporary)) return false;
			return index == ((PhiTemporary)o).index && Arrays.equals(mergedTemporaries, ((PhiTemporary)o).mergedTemporaries);
		}

		@Override
		public String toString() {
			return "local_" + index;//"(Local " + index + ": " + getType() + ")";
		}

		@Override
		protected void addRelevantInstructionsToListSorted(ArrayList<AbstractInsnNode> list) {
			list.add(getDeclaration());
			/*for(Temporary t : mergedTemporaries){
				if(t != null){
					t.addRelevantInstructionsToList(list);
				}
			}*/
		}

		@Override
		protected void addCriticalTemporariesToList(ArrayList<Temporary> list) {
			for(Temporary t : mergedTemporaries){
				if(t != null){
					list.add(t);
					//t.addCriticalTemporariesToList(list);
				}
			}
		}

		@Override
		protected Temporary clone() {
			return new PhiTemporary(mergedTemporaries, index, getType());
		}

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

	public static class BasicBlock {
		//index of block within the Method
		public final int index;

		public final ArrayList<BlockEdge> predecessors = new ArrayList<>();
		public final ArrayList<BlockEdge> successors = new ArrayList<>();
		//maximize size of 4, one for each BlockEdge.TYPE (TRUE, FALSE, ALWAYS, NEVER)

		public final Set<BasicBlock> dominates = new HashSet<>();

		public JavaStack stackAtStart;// = new Stack<>();
		public final ArrayList<Temporary> locals = new ArrayList<>();
		//public final ArrayList<Temporary> localsAtStart = new ArrayList<>();
		public final HashMap<AbstractInsnNode, Tuple<Temporary[], Temporary>> operandsAndResultPerInsn = new HashMap<>();
		public final HashMap<Integer, Temporary> localsSetInBlock = new HashMap<>();

		private AbstractInsnNode firstInsnInBlock;
		private AbstractInsnNode lastInsnInBlock;

		public BasicBlock(int index){
			this.index = index;
		}

		public AbstractInsnNode getFirstInsnInBlock(){
			return firstInsnInBlock;
		}

		public AbstractInsnNode getLastInsnInBlock(){
			return lastInsnInBlock;
		}

		public Iterator<AbstractInsnNode> instructionIteratorForward(){
			return new Iterator<AbstractInsnNode>() {
				private AbstractInsnNode cur = firstInsnInBlock;
				@Override
				public AbstractInsnNode next() {
					if(cur == null || cur == lastInsnInBlock.getNext()){
						return null;
					}
					AbstractInsnNode toRet = cur;
					cur = cur.getNext();
					return toRet;
				}

				@Override
				public boolean hasNext() {
					return cur != null && cur != lastInsnInBlock.getNext();
				}
			};
		}

		public Iterator<AbstractInsnNode> instructionIteratorReverse(){
			return new Iterator<AbstractInsnNode>() {
				private AbstractInsnNode cur = lastInsnInBlock;
				@Override
				public AbstractInsnNode next() {
					if(cur == null || cur == firstInsnInBlock.getPrevious()){
						return null;
					}
					AbstractInsnNode toRet = cur;
					cur = cur.getPrevious();
					return toRet;
				}

				@Override
				public boolean hasNext() {
					return cur != null && cur != firstInsnInBlock.getPrevious();
				}
			};
		}

		public String toString(){
			return "B" + String.valueOf(firstInsnInBlock.getIndex()) + "-" + String.valueOf(lastInsnInBlock.getIndex());
		}

	}

	public static class BlockEdge {

		public static enum Type {TRUE, FALSE, ALWAYS, NEVER};
		public static enum Classification {TREE, BACK, FORWARD, CROSS}

		public final Type type;
		public Classification classification;
		public final BasicBlock b1;
		public final BasicBlock b2;

		public BlockEdge(Type type, Classification classification, BasicBlock b1, BasicBlock b2){
			this.type = type;
			this.classification = classification;
			this.b1 = b1;
			this.b2 = b2;
		}

	}

	private static final Level DEFAULT_LOG_LEVEL = Level.FINE;
	private static final String TO_DEBUG = null;// "test/hi/Hello.loopTest()V";

	private static final Logger log = Logger.getLogger("DataManager");
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

	public static class LoopEntry {

		public final LoopEntry parent;
		public final BasicBlock entry;
		public final ArrayList<LoopEntry> children = new ArrayList<>();
		//must be array list to keep the relative order of the children preserved
		//first child that is executed is the first element; last is last
		public final Set<BasicBlock> blocksInLoop = new HashSet<>();
		public final BasicBlock end;
		public final int nthSibling;

		public LoopEntry(BasicBlock entry, BasicBlock endNotExit, LoopEntry prev){
			this.entry = entry;
			this.end = endNotExit;
			//endNotExit.successors.forEach((BE) -> {if(BE.classification == BlockEdge.Classification.BACK)BE.b2=entry.successors.get(0).b2;});

			while(prev != null && !prev.contains(entry)){
				prev = prev.parent;
			}
			this.parent = prev;
			if(parent != null){
				nthSibling = parent.children.size();
				parent.children.add(this);
			}else{
				nthSibling = -1;
			}

			Stack<BasicBlock> toAddStack = new Stack<>();
			toAddStack.add(end);
			BasicBlock toIgnoreAfter = entry.successors.get(0).b2;
			//first block executed within the loop; "top" of loop

			while(!toAddStack.isEmpty()){
				BasicBlock toAdd = toAddStack.pop();
				if(toAdd != toIgnoreAfter){
					blocksInLoop.add(toAdd);
					for(BlockEdge predecessor : toAdd.predecessors){
						if(!blocksInLoop.contains(predecessor.b1)){
							toAddStack.push(predecessor.b1);
						}
					}
				}
			}

		}

		public boolean contains(BasicBlock b){
			return blocksInLoop.contains(b);
		}

		@Override
		public String toString() {
			return "LE_" + entry;
		}

	}



	//TODO collections, math
	//for the purpose of this project, it is sufficient to predetermine whether or not
	//only a limited number of Java library methods are side-effect free
	private static final String[] KNOWN_NO_SIDE_EFFECTS = {
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
		Arrays.sort(KNOWN_NO_SIDE_EFFECTS);
	}

	public class MethodInformation implements Opcodes {
		//not static so we have references to the FieldNodes and ClassNodes

		public final MethodNode mn;
		public final HashMap<AbstractInsnNode, BasicBlock> blocks = new HashMap<>();

		public final HashMap<BasicBlock, Set<BasicBlock>> dominanceMap = new HashMap<>();
		public final HashMap<BasicBlock, BasicBlock> iDominanceMap = new HashMap<>();
		public final HashMap<BasicBlock, Set<BasicBlock>> childrenMap = new HashMap<>();

		public final HashMap<BasicBlock, LoopEntry> loopRoots = new HashMap<>();
		public final HashMap<BasicBlock, LoopEntry> loops = new HashMap<>();

		public final HashMap<AbstractInsnNode, Temporary> temporaries = new HashMap<>();
		public final HashMap<BasicBlock, Set<Temporary>> temporariesRead = new HashMap<>();
		//public final HashMap<BasicBlock, Set<Temporary>> temporariesWritten = new HashMap<>();
		public final HashMap<AbstractInsnNode, FieldTemporary> fieldsWritten = new HashMap<>();

		//public final HashMap<AbstractInsnNode, Tuple<Temporary[], Temporary>> operandsResultPerInsn = new HashMap<>();
		public final HashMap<AbstractInsnNode, Tuple<JavaStack, Temporary[]>> statePerInsn = new HashMap<>();

		public MethodInformation(MethodNode mn){
			this.mn = mn;
			recompute();
		}

		private boolean callingHasSideEffects;
		public boolean hasSideEffects() {
			if(fieldsWritten.size() > 0){
				return true;
			}
			Stack<MethodCallGraphNode> searching = new Stack<>();
			searching.add(DataManager.this.methodCallGraph.get(mn));
			while(!searching.isEmpty()){
				MethodCallGraphNode mcgn = searching.pop();
				if(mcgn.mn != null){
					MethodInformation mi = DataManager.this.methodInformations.get(mcgn.mn);
					if(mi.callingHasSideEffects){
						mi.callingHasSideEffects = false;
						return true; //avoid infinite recursion...assume side effects
					}else{
						mi.callingHasSideEffects = true;
						boolean miHasSideEffects = mi.hasSideEffects();
						mi.callingHasSideEffects = false;
						if(miHasSideEffects){
							return true;
						}
					}
				}else{
					final ForeignExecutableCallGraphNode fecgn = (ForeignExecutableCallGraphNode) mcgn;
					final Type type = fecgn.e instanceof Method ? Type.getType((Method)fecgn.e) : Type.getType((Constructor<?>)fecgn.e);
					final String s = fecgn.e.getDeclaringClass().getName().replace('.', '/') + "." + fecgn.e.getName() + type.getDescriptor();
					if(Arrays.binarySearch(KNOWN_NO_SIDE_EFFECTS, s) < 0){
						return true;
					}
				}
			}
			return false;
		}

		private void printGraph(){
			//mn.instructions.get(0);
			Stack<BasicBlock> stack = new Stack<>();
			HashSet<BasicBlock> searched = new HashSet<>();
			stack.add(getFirstBlock());
			if(log.getLevel().intValue() <= Level.FINEST.intValue()){
				while(!stack.isEmpty()){
					BasicBlock block = stack.pop();
					log.finest(block.toString());

					for(BlockEdge successor : block.successors){
						log.finest("\t" + successor.b2);
						if(searched.add(successor.b2)){
							stack.push(successor.b2);
						}
					}

				}
			}
		}
		public void recompute(){

			if(Modifier.isAbstract(mn.access) || Modifier.isNative(mn.access))
				return;

			boolean isOfInterest = (methodNodeToOwnerMap.get(mn).name + "." + mn.name + mn.desc).equals(TO_DEBUG);
			if(isOfInterest){
				System.out.println("walk");
			}
			log.setLevel(isOfInterest ? Level.FINEST : DEFAULT_LOG_LEVEL);

			log.finer("Computing " + methodNodeToOwnerMap.get(mn).name + "." + mn.name + mn.desc);

			blocks.clear();

			loops.clear();

			dominanceMap.clear();
			iDominanceMap.clear();
			childrenMap.clear();

			//HashMap<string>

			temporaries.clear();
			temporariesRead.clear();
			fieldsWritten.clear();

			mn.instructions.get(0);
			computeGraph();
			mn.instructions.get(0);
			//computeFieldReadWriteInfo();

			labelEdgesDepthFirst();

			//omputeiDominanceNew();
			//computeDominance();
			//computeiDominance();
			computeChildren();


			if(isOfInterest){
				printGraph();
				System.out.println("Dominance: " + dominanceMap);
				System.out.println("Immediate dominance: " + iDominanceMap);
				System.out.println("Children: " + childrenMap);
			}

			doSymbolicExecution();

			computeLoopGraph();

			mn.instructions.get(0);//TODO remove me
			//We do this after computeGraph because that function inserts
			//a LabelNode at the begining of mn.instructions
			//and because computeLoopGraph inserts other labels

			if(log.getLevel().intValue() <= Level.FINEST.intValue()){
				for(LoopEntry loop : loops.values()){
					log.finest("\tLoop entry: " + loop.entry);
					log.finest("\t\tParent loop: " + (loop.parent == null ? null : loop.parent.entry));
					StringBuilder sb = new StringBuilder();
					for(LoopEntry child : loop.children){
						sb.append(", ").append(child.entry);
					}
					if(sb.length() > 0){
						log.finest("\t\tChildren: " + sb.substring(2));
					}
					sb = new StringBuilder();
					for(BasicBlock b : blocks.values()){
						if(loop.contains(b)){
							sb.append(", " + b);
						}
					}
					if(sb.length() > 0){
						log.finest("\t\tContains: " + sb.substring(2));
					}
				}
			}
			//log.finest("\tLoop graph: " + loops);

			if(log.getLevel().intValue() <= Level.FINEST.intValue()){
				log.finest(blocks.toString());
				if(log.getLevel().intValue() <= Level.FINEST.intValue()){
					for(BasicBlock b : blocks.values()){
						log.finest("\t" + b);
						for(BlockEdge edge : b.successors){
							log.finest("\t\t" + edge.type + " (" + edge.classification + ") " + edge.b2);
						}
					}
					log.finest("\tChildren map: " + childrenMap);
				}
			}

			//doSymbolicExecution();

			log.finer("\n\n");
		}

		private BasicBlock getBlockFromInstruction(AbstractInsnNode ain){
			BasicBlock block = blocks.get(ain);
			if(block == null){
				block = new BasicBlock(blocks.size());
				block.firstInsnInBlock = ain;
				block.lastInsnInBlock = ain;
				blocks.put(ain, block);
			}
			return block;
		}

		private void link(AbstractInsnNode onInstruction, AbstractInsnNode b1Delimeter, AbstractInsnNode b2Delimeter, BlockEdge.Type type){
			//assumed that b1 is known, as it is the curBlock local variable from the computeGraph()
			BasicBlock b1 = getBlockFromInstruction(b1Delimeter);
			BasicBlock b2 = getBlockFromInstruction(b2Delimeter);

			BlockEdge edge = new BlockEdge(type, null, b1, b2);
			if(b1.successors.stream().anyMatch((b)->b.type == type)){
				mn.instructions.get(0);//build cache for meaningful output
				System.out.println("Error: cannot add edge twice: " + b1.firstInsnInBlock.getIndex() + " " + type + " " + b2.firstInsnInBlock.getIndex());

				return;
			}
			b1.successors.add(edge);
			b1.lastInsnInBlock = onInstruction;
			b2.predecessors.add(edge);
		}

		public BasicBlock getFirstBlock(){
			return blocks.get(this.mn.instructions.getFirst());
		}

		public BasicBlock getLastBlock(){
			return blocks.get(this.mn.instructions.getLast());
		}

		private void computeGraph(){			
			//if(!(mn.instructions.getFirst() instanceof LabelNode))
			//we want our own completely unique entry block for the purposes of loops
			mn.instructions.insert(new LabelNode(new Label()));
			if(!(mn.instructions.getLast() instanceof LabelNode))
				mn.instructions.add(new LabelNode(new Label()));
			mn.instructions.get(0);
			

			AbstractInsnNode curBlockDelimeter = null;
			//leave the very first LabelNode to be its own unique entrance block
			for(AbstractInsnNode ain = mn.instructions.getFirst(); ain != null; ain = ain.getNext()){
				int index = ain.getIndex();
				if(curBlockDelimeter == null){
					curBlockDelimeter = ain;
				}else{
					if(ain.getType() == AbstractInsnNode.LABEL && curBlockDelimeter != ain){
						link(ain.getPrevious(), curBlockDelimeter, ain, BlockEdge.Type.ALWAYS);
						curBlockDelimeter = ain;
					}
				}
				if(ain.getOpcode() == Opcodes.GOTO){
					link(ain, curBlockDelimeter, ((JumpInsnNode)ain).label, BlockEdge.Type.ALWAYS);
					curBlockDelimeter = null;
				}else if(ain.getType() == AbstractInsnNode.METHOD_INSN){
					link(ain, curBlockDelimeter, ain.getNext(), BlockEdge.Type.ALWAYS);
					curBlockDelimeter = ain.getNext();
				}else if(ain.getType() == AbstractInsnNode.JUMP_INSN){
					//jump insn but not a direct GOTO
					link(ain, curBlockDelimeter, ((JumpInsnNode)ain).label, BlockEdge.Type.TRUE);
					link(ain, curBlockDelimeter, ain.getNext(), BlockEdge.Type.FALSE);
					curBlockDelimeter = null;
				}else if((ain.getOpcode() >= Opcodes.IRETURN && ain.getOpcode() <= Opcodes.RETURN) ||
						ain.getOpcode() == Opcodes.ATHROW){
					link(ain, curBlockDelimeter, mn.instructions.getLast(), BlockEdge.Type.NEVER);
					curBlockDelimeter = null;
				}
			}
			if(getFirstBlock() == null){
				System.err.println("What in the fucking hell");
			}
			//link(mn.instructions.getFirst(), mn.instructions.getFirst(), mn.instructions.getFirst().getNext(), BlockEdge.Type.ALWAYS);
		}

		private void printBlock(BasicBlock b){
			System.out.println(b);
			Textifier t = new Textifier();
			mn.accept(new TraceMethodVisitor(t));
			mn.instructions.get(0);
			Iterator<AbstractInsnNode> iter = b.instructionIteratorForward();
			while(iter.hasNext()){
				int op = iter.next().getOpcode();
				if(op != -1)
					System.out.println('\t' + Textifier.OPCODES[op]);//t.text.get(iter.next().getIndex()));
			}
		}

		private void labelEdgesDepthFirst(){
			
			pre = new int[blocks.size()];
			post = new int[blocks.size()];
			precount = 0;
			postcount = blocks.size() + 1;

			iDominanceMap.put(getFirstBlock(), null);

			dfs(getFirstBlock());

			/*if(iDominanceMap.size() + 1 != blocks.size()){
			  Java compiler VERY often generate unreacahble bytecode? Especially the eclipse compiler

				mn.instructions.get(0);
				System.err.println("Warning: unreachable blocks:");
				blocks.values().stream().filter((b) -> !iDominanceMap.containsKey(b) && b != getFirstBlock() && b != getLastBlock()).forEach(MethodInformation.this::printBlock);
				System.out.println();
			}*/

		}

		private int[] pre;
		private int[] post;
		private int precount;
		private int postcount;

		private BasicBlock ancestor(BasicBlock b1, BasicBlock b2){
			BasicBlock initialB1 = b1;
			BasicBlock initialB2 = b2;
			while(b1 != null && b1 != b2){
				b2 = initialB2;
				while(b2 != null && b1 != b2){
					b2 = iDominanceMap.get(b2);
				}
				if(b1 == b2) break;
				b1 = iDominanceMap.get(b1);
			}
			return b1; //b1 == b2
		}

		private void dfs(BasicBlock b){
			if(b == null){
				//occurs with methods with a single instruction method
				return;
			}
			pre[b.index] = precount++;
			for(BlockEdge e : b.successors){
				BasicBlock s = e.b2;
				if(pre[s.index] == 0){
					e.classification = BlockEdge.Classification.TREE;
					iDominanceMap.put(s, b);
					dfs(s);
				}else if(post[s.index] == 0){
					e.classification = BlockEdge.Classification.BACK;
					//ignore iDominance; no effect
				}else if(pre[b.index] < pre[s.index]){
					e.classification = BlockEdge.Classification.FORWARD;
					iDominanceMap.put(s, ancestor(iDominanceMap.get(s), iDominanceMap.get(b)));
				}else{
					e.classification = BlockEdge.Classification.CROSS;
					iDominanceMap.put(s, ancestor(iDominanceMap.get(s), iDominanceMap.get(b)));
				}
			}
			post[b.index] = postcount--;
		}

		//TODO TOO SLOW!!! This slows down my program 10-100x
		/*private final HashMap<BasicBlock, BasicBlock> loop_entry = new HashMap<>();
		private final HashMap<BasicBlock, Set<BasicBlock>> loop_contains = new HashMap<>();
		private BasicBlock loop_root;
		private final HashMap<BasicBlock, BasicBlock> loop_parent = new HashMap<>();

		private void computeLoopGraph(){
			for(BasicBlock B : blocks.values()){
				loop_entry.put(B, B);
				loop_contains.put(B, new HashSet<>());
				loop_contains.get(B).add(B);
			}
			Stack<BasicBlock> DFSStack = new Stack<>();
			DFSStack.push(getFirstBlock());
			while(!DFSStack.isEmpty()){
				BasicBlock B = DFSStack.pop();
				findLoop(B);
				BasicBlock nextDFS = null;
				for(BlockEdge successor : B.successors){
					if(successor.classification != BlockEdge.Classification.TREE){
						DFSStack.push(successor.b2);
					}else{
						nextDFS = successor.b2;
					}
				}
				if(nextDFS != null){
					DFSStack.push(nextDFS);
				}
			}
			Set<BasicBlock> bset = new HashSet<>();
			bset.add(getLastBlock());
			loop_root = findLoopBody(bset, getFirstBlock());
		}

		private void findLoop(BasicBlock B){
			Set<BasicBlock> loop = new HashSet<>();
			for(BlockEdge E : B.predecessors){
				if(E.classification == BlockEdge.Classification.BACK){
					BasicBlock P = E.b1;
					if(P != B){
						loop.add(P);
					}
				}
			}
			findLoopBody(loop, B);
		}

		private BasicBlock findLoopBody(Set<BasicBlock> gen, BasicBlock head){
			Set<BasicBlock> loop = new HashSet<>();
			LinkedList<BasicBlock> queue = new LinkedList<>();
			for(BasicBlock B : gen){
				BasicBlock L = loopAncestor(B);
				if(loop.add(L)){
					queue.add(L);
				}
			}

			while(!queue.isEmpty()){
				BasicBlock B = queue.removeFirst();
				for(BlockEdge predecessor : B.predecessors){
					BasicBlock P = predecessor.b1;
					if(P != head){
						BasicBlock L = loopAncestor(P);
						if(loop.add(L)){
							queue.add(L);
						}
					}
				}
			}

			loop.add(head);
			BasicBlock X = new BasicBlock(blocks.size());

			loop_contains.put(X, loop);
			loop_entry.put(X, head);
			loop_parent.put(X, null);
			for(BasicBlock B : loop){
				loop_parent.put(B, X);
			}

			return X;
		}

		private BasicBlock loopAncestor(BasicBlock B){
			while(loop_parent.get(B) != null){
				B = loop_parent.get(B);
			}
			return B;
		}*/

		private LoopEntry recursiveLoopSearch(LoopEntry entry, BasicBlock b){
			//entry.contains(b) == true
			for(LoopEntry child : entry.children){
				if(child.contains(b)){
					return recursiveLoopSearch(child, b);
				}
			}
			return entry;
		}

		public LoopEntry getInnermostLoopContaining(BasicBlock b){
			for(LoopEntry root : loopRoots.values()){
				if(root.contains(b)){
					return recursiveLoopSearch(root, b);
				}
			}
			return null;
		}

		private void computeLoopGraph(){
			LoopEntry prev = null;
			Set<BasicBlock> searched = new HashSet<>();
			Stack<BasicBlock> toSearch = new Stack<>();
			toSearch.add(getFirstBlock());
			while(!toSearch.isEmpty()){
				BasicBlock b = toSearch.pop();
				for(int i = 0; i < b.predecessors.size(); i++){
					BlockEdge edge = b.predecessors.get(i);
					if(edge.classification == BlockEdge.Classification.BACK){
						BasicBlock uniqueEntry = null;
						for(int j = 0; j < b.predecessors.size(); j++){
							if(j == i) continue;
							if(b.predecessors.get(j).classification == BlockEdge.Classification.TREE){
								uniqueEntry = b.predecessors.get(j).b1;
								//java compiler always compiles so that each loop has a unique entrance,
								//except for the special case when the loop is the first block,
								//handled below
								break;
							}
						}
						if(uniqueEntry == null){
							//first block is part of the loop
							//create a uniqueEntry just before it
							if(b.getFirstInsnInBlock() != mn.instructions.getFirst()){
								throw new RuntimeException("Cannot determine unique entry for " + b);
							}
							LabelNode ln = new LabelNode(new Label());
							mn.instructions.insert(ln);
							link(ln, ln, b.getFirstInsnInBlock(), BlockEdge.Type.ALWAYS);
							uniqueEntry = blocks.get(ln);
						}
						LoopEntry loop = new LoopEntry(uniqueEntry,	edge.b1, prev);
						if(loop.parent == null){
							loopRoots.put(uniqueEntry, loop);
						}

						loops.put(uniqueEntry, loop);
						prev = loop;
						break;
					}
				}

				for(BlockEdge successor : b.successors){
					if(searched.add(successor.b2)){
						toSearch.push(successor.b2);
					}
				}

			}

		}

		//		for all nodes, b /* initialize the dominators array */ doms[b] ← Undefined
		//		doms[start node] ← start node Changed ← true
		//		while (Changed)
		//		Changed ← false
		//		for all nodes, b, in reverse postorder (except start node)
		//		new idom ← first (processed) predecessor of b /* (pick one) */ for all other predecessors, p, of b
		//		if doms[p] ̸= Undefined /* i.e., if doms[p] already calculated */ new idom ← intersect(p, new idom)
		//		if doms[b] ̸= new idom doms[b] ← new idom Changed ← true
		//		function intersect(b1, b2) returns node finger1 ← b1
		//		finger2 ← b2
		//		while (finger1 ̸= finger2)
		//		while (finger1 < finger2) finger1 = doms[finger1]
		//		while (finger2 < finger1) finger2 = doms[finger2]
		//		return finger1

		/*private void computeiDominanceNew(){
			iDominanceMap.clear();

			boolean changed = true;


			while(changed){
				changed = false;

				Set<BasicBlock> processed = new HashSet<>();
				Stack<BasicBlock> stack = new Stack<>();
				getFirstBlock().successors.forEach((be) -> stack.push(be.b2));

				while(!stack.isEmpty()){
					BasicBlock b = stack.pop();
					processed.add(b);
					BasicBlock new_idom = null;
					for(BlockEdge predecessor : b.predecessors){
						if(new_idom == null){
							new_idom = predecessor.b1;
						}else{
							BasicBlock intersection = intersect(predecessor.b1, new_idom);
							//idom = firstblock if intersection is null
							new_idom = intersection == null ? getFirstBlock() : intersection;
						}
					}
					if(iDominanceMap.get(b) != new_idom){
						iDominanceMap.put(b, new_idom);
						changed = true;
					}

					for(BlockEdge successor : b.successors){
						if(processed.add(successor.b2)){
							stack.push(successor.b2);
						}
					}
				}
			}
		}

		private BasicBlock intersect(BasicBlock b1, BasicBlock b2){
			final BasicBlock initialB2 = b2;
			while(b1 != b2){
				b2 = initialB2;
				while(b1 != b2 && b2 != null){
					b2 = iDominanceMap.get(b2);
				}
				if(b2 != null){ //b1 == b2
					break;
				}
				b1 = iDominanceMap.get(b1);
			}
			return b1;
		}*/

		private void computeDominanceLegacy2(){
			try{
				Stack<BasicBlock> worklist = new Stack<>();
				worklist.add(getFirstBlock());

				Set<BasicBlock> tempSet = new HashSet<>();
				working:
					while(!worklist.isEmpty()){
						BasicBlock workingWith = worklist.pop();
						if(workingWith.toString().equals("B-49")){
							System.out.println("walk with me");
						}
						Set<BasicBlock> blocksDominatingCurrent = dominanceMap.get(workingWith);
						if(blocksDominatingCurrent == null){
							blocksDominatingCurrent = new HashSet<>();
							blocksDominatingCurrent.add(workingWith);
							dominanceMap.put(workingWith, blocksDominatingCurrent);
						}

						tempSet.clear();
						for(BlockEdge edgeToThis : workingWith.predecessors){
							if(edgeToThis.classification == BlockEdge.Classification.BACK)
								continue;
							BasicBlock predecessor = edgeToThis.b1;
							if(dominanceMap.get(predecessor) == null){
								worklist.push(workingWith);
								worklist.push(predecessor);
								continue working;
							}
							if(tempSet.isEmpty()){
								tempSet.addAll(dominanceMap.get(predecessor));
							}else{
								Set<BasicBlock> predecessorSet = dominanceMap.get(predecessor);
								tempSet.removeIf((block) -> !predecessorSet.contains(block));
							}
						}
						blocksDominatingCurrent.addAll(tempSet);
						for(BlockEdge successor : workingWith.successors){
							if(successor.b2.toString().equals("B-30")){
								System.err.println("walk with me 2");
							}
							if(dominanceMap.get(successor.b2) == null){
								worklist.push(successor.b2);
							}
						}
					}

			}catch(Throwable th){
				Textifier t = new Textifier();
				mn.accept(new TraceMethodVisitor(t));
				String[] insnTexts = t.text.toArray(new String[t.text.size()]);
				for(int i = 0; i < insnTexts.length; i++){
					System.out.print(i + ": " + insnTexts[i]);
				}
				for(AbstractInsnNode key : blocks.keySet()){
					BasicBlock b = blocks.get(key);
					System.out.println(b.toString());
					for(BlockEdge be : b.successors){
						System.out.println("\t" + be.b2.toString() + "(" + be.classification + ")");
					}
				}
				System.out.println(dominanceMap);
				System.out.println(iDominanceMap);
				System.out.println(childrenMap);
				throw new RuntimeException(th);
			}
		}

		@SuppressWarnings("unused")
		private void computeDominanceLegacy(){
			BasicBlock R = getFirstBlock();
			//Set<BasicBlock> dset = new HashSet<>(); //???
			Set<BasicBlock> tset = new HashSet<>();
			Collection<BasicBlock> all = blocks.values();

			dominanceMap.put(R, new HashSet<>());
			dominanceMap.get(R).add(R);

			for(BasicBlock b : all){
				if(b != R){
					dominanceMap.put(b, new HashSet<>(all));
				}
			}

			boolean change = true;
			while(change){
				change = false;
				for(BasicBlock N : all){
					if(N != R){
						tset.clear();

						for(BlockEdge predecessor : N.predecessors){
							BasicBlock P = predecessor.b1;
							tset.addAll(dominanceMap.get(P));
						}

						tset.add(N);

						if(!tset.equals(dominanceMap.get(N))){
							//Set.equals(Set) --> desired behavior
							change = true;
							dominanceMap.put(N, tset);
						}
					}
				}
			}
		}

		private void computeiDominanceLegacy(){
			try{
				BasicBlock R = getFirstBlock();

				HashMap<BasicBlock, Set<BasicBlock>> idom = new HashMap<>();

				for(BasicBlock N : blocks.values()){
					if(dominanceMap.get(N) == null){
						continue;
					}
					Set<BasicBlock> domin = new HashSet<>(dominanceMap.get(N));
					domin.remove(N);
					idom.put(N, domin);
				}

				for(BasicBlock N : blocks.values()){
					Set<BasicBlock> bset = new HashSet<>();
					if(N != R){
						Set<BasicBlock> idomN = idom.get(N);
						if(idomN == null) continue;
						for(BasicBlock S : idom.get(N)){
							for(BasicBlock T : idom.get(S)){
								if(T != S && idom.get(S).contains(T)){
									bset.add(T);
								}
							}
						}
					}
					for(BasicBlock toRemove : bset){
						idom.get(N).remove(toRemove);
					}
				}

				for(BasicBlock N : blocks.values()){
					if(N != R){
						Set<BasicBlock> set = idom.get(N);
						if(set == null || set.size() == 0){
							System.err.println("Error calculating immediate dominator for " + N);
							continue;
						}
						iDominanceMap.put(N, (BasicBlock) set.toArray()[0]);
					}
				}

			}catch(Throwable th){
				Textifier t = new Textifier();
				mn.accept(new TraceMethodVisitor(t));
				String[] insnTexts = t.text.toArray(new String[t.text.size()]);
				for(int i = 0; i < insnTexts.length; i++){
					System.out.print(i + ": " + insnTexts[i]);
				}
				for(AbstractInsnNode key : blocks.keySet()){
					BasicBlock b = blocks.get(key);
					System.out.println(b.toString());
					for(BlockEdge be : b.successors){
						System.out.println("\t" + be.b2.toString() + "(" + be.classification + ")");
					}
				}
				System.out.println(dominanceMap);
				System.out.println(iDominanceMap);
				System.out.println(childrenMap);
				throw new RuntimeException(th);
			}

		}

		private void computeChildren(){
			for(BasicBlock B : blocks.values()){
				BasicBlock C = iDominanceMap.get(B);
				if(C != null){


					Set<BasicBlock> children = childrenMap.get(C);
					if(children == null){
						children = new HashSet<>();
						childrenMap.put(C, children);
					}
					children.add(B);
				}
			}
		}

		private void addToTemporariesRead(BasicBlock block, Temporary tmp){
			Set<Temporary> read = temporariesRead.get(block);
			if(read == null){
				read = new HashSet<>();
				temporariesRead.put(block, read);
			}
			read.add(tmp);
		}

		/*@Deprecated
		private void addToTemporariesWritten(BasicBlock block, Temporary tmp){
			temporaries.add(tmp);
			Set<Temporary> written = temporariesWritten.get(block);
			if(written == null){
				written = new HashSet<>();
				temporariesWritten.put(block, written);
			}
			written.add(tmp);
			/*if(tmp.constancy == Temporary.CONSTANCY_UNKNOWN){
				tmp.constancy = Temporary.CONSTANT; //written to once so far; "constant"
				System.err.println("SETTING CONSTANT: " + tmp);
			}else if(tmp.constancy == Temporary.CONSTANT){
				tmp.constancy = Temporary.NOT_CONSTANT;
				//TODO FIXME I don't know if this works!
			}*/
		//}

		private JavaStack mergeStacksAndClone(JavaStack prev, JavaStack toMerge){
			if(prev == null || prev.size() == 0) return toMerge.clone();
			if(toMerge == null || toMerge.size() == 0) return prev.clone();
			if(prev.size() != toMerge.size()){
				JavaStack toRet = toMerge.clone();
				System.err.println("Warning: cannot merge stacks with different sizes:\n\t" + prev + "\n\t" + toMerge + "\n");
				for(int i = 0; i < toRet.size(); i++){
					toRet.elementAt(i).forceConstancy(Temporary.NOT_CONSTANT);
				}
				return toMerge;
			}
			JavaStack toRet = toMerge.clone();
			for(int i = 0; i < prev.size(); i++){
				if(toRet.elementAt(i) != prev.elementAt(i)){
					//shallow equals is sufficient for virtually all merges
					if(!toRet.elementAt(i).equals(prev.elementAt(i))){
						//now they are actually not equal
						toRet.set(i, new PhiTemporary(new Temporary[]{toRet.elementAt(i), prev.elementAt(i)}, -1), null);
					}
				}
			}
			return toRet;
		}

		private FieldTemporary getFieldTemporary(AbstractInsnNode instruction, MethodNode mn, Temporary objectRef, String owner, String name, String desc, Temporary toStore){
			FieldTemporary instance = null;
			synchronized(Temporary.GLOBAL_TEMPORARIES){
				for(Temporary tmp : Temporary.GLOBAL_TEMPORARIES){
					if(tmp instanceof FieldTemporary){
						FieldTemporary ft = (FieldTemporary) tmp;
						//essentially copy ft.equals(o) except without creating a new object
						if(owner.equals(ft.owner) && name.equals(ft.name)){
							Temporary otherObjectRef = ft.getObjectRef();
							if(objectRef == otherObjectRef || (objectRef != null && otherObjectRef != null && objectRef.equals(otherObjectRef))){
								instance = ft;
								break;
							}
						}
					}
				}
			}
			if(instance == null){
				final Type type = Type.getType(desc);
				boolean isConstant = false;
				boolean isVolatile = true;
				ClassHierarchyTreeNode chtn = defineClassHierarchyTreeNode(owner);
				if(chtn == null){
					instance = new FieldTemporary(isConstant, objectRef, owner, name, type, isVolatile);
					return instance.cloneSpecialCase(instruction, mn, toStore);
				}
				outer: do {
					ClassNode cn = getClassNode(chtn.name);
					if(cn != null){
						FieldNode fn = cn.getFieldNode(name);
						if(fn != null){
							isConstant = Modifier.isFinal(fn.access);
							isVolatile = Modifier.isVolatile(fn.access);
							break outer;
						}
					}else{
						try {
							Class<?> clazz = Class.forName(owner.replace('/', '.'));
							for(Field f : clazz.getDeclaredFields()){
								if(f.getName().equals(name)){
									isConstant = Modifier.isFinal(f.getModifiers());
									isVolatile = Modifier.isVolatile(f.getModifiers());
									break outer;
								}
							}
						} catch (ClassNotFoundException e) {
							instance = new FieldTemporary(false, objectRef, owner, name, type, true);
							//unknown field

							//throw new RuntimeException(e);
						}
					}
				} while((chtn = chtn.superNode) != null);
				instance = new FieldTemporary(isConstant, objectRef, owner, name, type, isVolatile);
			}
			return instance.cloneSpecialCase(instruction, mn, toStore);
		}

		private void doSymbolicExecution(){
			if(Modifier.isAbstract(mn.access) || Modifier.isNative(mn.access)) return;
			log.finest("Symbolically executing");


			LinkedList<BasicBlock> toExecute = new LinkedList<>();
			Set<BasicBlock> executed = new HashSet<>();
			toExecute.add(getFirstBlock());

			//Stack<Temporary> temporariesStack = new Stack<>();
			JavaStack stack = new JavaStack();
			ArrayList<Temporary> locals = new ArrayList<>();
			Type[] types = Type.getArgumentTypes(mn.desc);
			if(!Modifier.isStatic(mn.access)){
				final ClassNode owner = methodNodeToOwnerMap.get(mn);
				locals.add(new ParameterTemporary(mn, -1, Type.getType('L' + owner.name + ';')));
			}
			for(int i = 0; i < types.length; i++){
				locals.add(new ParameterTemporary(mn, i, types[i]));
				if(types[i] == Type.DOUBLE_TYPE || types[i] == Type.LONG_TYPE){
					locals.add(null);//takes up 2 words
				}
			}

			int maxStack = 0;

			for(int i = locals.size(); i < mn.maxLocals; i++){
				//fill rest of locals array with nulls so when we set we don't get an exception
				locals.add(null);
			}

			for(int i = 0; i < locals.size(); i++){
				
				
				getFirstBlock().locals.add(locals.get(i));
				getFirstBlock().localsSetInBlock.put(i, locals.get(i));
			}

			for(BasicBlock block : blocks.values()){
				if(block == getFirstBlock()) continue;
				block.locals.clear();
				block.locals.addAll(locals);
			}

			log.finest("Locals before first instruction: " + locals);




			Textifier t = new Textifier();
			mn.accept(new TraceMethodVisitor(t));
			Object[] instructionsInText = t.text.toArray(new Object[t.text.size()]);

			while(!toExecute.isEmpty()){
				BasicBlock executingBlock = toExecute.removeFirst();
				for(BlockEdge pred : executingBlock.predecessors){
					if(pred.classification != BlockEdge.Classification.BACK && !executed.contains(pred.b1)){
						System.err.println("Warning: did not execute "  + pred.b1 + " before " + executingBlock);
						break;
					}
				}

				executed.add(executingBlock);

				if(executingBlock.stackAtStart != null){
					stack = executingBlock.stackAtStart;

				}

				BasicBlock parent = iDominanceMap.get(executingBlock);
				locals = executingBlock.locals;
				if(parent != null){
					//locals = parent.locals;
					//locals.clear();
					//locals.addAll(iDominanceMap.get(executingBlock).locals);
					//locals = idom(cur).locals

					//locals.addAll(executingBlock.locals);
				}

				if(executingBlock.toString().startsWith("B575")){
					System.out.println("Bp");
				}
				/*int numEdgesToThis = executingBlock.predecessors.size();
				if(numEdgesToThis > 1){
					for(int i = 0; i < locals.size(); i++){
						Temporary[] toMerge = new Temporary[numEdgesToThis];
						boolean shouldActuallyMerge = false;
						for(int j = 0; j < numEdgesToThis; j++){
							toMerge[j] = executingBlock.predecessors.get(j).b1.localsSetInBlock.get(i);
							if(j == 0) continue;
							if(!shouldActuallyMerge){
								if(toMerge[0] == toMerge[j]) continue;
								if(toMerge[0] == null || toMerge[j] == null){
									shouldActuallyMerge = true;
									continue;
								}else{
									shouldActuallyMerge = !toMerge[0].equals(toMerge[j]);
								}
							}
						}

						if(shouldActuallyMerge){
							locals.set(i, new PhiTemporary(toMerge, i, executingBlock));
						}else if(toMerge[0] != null){
							//when !shouldActuallyMergge,
							//toMerge[0] == null iff the local was not set in any predecessor
							//all of toMerge is null
							locals.set(i, toMerge[0]);
						}

					}
				}
				//TODO merge locals from predecessors*/


				log.finest("Executing block " + executingBlock);
				log.finest("Temporaries stack at start: " + executingBlock.stackAtStart);
				log.finest("Locals at start: " + locals);

				//System.out.println("Picking it up at Block " + executingBlock.index + " at Instruction " + executingBlock.firstInsnInBlock.getIndex() +
				//	"\n\t" + t.text.get(executingBlock.firstInsnInBlock.getIndex())  + "\tStack: " + executingBlock.stackAtStart + "\n\tLocals: " + locals + "\n");
				//execute
				final Iterator<AbstractInsnNode> insnsOfBlock = executingBlock.instructionIteratorForward();
				AbstractInsnNode executingInstruction = null;

				try{
					while(insnsOfBlock.hasNext()){
						executingInstruction = insnsOfBlock.next();

						log.finest(instructionsInText[executingInstruction.getIndex()].toString());
						if(executingInstruction.getType() == AbstractInsnNode.FRAME ||
								executingInstruction.getType() == AbstractInsnNode.LABEL ||
								executingInstruction.getType() == AbstractInsnNode.LINE){
							continue;
						}

						//System.out.print(t.text.get(executingInstruction.getIndex()));

						Temporary[] popped = null;
						Temporary toPush = null;
						switch(executingInstruction.getOpcode()){
						case NOP:
							break;
						case ACONST_NULL:
							toPush = new ConstantTemporary(executingInstruction, null, Type.getType(Object.class));
							break;
						case ICONST_M1:
							toPush = new ConstantTemporary(executingInstruction, -1, Type.INT_TYPE);
							break;
						case ICONST_0:
							toPush = new ConstantTemporary(executingInstruction, 0, Type.INT_TYPE);
							break;
						case ICONST_1:
							toPush = new ConstantTemporary(executingInstruction, 1, Type.INT_TYPE);
							break;
						case ICONST_2:
							toPush = new ConstantTemporary(executingInstruction, 2, Type.INT_TYPE);
							break;
						case ICONST_3:
							toPush = new ConstantTemporary(executingInstruction, 3, Type.INT_TYPE);
							break;
						case ICONST_4:
							toPush = new ConstantTemporary(executingInstruction, 4, Type.INT_TYPE);
							break;
						case ICONST_5:
							toPush = new ConstantTemporary(executingInstruction, 5, Type.INT_TYPE);
							break;
						case LCONST_0:
							toPush = new ConstantTemporary(executingInstruction, 0L, Type.LONG_TYPE);
							break;
						case LCONST_1:
							toPush = new ConstantTemporary(executingInstruction, 1L, Type.LONG_TYPE);
							break;
						case FCONST_0:
							toPush = new ConstantTemporary(executingInstruction, 0F, Type.FLOAT_TYPE);
							break;
						case FCONST_1:
							toPush = new ConstantTemporary(executingInstruction, 1F, Type.FLOAT_TYPE);
							break;
						case FCONST_2:
							toPush = new ConstantTemporary(executingInstruction, 2F, Type.FLOAT_TYPE);
							break;
						case DCONST_0:
							toPush = new ConstantTemporary(executingInstruction, 0D, Type.DOUBLE_TYPE);
							break;
						case DCONST_1:
							toPush = new ConstantTemporary(executingInstruction, 1D, Type.DOUBLE_TYPE);
							break;
						case BIPUSH:
							toPush = new ConstantTemporary(executingInstruction, (byte) ((IntInsnNode)executingInstruction).operand, Type.BYTE_TYPE);
							break;
						case SIPUSH:
							toPush = new ConstantTemporary(executingInstruction, (short) ((IntInsnNode)executingInstruction).operand, Type.SHORT_TYPE);
							break;
						case LDC:
							LdcInsnNode lin = (LdcInsnNode) executingInstruction;
							Type type;
							if(lin.cst instanceof Type){
								type = (Type) lin.cst;
							}else if(lin.cst instanceof Integer){
								type = Type.INT_TYPE;
							}else if(lin.cst instanceof Double){
								type = Type.DOUBLE_TYPE;
							}else if(lin.cst instanceof Long){
								type = Type.LONG_TYPE;
							}else if(lin.cst instanceof Float){
								type = Type.FLOAT_TYPE;
							}else if(lin.cst instanceof String){
								type = Type.getType(String.class);
							}else{
								throw new RuntimeException("Unknown type of constant: " + lin.cst.getClass().getSimpleName());
							}

							toPush = new ConstantTemporary(executingInstruction, lin.cst, type);
							break;
						case ILOAD:
						case LLOAD:
						case FLOAD:
						case DLOAD:
						case ALOAD:
							VarInsnNode vvi = (VarInsnNode) executingInstruction;
							toPush = locals.get(vvi.var);
							if(toPush == null){
								throw new RuntimeException("Undefined local variable " + vvi.var + "\n\t" + childrenMap);
							}
							toPush = toPush.cloneOnInstruction(executingInstruction);
							toPush.addReference(executingInstruction, mn);
							addToTemporariesRead(executingBlock, toPush);

							//yes, after the above code
							//toPush = new LocalVariableReference(executingInstruction, toPush);
							break;
						case IALOAD:
						case LALOAD:
						case FALOAD:
						case DALOAD:
						case AALOAD:
						case BALOAD:
						case CALOAD:
						case SALOAD:
							popped = new Temporary[2];
							Temporary index = popped[0] = stack.pop();
							Temporary arrayRef = popped[1] = stack.pop();
							toPush = new ArrayReferenceTemporary(executingInstruction, arrayRef, index);
							break;
						case ISTORE:
						case LSTORE:
						case FSTORE:
						case DSTORE:
						case ASTORE:
							vvi = (VarInsnNode) executingInstruction;
							popped = new Temporary[1];
							Temporary valueToStore = popped[0] = stack.pop();
							valueToStore.addReference(executingInstruction, mn);


							//valueToStore = valueToStore.cloneOnInstruction(executingInstruction);
							locals.set(vvi.var, valueToStore);
							if(parent != null){
								//assignLocalToParent(parent, vvi.var, valueToStore);
							}

							executingBlock.localsSetInBlock.put(vvi.var, locals.get(vvi.var));
							temporaries.put(executingInstruction, valueToStore);
							//addToTemporariesWritten(executingBlock, valueToStore);
							break;
						case IASTORE:
						case LASTORE:
						case FASTORE:
						case DASTORE:
						case AASTORE:
						case BASTORE:
						case CASTORE:
						case SASTORE:
							popped = new Temporary[3];
							valueToStore = popped[0] = stack.pop();
							index = popped[1] = stack.pop();
							arrayRef = popped[2] = stack.pop();
							temporaries.put(executingInstruction, arrayRef.cloneOnInstruction(executingInstruction));
							//addToTemporariesWritten(executingBlock, arrayRef);//new ArrayReferenceTemporary(executingInstruction, arrayRef, index));
							break;
						case POP:
							stack.pop();
							break;
						case POP2:
							Temporary top = stack.pop();
							if(top.getType() == Type.DOUBLE_TYPE || top.getType() == Type.LONG_TYPE){
								//we're done
							}else{
								//actually popping 2
								stack.pop();
							}
							break;
						case DUP:
							stack.push(stack.peek().cloneOnInstruction(executingInstruction), executingInstruction);
							if(stack.peek() instanceof ObjectInstanceTemporary){
								((ObjectInstanceTemporary)stack.peek()).setIsDupped(true);
							}
							temporaries.put(executingInstruction, stack.peek());
							break;
						case DUP_X1:
							//element0, element1, element2 --->  element0, element2, element1, element2
							Temporary cloned = stack.peek().cloneOnInstruction(executingInstruction);
							stack.insertElementAt(cloned, executingInstruction, stack.size() - 2);
							temporaries.put(executingInstruction, cloned);
							break;
						case DUP_X2:
							Temporary beneathTop = stack.elementAt(stack.size() - 2);
							Temporary topCloned = stack.peek().cloneOnInstruction(executingInstruction);
							if(beneathTop.getType() == Type.DOUBLE_TYPE || beneathTop.getType() == Type.LONG_TYPE){
								stack.insertElementAt(topCloned, executingInstruction, stack.size() - 2);
							}else{
								stack.insertElementAt(topCloned.cloneOnInstruction(executingInstruction), executingInstruction, stack.size() - 3);
							}
							temporaries.put(executingInstruction, topCloned);
							break;
						case DUP2:
							topCloned = stack.peek().cloneOnInstruction(executingInstruction);
							if(topCloned.getType() == Type.DOUBLE_TYPE || topCloned.getType() == Type.LONG_TYPE){
								stack.push(topCloned, executingInstruction);
							}else{
								beneathTop = stack.elementAt(stack.size() - 2);
								stack.push(beneathTop.cloneOnInstruction(executingInstruction), executingInstruction);
								stack.push(topCloned, executingInstruction);
							}
							temporaries.put(executingInstruction, topCloned);
							//known limitation: can't associate two values with one key in a map
							break;
						case DUP2_X1:
							topCloned = stack.peek().cloneOnInstruction(executingInstruction);
							if(topCloned.getType() == Type.DOUBLE_TYPE || topCloned.getType() == Type.LONG_TYPE){
								stack.insertElementAt(topCloned, executingInstruction, stack.size() - 2); 
							}else{
								//E0, E1, E2, E3 ----> E0, E2, E3, E1, E2, E3
								//top == E3
								beneathTop = stack.elementAt(stack.size() - 2);
								//beneathTop == E2
								stack.insertElementAt(beneathTop.cloneOnInstruction(executingInstruction), executingInstruction, stack.size() - 3);
								//E0, E2, E1, E2, E3
								stack.insertElementAt(topCloned, executingInstruction, stack.size() - 3);
								//E0, E2, E3, E1, E2, E3
							}
							temporaries.put(executingInstruction, topCloned);
							break;
						case DUP2_X2:
							topCloned = stack.peek().cloneOnInstruction(executingInstruction);
							beneathTop = stack.elementAt(stack.size() - 2);
							//Temporary thirdFromTop = temporariesStack.get(temporariesStack.size() - 2);
							if(topCloned.getType() == Type.DOUBLE_TYPE || topCloned.getType() == Type.LONG_TYPE){
								if(beneathTop.getType() == Type.DOUBLE_TYPE || beneathTop.getType() == Type.LONG_TYPE){
									//D0, D1, D2 --> D0, D2, D1, D2
									stack.insertElementAt(topCloned, executingInstruction, stack.size() - 2);
								}else{
									//E0, E1, E2, D3 --> E0, D3, E1, E2, D3
									stack.insertElementAt(topCloned, executingInstruction, stack.size() - 3);
								}
							}else{
								Temporary thirdFromTop = stack.elementAt(stack.size() - 3);
								if(thirdFromTop.getType() == Type.DOUBLE_TYPE || thirdFromTop.getType() == Type.LONG_TYPE){
									//E0, D1, E2, E3 --> E0, E2, E3, D1, E2, E3
									//E0, E2, D1, E2, E3
									stack.insertElementAt(beneathTop.cloneOnInstruction(executingInstruction), executingInstruction, stack.size() - 3);
									//E0, E2, E3, D1, E2, E3
									stack.insertElementAt(topCloned, executingInstruction, stack.size() - 3);
								}else{
									//E0, E1, E2, E3, E4 --> E0, E3, E4 E1, E2, E3, E4
									stack.insertElementAt(beneathTop.cloneOnInstruction(executingInstruction), executingInstruction, stack.size() - 4);
									stack.insertElementAt(topCloned, executingInstruction, stack.size() - 4);
								}
							}
							break;
						case SWAP:
							Temporary temporary = stack.pop();
							stack.insertElementAt(temporary, executingInstruction, stack.size() - 1);
							break;
						case IADD:
						case LADD:
						case FADD:
						case DADD:
							popped = new Temporary[2];
							Temporary rhs = popped[0] = stack.pop();
							Temporary lhs = popped[1] = stack.pop();
							toPush = new BinaryOperatorTemporary(executingInstruction, rhs, lhs, BinaryOperatorTemporary.ADD);
							break;
						case ISUB:
						case LSUB:
						case FSUB:
						case DSUB:
							popped = new Temporary[2];
							rhs = popped[0] = stack.pop();
							lhs = popped[1] = stack.pop();
							toPush = new BinaryOperatorTemporary(executingInstruction, rhs, lhs, BinaryOperatorTemporary.SUB);
							break;
						case IMUL:
						case LMUL:
						case FMUL:
						case DMUL:
							popped = new Temporary[2];
							rhs = popped[0] = stack.pop();
							lhs = popped[1] = stack.pop();
							toPush = new BinaryOperatorTemporary(executingInstruction, rhs, lhs, BinaryOperatorTemporary.MUL);
							break;
						case IDIV:
						case LDIV:
						case FDIV:
						case DDIV:
							popped = new Temporary[2];
							rhs = popped[0] = stack.pop();
							lhs = popped[1] = stack.pop();
							toPush = new BinaryOperatorTemporary(executingInstruction, rhs, lhs, BinaryOperatorTemporary.DIV);
							break;
						case IREM:
						case LREM:
						case FREM:
						case DREM:
							popped = new Temporary[2];
							rhs = popped[0] = stack.pop();
							lhs = popped[1] = stack.pop();
							toPush = new BinaryOperatorTemporary(executingInstruction, rhs, lhs, BinaryOperatorTemporary.REM);
							break;
						case INEG:
						case LNEG:
						case FNEG:
						case DNEG:
							popped = new Temporary[1];
							temporary = popped[0] = stack.pop();
							toPush = new NegateOperatorTemporary(executingInstruction, temporary);
							break;
						case ISHL:
						case LSHL:
						case ISHR:
						case LSHR:
							popped = new Temporary[2];
							rhs = popped[0] = stack.pop();
							lhs = popped[1] = stack.pop();
							toPush = new BinaryOperatorTemporary(executingInstruction, rhs, lhs, BinaryOperatorTemporary.SHR);
							break;
						case IUSHR:
						case LUSHR:
							popped = new Temporary[2];
							rhs = popped[0] = stack.pop();
							lhs = popped[1] = stack.pop();
							toPush = new BinaryOperatorTemporary(executingInstruction, rhs, lhs, BinaryOperatorTemporary.USHR);
							break;
						case IAND:
						case LAND:
							popped = new Temporary[2];
							rhs = popped[0] = stack.pop();
							lhs = popped[1] = stack.pop();
							toPush = new BinaryOperatorTemporary(executingInstruction, rhs, lhs, BinaryOperatorTemporary.AND);
							break;
						case IOR:
						case LOR:
							popped = new Temporary[2];
							rhs = popped[0] = stack.pop();
							lhs = popped[1] = stack.pop();
							toPush = new BinaryOperatorTemporary(executingInstruction, rhs, lhs, BinaryOperatorTemporary.OR);
							break;
						case IXOR:
						case LXOR:
							popped = new Temporary[2];
							rhs = popped[0] = stack.pop();
							lhs = popped[1] = stack.pop();
							toPush = new BinaryOperatorTemporary(executingInstruction, rhs, lhs, BinaryOperatorTemporary.XOR);
							break;
						case IINC:
							/*
							 *
							vvi = (VarInsnNode) executingInstruction;
							popped = new Temporary[1];
							Temporary valueToStore = popped[0] = stack.pop();
							valueToStore.addReference(executingInstruction, mn);

							valueToStore = valueToStore.cloneOnInstruction(executingInstruction);
							locals.set(vvi.var, valueToStore);
							if(parent != null){
								assignLocalToParent(parent, vvi.var, valueToStore);
							}
							executingBlock.localsSetInBlock.put(vvi.var, locals.get(vvi.var));
							temporaries.put(executingInstruction, valueToStore);
							 */
							IincInsnNode iin = (IincInsnNode) executingInstruction;
							Temporary localTemp = locals.get(iin.var);
							localTemp.forceConstancy(Temporary.NOT_CONSTANT);
							/*Temporary toStore = new BinaryOperatorTemporary(executingInstruction,
									new ConstantTemporary(executingInstruction, iin.incr, Type.INT_TYPE),
									locals.get(iin.var), BinaryOperatorTemporary.ADD);*/
							//locals.set(iin.var, toStore);
							//toStore.forceConstancy(Temporary.NOT_CONSTANT);
							if(parent != null){
								//assignLocalToParent(parent, iin.var, localTemp);
							}
							//localTemp.forceConstancy(Temporary.NOT_CONST`1ANT);
							executingBlock.localsSetInBlock.put(iin.var, localTemp);
							//addToTemporariesWritten(executingBlock, locals.get(iin.var));
							break;
						case I2L:
						case I2F:
						case I2D:
						case L2I:
						case L2F:
						case L2D:
						case F2I:
						case F2L:
						case F2D:
						case D2I:
						case D2L:
						case D2F:
						case I2B:
						case I2C:
						case I2S:
							popped = new Temporary[1];
							temporary = popped[0] = stack.pop();
							toPush = new CastOperatorTemporary(executingInstruction, temporary, executingInstruction.getOpcode());
							break;
						case LCMP:
						case FCMPL:
						case FCMPG:
						case DCMPL:
						case DCMPG:
							popped = new Temporary[2];
							rhs = popped[1] = stack.pop();
							lhs = popped[0] = stack.pop();
							toPush = new CompareOperatorTemporary(executingInstruction, lhs, rhs, executingInstruction.getOpcode());
							break;
						case IFEQ:
						case IFNE:
						case IFLT:
						case IFGE:
						case IFGT:
						case IFLE:
							popped = new Temporary[1];
							lhs = popped[0] = stack.pop();
							//branch statements
							//toPush = new CompareOperatorTemporary(executingInstruction, lhs, null, executingInstruction.getOpcode());
							break;
						case IF_ICMPEQ:
						case IF_ICMPNE:
						case IF_ICMPLT:
						case IF_ICMPGE:
						case IF_ICMPGT:
						case IF_ICMPLE:
						case IF_ACMPEQ:
						case IF_ACMPNE:
							popped = new Temporary[2];
							rhs = popped[1] = stack.pop();
							lhs = popped[0] = stack.pop();
							//branch statements
							//toPush = new CompareOperatorTemporary(executingInstruction, lhs, rhs, executingInstruction.getOpcode());
							break;
						case GOTO:
							break;
						case JSR:
							throw new UnsupportedOperationException("JSR not supported");
						case RET:
							throw new UnsupportedOperationException("RET not supported");
						case TABLESWITCH:
						case LOOKUPSWITCH:
							popped = new Temporary[1];
							popped[0] = stack.pop();
							break;
						case IRETURN:
						case LRETURN:
						case FRETURN:
						case DRETURN:
						case ARETURN:
							popped = new Temporary[1];
							popped[0] = stack.pop();
							break;
						case RETURN:
							break;
						case GETSTATIC:
							FieldInsnNode fin = (FieldInsnNode) executingInstruction;
							toPush = getFieldTemporary(executingInstruction, mn, null, fin.owner, fin.name, fin.desc, null);
							break;
						case PUTSTATIC:
							fin = (FieldInsnNode) executingInstruction;
							popped = new Temporary[1];
							valueToStore = popped[0] = stack.pop();
							toPush = getFieldTemporary(executingInstruction, mn, null, fin.owner, fin.name, fin.desc, valueToStore);
							fieldsWritten.put(executingInstruction, (FieldTemporary)toPush);
							break;
						case GETFIELD:
							fin = (FieldInsnNode) executingInstruction;
							popped = new Temporary[1];
							Temporary objectRef = popped[0] = stack.pop();
							toPush = getFieldTemporary(executingInstruction, mn, objectRef, fin.owner, fin.name, fin.desc, null);
							toPush.addReference(executingInstruction, mn);
							break;
						case PUTFIELD:
							fin = (FieldInsnNode) executingInstruction;
							popped = new Temporary[2];
							valueToStore = popped[0] = stack.pop();
							objectRef = popped[1] = stack.pop();
							toPush = getFieldTemporary(executingInstruction, mn, objectRef, fin.owner, fin.name, fin.desc, valueToStore);
							fieldsWritten.put(executingInstruction, (FieldTemporary)toPush);
							break;
						case INVOKESPECIAL:
							MethodInsnNode min = (MethodInsnNode) executingInstruction;
							Type[] args = Type.getArgumentTypes(min.desc);
							Temporary instance;
							if((stack.size() - args.length - 1) >= 0){
								instance = stack.elementAt(stack.size() - args.length - 1);
							}else{
								instance = null;
							}

							if(instance != null && instance instanceof ObjectInstanceTemporary && ((ObjectInstanceTemporary)instance).isDupped()
									&& min.name.equals("<init>")){
								/*
								 * What it is:
								 * new X (X); push 1
								 * dup (X, X); push top
								 * invokespecial (X); pop 1, change fields
								 * 
								 * What we're simulating
								 * new X (X); push 1
								 * dup (X, X); push top
								 * invokespecial (X); pop 2, push first X
								 * 
								 * Purpose: so that the DUP instruction does not
								 * break contiguous blocks in StackManipulator
								 */
								popped = new Temporary[args.length + 1 + 1]; //1 for "this",	
								for(int i = 0; i < popped.length; i++){
									popped[popped.length - i - 1] = stack.pop();
								}
								toPush = new InvokeSpecialTemporary(executingInstruction, popped, min.name, min.desc);
								((ObjectInstanceTemporary)instance).setIsDupped(false);
								break;
							}else{
								//fallthrough
							}
						case INVOKEVIRTUAL:
						case INVOKESTATIC:
						case INVOKEINTERFACE:
							min = (MethodInsnNode) executingInstruction;
							args = Type.getArgumentTypes(min.desc);
							if(executingInstruction.getOpcode() == INVOKESTATIC){
								popped = new Temporary[args.length];
							}else{
								popped = new Temporary[args.length + 1];
							}
							for(int i = 0; i < popped.length; i++){
								popped[popped.length - i - 1] = stack.pop();
							}
							Type retType = Type.getReturnType(min.desc);
							toPush = new MethodInvocationTemporary(executingInstruction, popped, min.owner, min.name, min.desc, retType);
							break;
						case INVOKEDYNAMIC:
							InvokeDynamicInsnNode idin = (InvokeDynamicInsnNode) executingInstruction;
							args = Type.getArgumentTypes(idin.desc);

							popped = new Temporary[args.length];
							for(int i = 0; i < popped.length; i++){
								popped[popped.length - i - 1] = stack.pop();
							}
							retType = Type.getReturnType(idin.desc);
							toPush = new MethodInvocationTemporary(executingInstruction, popped, idin.bsm.toString(), idin.name, idin.desc, retType);

							break;
						case NEW:
							TypeInsnNode tin = (TypeInsnNode) executingInstruction;
							toPush = new ObjectInstanceTemporary(executingInstruction, Type.getType('L' + tin.desc + ';'));
							break;
						case NEWARRAY:
							IntInsnNode inin = (IntInsnNode) executingInstruction;
							popped = new Temporary[1];
							Temporary count = popped[0] = stack.pop();
							switch(inin.operand){
							case 4: //boolean
								type = Type.BOOLEAN_TYPE;
								break;
							case 5:
								type = Type.CHAR_TYPE;
								break;
							case 6:
								type = Type.FLOAT_TYPE;
								break;
							case 7:
								type = Type.DOUBLE_TYPE;
								break;
							case 8:
								type = Type.BYTE_TYPE;
								break;
							case 9:
								type = Type.SHORT_TYPE;
								break;
							case 10:
								type = Type.INT_TYPE;
								break;
							case 11:
								type = Type.LONG_TYPE;
								break;
							default:
								throw new RuntimeException("Unknown array type: " + inin.operand);	
							}
							toPush = new ArrayInstanceTemporary(executingInstruction, type, count);
							break;
						case ANEWARRAY:
							tin = (TypeInsnNode) executingInstruction;
							popped = new Temporary[1];
							count = popped[0] = stack.pop();
							toPush = new ArrayInstanceTemporary(executingInstruction, Type.getType(tin.desc), count);
							break;
						case ARRAYLENGTH:
							popped = new Temporary[1];
							arrayRef = popped[0] = stack.pop();
							toPush = new ArrayLengthOperator(executingInstruction, arrayRef);
							break;
						case ATHROW:
							temporary = stack.pop();
							while(stack.size() > 0){
								stack.pop();
							}
							stack.push(temporary, executingInstruction);
							break;
						case CHECKCAST:
							//we can't model this nor do we need to
							break;
						case INSTANCEOF:
							tin = (TypeInsnNode) executingInstruction;
							popped = new Temporary[1];
							temporary = popped[0] = stack.pop();
							toPush = new InstanceofOperatorTemporary(executingInstruction, temporary, Type.getType(tin.desc));
							break;
						case MONITORENTER:
						case MONITOREXIT:
							popped = new Temporary[1];
							popped[0] = stack.pop();
							break;
						case MULTIANEWARRAY:
							MultiANewArrayInsnNode manain = (MultiANewArrayInsnNode) executingInstruction;
							type = Type.getType(manain.desc);
							popped = new Temporary[manain.dims];
							for(int i = 0; i < popped.length; i++){
								popped[popped.length - i - 1] = stack.pop();
							}
							toPush = new ArrayInstanceTemporary(executingInstruction, type, popped);
							break;
						case IFNULL:
						case IFNONNULL:
							popped = new Temporary[1];
							popped[0] = stack.pop();
							break;
						default:
							throw new RuntimeException("Unknown opcode: " + executingInstruction.getOpcode() + "(" + Printer.OPCODES[executingInstruction.getOpcode()] + ")");
						}
						//operandsResultPerInsn.put(executingInstruction, new Tuple<>(popped, toPush));
						if(toPush != null){
							if(toPush.getType() != Type.VOID_TYPE){
								stack.push(toPush, executingInstruction);
							}
							temporaries.put(executingInstruction, toPush); //we want to add methods that return VOID to temporaries for StackManipulator
						}
						if(stack.size() > maxStack){
							maxStack = stack.size();
						}
						statePerInsn.put(executingInstruction, new Tuple<>(stack.clone(),
								locals.toArray(new Temporary[locals.size()])));
						if(popped != null){
							for(Temporary pop : popped){
								addToTemporariesRead(executingBlock, pop);
							}
						}
						log.finest("\t\tPopped: " + (popped == null ? "null" : Arrays.toString(popped)));
						log.finest("\t\tTo push: " + toPush);
						log.finest("\t\tTemporaries after execution: " + stack);
						log.finest("\t\tLocals after execution: " + locals);

						//System.out.println(Textifier.OPCODES[executingInstruction.getOpcode()]);

						//System.out.println("\t\tStack: " + temporariesStack
						//	+ "\n\t\tLocals: " + locals);

					}
				}catch(Throwable throwable){
					//throwable.printStackTrace();
					mn.instructions.get(0); //build index of instructions
					final StringBuilder errorMessage =
							new StringBuilder("Symbolic execution of ").append(methodNodeToOwnerMap.get(mn).name)
							.append('.').append(mn.name).append(mn.desc).append(" failed at Block ")
							.append(executingBlock.toString()).append(" at Instruction ")
							.append(executingInstruction.getIndex()).append("\n\tPsuedo stack trace: \n");//+ "." + mn.name + mn.desc +" failed."
					//+ "\nPsuedo Stack Trace: \n");
					if(executingInstruction.getPrevious().getOpcode() != -1){
						//Tuple<Temporary[], Temporary> info = operandsResultPerInsn.get(executingInstruction.getPrevious());
						errorMessage.append("\tPrevious instruction that executed: ").append(Textifier.OPCODES[executingInstruction.getPrevious().getOpcode()]).append(" (Instruction ").append(executingInstruction.getPrevious().getIndex()).append(')').append('\n');
						//errorMessage.append("\t\t").append(info.val2 == null ? "null" : info.val2.toString()).append('\n');
					}
					errorMessage.append("\tInstruction that failed to execute: ").append(Textifier.OPCODES[executingInstruction.getOpcode()]).append(" (Instruction ").append(executingInstruction.getIndex()).append(')').append('\n');
					errorMessage.append("\tExecuting block stack at start: " + executingBlock.stackAtStart + "\n\tCurrently\t: " + stack).append('\n');
					errorMessage.append("\tLocals: " + locals).append('\n');
					Textifier textifier = new Textifier();
					mn.accept(new TraceMethodVisitor(textifier));
					for(int i = 0; i < textifier.text.size(); i++){
						//errorMessage.append(i).append(": ").append(textifier.text.get(i));
					}
					log.severe(errorMessage.toString());
					return;
				}


				//executingBlock.stackAtEnd.clear();
				//executingBlock.stackAtEnd.addAll(temporariesStack);
				//executingBlock.localsAtEnd.clear();
				//executingBlock.localsAtEnd.addAll(locals);

				log.finest("Stack at end of " + executingBlock + ": " + stack);
				log.finest("Locals at end of " + executingBlock + ": " + locals);

				Set<BasicBlock> children = childrenMap.get(executingBlock);

				/*if(children != null){
					for(BasicBlock child : children){
						child.stackAtStart = mergeStacksAndClone(child.stackAtStart, stack);

						child.localsAtStart.clear();
						child.localsAtStart.addAll(locals);

						log.finest("Adding " + child + " to execution queue");
						toExecute.push(child);
						//TODO go through a loop again so that the values re-flow
						//and mark NOT_CONSTANT whatever is actually not constant
						//through the phi operators
					}
				}*/
				for(BlockEdge edge : executingBlock.successors){
					BasicBlock successor = edge.b2;
					if(!executed.contains(successor)){

						boolean executedAllPredecessors = true;
						for(BlockEdge pred : successor.predecessors){
							if(pred.classification != BlockEdge.Classification.BACK && !executed.contains(pred.b1)){
								executedAllPredecessors = false;
								break;
							}
						}



						mergeLocals(successor.locals, executingBlock.locals);


						if(executedAllPredecessors){



							//successor.locals.clear();
							//successor.locals.addAll(locals);
							successor.stackAtStart = mergeStacksAndClone(successor.stackAtStart, stack);
							if(!toExecute.contains(successor)){
								toExecute.add(successor);
							}
						}



					}
				}


				/*
				for(BlockEdge b : executingBlock.edgesFromThis){
					b.b2.stackAtStart.clear();
					b.b2.stackAtStart.addAll(temporariesStack);
					b.b2.localsAtStart.clear();
					b.b2.localsAtStart.addAll(locals);
					//if(b.classification == BlockEdge.Classification.TREE){
					if(!executed.contains(b.b2) && !toExecute.contains(b.b2)){
						toExecute.add(b.b2);
						log.finest("Adding " + b.b2 + " to execution queue");
						//toExecute.push(b.b2);
					}
					//}
				}*/
				log.finest("Execution queue after " + executingBlock + ": " + toExecute);;

			}
			mn.maxStack = maxStack;
			log.finest("Finished symbolic execution.");
		}

		private void mergeLocals(ArrayList<Temporary> localsToMergeInto, ArrayList<Temporary> mergingFrom){
			for(int i = 0; i < mergingFrom.size(); i++){
				Temporary curVal = mergingFrom.get(i);
				Temporary successorVal = localsToMergeInto.get(i);
				if(curVal == successorVal){
					continue;
				}else if(curVal == null){
					continue;
				}else if(successorVal == null){
					localsToMergeInto.set(i, curVal);
				}else if(curVal.equals(successorVal)){
					continue;
				}else{
					boolean curIsPhi = curVal instanceof PhiTemporary;
					boolean successorIsPhi = successorVal instanceof PhiTemporary;
					Temporary[] toMerge;
					if(curIsPhi){
						PhiTemporary curPhi = (PhiTemporary) curVal;
						if(successorIsPhi){
							PhiTemporary successorPhi = (PhiTemporary) successorVal;
							toMerge = new Temporary[curPhi.mergedTemporaries.length + successorPhi.mergedTemporaries.length];
							System.arraycopy(successorPhi.mergedTemporaries, 0, toMerge, 0, successorPhi.mergedTemporaries.length);
							System.arraycopy(curPhi.mergedTemporaries, 0, toMerge, successorPhi.mergedTemporaries.length, curPhi.mergedTemporaries.length);
						}else{
							toMerge = new Temporary[curPhi.mergedTemporaries.length + 1];
							toMerge[0] = successorVal;
							System.arraycopy(curPhi.mergedTemporaries, 0, toMerge, 1, curPhi.mergedTemporaries.length);
						}
					}else if(successorIsPhi){
						PhiTemporary successorPhi = (PhiTemporary) successorVal;
						toMerge = new Temporary[successorPhi.mergedTemporaries.length + 1];
						System.arraycopy(successorPhi.mergedTemporaries, 0, toMerge, 0, successorPhi.mergedTemporaries.length);
						toMerge[toMerge.length - 1] = curVal;
					}else{
						toMerge = new Temporary[]{successorVal, curVal};
					}
					localsToMergeInto.set(i, new PhiTemporary(toMerge, i));
				}
			}
		}



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

	private ClassHierarchyTreeNode defineClassHierarchyTreeNode(String className){
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

		/*classes.parallelStream().forEach((cn) -> cn.methods.parallelStream().forEach((mn) -> {
			if(Modifier.isNative(mn.access) || Modifier.isAbstract(mn.access)){
				return;
			}
			methodInformations.put(mn, new MethodInformation(mn));
		}));*/

		for(ClassNode cn : classes){
			for(MethodNode mn : cn.methods){
				if(Modifier.isNative(mn.access) || Modifier.isAbstract(mn.access)){
					continue;
				}
				methodInformations.put(mn, new MethodInformation(mn));
			}
		}

		//		for(ClassNode cn : classes){
		//			for(FieldNode fn : cn.fields){
		//				new FieldNodeTemporary(cn, fn);
		//			}
		//		}

		final long end = System.currentTimeMillis();
		System.out.println("Gathered all relevant information in " + (end - start) + " ms.");

	}

}
