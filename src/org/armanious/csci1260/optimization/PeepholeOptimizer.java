package org.armanious.csci1260.optimization;

import org.armanious.csci1260.DataManager;
import org.armanious.csci1260.DataManager.BinaryOperatorTemporary;
import org.armanious.csci1260.DataManager.MethodInformation;
import org.armanious.csci1260.DataManager.NegateOperatorTemporary;
import org.armanious.csci1260.DataManager.Temporary;
import org.objectweb.asm.tree.AbstractInsnNode;

public class PeepholeOptimizer {
	
	private final DataManager dm;
	
	public PeepholeOptimizer(DataManager dm){
		this.dm = dm;
	}
	
	/*
	 * dup, dup => dup2
	 * pop, pop => pop2
	 * _store X, _load X => dup/dup2, _store X (check if need to increase max stack by 1)
	 * _load X, _load X => _load X, dup/dup2
	 * _load X, _const1/_constM1, _add, _storeX => _inc/_dec X
	 * 
	 * BinaryOperatorTemporary:
	 * x * 1 || 1 * x || x / 1 || x + 0 || x - 0 => x
	 * x * 0 || 0 * x || 0 / x || x - x => 0
	 * x * 2^n => x << n
	 * x / 2^n => x >>> n
	 * x + (-y) => x - y
	 * x - (-y) => x + y
	 * -x + y => y - x
	 * 
	 * NegateOperatorTemporary:
	 * -0 => 0
	 * -(-x) => x
	 * 
	 * ArrayLengthOperatorTemporary? Could it be done in ConstantFolder? yes...
	 * 
	 * _store X, _load X; and _load X is not called again before another _store X executes => nothing
	 * i.e.: String s = object.toString(); return s; ==> return object.toString();
	 * Single-use variables removed essentially
	 */
	private boolean optimize(MethodInformation mi){
		boolean changed = false;
		
		for(Temporary t : mi.temporaries.values()){
			if(t instanceof BinaryOperatorTemporary){
				if(optimizeBinaryOperatorTemporary(mi, (BinaryOperatorTemporary) t)){
					changed = true;
				}
			}else if(t instanceof NegateOperatorTemporary){
				//-0 => 0; -(-x) => x
			}
		}
		
		AbstractInsnNode prev = null;
		
		
		return changed;
	}
	
	private boolean optimizeBinaryOperatorTemporary(MethodInformation mi, BinaryOperatorTemporary bot){
		boolean changed = false;
		
		return changed;
	}
	
	public void optimize(){
		for(MethodInformation mi : dm.methodInformations.values()){
			if(optimize(mi)){
				mi.recompute();
			}
		}
	}

}
