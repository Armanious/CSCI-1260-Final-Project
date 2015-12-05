package org.armanious.csci1260;

import java.io.File;
import java.io.IOException;

public class Tester {

	public static void main(String...unused) throws IOException {
		System.out.println("IM HERE");
		//final String PROGRAM_TO_TEST_ON = "CSCI 1260 Tests";
		//File f = new File(new File(System.getProperty("user.dir")).getParentFile(), PROGRAM_TO_TEST_ON + File.separator + "bin");
		File f = new File("/Users/david/OneDrive/Documents/workspace/For Andrew/bin");
		String[] args = new String[]{f.toString(), 
				"run_output=true",
				"obfuscation.compress_output=false",
				"obfuscation.main_class=org.armanious.AndrewFlipperTool",
				"obfuscation.name_pattern=Il",
				"obfuscation.name_length=10",
				"obfuscation.use_obfuscation=true",
				"optimization.use_optimization=true"};
		Entry.main(args);
	}

}
