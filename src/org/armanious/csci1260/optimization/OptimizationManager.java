package org.armanious.csci1260.optimization;

import java.util.ArrayList;

import org.armanious.csci1260.DataManager;
import org.objectweb.asm.tree.ClassNode;

public final class OptimizationManager {
	
	private OptimizationManager(){}
	
	public static void run(DataManager dm, ArrayList<ClassNode> classes, boolean inline_methods){
		new LoopOptimizations(dm).optimize();
		new ConstantFolder(dm).optimize();
		//new RedundantComputationRemover(dm).optimize(); //TODO implement
		//new DeadCodeRemover(dm).optimize(); //TODO
		//new PeepholeOptimizer(dm).optimize(); //TODO
	}

}
