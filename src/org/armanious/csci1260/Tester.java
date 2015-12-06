package org.armanious.csci1260;

import java.io.File;
import java.io.IOException;

public class Tester {
	
	public static void main(String...unused) throws IOException {
		System.out.println("Testing");
		//final String PROGRAM_TO_TEST_ON = "CSCI 1260 Tests";
		//File f = new File(new File(System.getProperty("user.dir")).getParentFile(), PROGRAM_TO_TEST_ON + File.separator + "bin");
		File f = new File("/Users/david/OneDrive/Documents/workspace/CSCI 1260 Tests/bin");
		String[] args = new String[]{f.toString(), 
				"run_output=true",
				"obfuscation.compress_output=true",
				"obfuscation.main_class=test.hi.Hello",
				"obfuscation.name_pattern=Il",
				"obfuscation.name_length=10",
				"obfuscation.use_obfuscation=true",
				"optimization.use_optimization=true"};
		//Entry.main(args);
		
		f = new File("/Users/david/Desktop/battleship/battleship.jar");
		args = new String[]{f.toString(), 
				"run_output=true",
				"obfuscation.compress_output=false",
				"obfuscation.main_class=Battleship",
				"obfuscation.name_pattern=CTGA",
				"obfuscation.name_length=4",
				"obfuscation.use_obfuscation=true",
				"optimization.use_optimization=true"};
		Entry.main(args);		
		
		
		System.exit(0);;
		
		f = new File("/Users/david/OneDrive/Documents/workspace/CSCI 1260 Final Project/bin");
		args = new String[]{f.toString(), 
				"run_output=true",
				"obfuscation.compress_output=false",
				"obfuscation.main_class=org.armanious.csci1260.Entry",
				"obfuscation.name_pattern=CTGA",
				"obfuscation.name_length=4",
				"obfuscation.use_obfuscation=true",
				"optimization.use_optimization=true"};
		Entry.main(args);
		
		f = new File("/Users/david/OneDrive/Documents/workspace/For Andrew/bin");
		args = new String[]{f.toString(), 
				"run_output=true",
				"obfuscation.compress_output=true",
				"obfuscation.main_class=org.armanious.AndrewFlipperTool",
				"obfuscation.name_pattern=Il",
				"obfuscation.name_length=10",
				"obfuscation.use_obfuscation=true",
				"optimization.use_optimization=true"};
		Entry.main(args);
	}

}
