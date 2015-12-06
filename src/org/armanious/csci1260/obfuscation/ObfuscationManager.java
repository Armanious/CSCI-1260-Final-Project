package org.armanious.csci1260.obfuscation;

import java.io.File;
import java.util.ArrayList;

import org.armanious.csci1260.DataManager;
import org.objectweb.asm.tree.ClassNode;

public final class ObfuscationManager {

	private final ArrayList<Obfuscator> obfuscators = new ArrayList<>();
	private final ArrayList<ClassNode> classes;
	
	public ObfuscationManager(ArrayList<ClassNode> classes){
		this.classes = classes;
	}
	
	public ArrayList<ClassNode> getClassNodes(){
		return classes;
	}
	
	public void addObfuscationPhase(Obfuscator obfuscator){
		obfuscators.add(obfuscator);
	}
	
	public void runAllObfuscators(){
		for(Obfuscator obfuscator : obfuscators){
			obfuscator.obfuscate();
		}
	}
	
	public static void run(DataManager dm, ArrayList<ClassNode> classes,
			String name_pattern, int name_length,
			boolean preserve_package_structure, File outputFileForNameRemapping,
			boolean use_stack_manipulation){
		ObfuscationManager om = new ObfuscationManager(classes);
		//TODO
		//om.addObfuscationPhase(new NameObfuscatorBeta(dm, om, name_pattern, name_length, preserve_package_structure, outputFileForNameRemapping));
		if(use_stack_manipulation){
			new StackManipulator(dm).obfuscate();
			//om.addObfuscationPhase(new SimpleStackManipulator(om));
		}
		om.runAllObfuscators();
		new StringLiteralEncryption(dm).obfuscate();
		//new ExcecptionBlockObfuscator(dm).obfuscate();
		//do StringLiteralEncryption after StackManipulation(and others) because it really 
		//messes up the instructions; also if we fix reflection in the name obfuscator,
		//we want to do that before we cipher the string literals
	}

}
