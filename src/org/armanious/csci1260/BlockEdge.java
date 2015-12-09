package org.armanious.csci1260;

public class BlockEdge {

	public static enum Type {TRUE, FALSE, ALWAYS, NEVER};
	public static enum Classification {TREE, BACK, FORWARD, CROSS}

	public final BlockEdge.Type type;
	public BlockEdge.Classification classification;
	public final BasicBlock b1;
	public final BasicBlock b2;

	public BlockEdge(BlockEdge.Type type, BasicBlock b1, BasicBlock b2){
		this.type = type;
		this.b1 = b1;
		this.b2 = b2;
	}

}