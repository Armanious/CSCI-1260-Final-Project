package org.armanious.csci1260.obfuscation;

public abstract class Obfuscator {
	
	protected final ObfuscationManager om;
	
	public Obfuscator(ObfuscationManager manager) {
		this.om = manager;
	}
	
	public abstract void obfuscate();

}
