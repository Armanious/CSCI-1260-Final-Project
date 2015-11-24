package org.armanious.csci1260.optimization;

import org.armanious.csci1260.DataManager;
import org.armanious.csci1260.DataManager.MethodInformation;

public class PeepholeOptimizations {
	
	private final DataManager dm;
	
	public PeepholeOptimizations(DataManager dm){
		this.dm = dm;
	}
	
	//TODO
	/*
	 * dup
	 * dup  => dup2
	 * 
	 * pop
	 * pop => pop2
	 */
	public void optimize(){
		for(MethodInformation mi : dm.methodInformations.values()){
			
		}
	}

}
