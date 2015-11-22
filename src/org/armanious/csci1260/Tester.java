package org.armanious.csci1260;

import java.io.File;
import java.io.IOException;

public class Tester {

	public static void main(String...unused) throws IOException{
		File f = new File("/Users/david/Desktop/maze/MazeMultiplayer.jar");
		String[] args = new String[]{f.toString(), 
				"run_output=true",
				"obfuscation.compress_output=true",
				"obfuscation.main_class=Battleship",
				"obfuscation.name_pattern=Il",
				"obfuscation.name_length=10",
				"obfuscation.use_obfuscation=true",
				"optimization.use_optimization=false"};
		Entry.main(args);
	}

}
