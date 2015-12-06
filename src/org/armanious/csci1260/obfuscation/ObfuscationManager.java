package org.armanious.csci1260.obfuscation;

import java.io.File;
import java.util.ArrayList;

import org.armanious.csci1260.DataManager;
import org.objectweb.asm.tree.ClassNode;

public final class ObfuscationManager {
	
	public static void run(DataManager dm, ArrayList<ClassNode> classes,
			String name_pattern, int name_length,
			boolean preserve_package_structure, File outputFileForNameRemapping,
			boolean use_stack_manipulation){
		
		new NameObfuscatorBeta(dm, name_pattern, name_length, preserve_package_structure, outputFileForNameRemapping).obfuscate();
		if(use_stack_manipulation){
			new StackManipulator(dm).obfuscate();
		}
		new StringLiteralEncryption(dm).obfuscate();
		//new ExcecptionBlockObfuscator(dm).obfuscate();
		//new Inliner(dm).obfuscate();
	}

}
