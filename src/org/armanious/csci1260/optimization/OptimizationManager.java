package org.armanious.csci1260.optimization;

import java.util.ArrayList;

import org.armanious.csci1260.DataManager;
import org.objectweb.asm.tree.ClassNode;

public final class OptimizationManager {
	
	private OptimizationManager(){}
	
	public static void run(DataManager dm, ArrayList<ClassNode> classes, boolean inline_methods){
		new RedundantComputationRemover(dm).optimize();
		new ConstantFolder(dm).optimize();
		new LoopOptimizations(dm).optimize();
		new DeadCodeRemover(dm).optimize();
	}

}
