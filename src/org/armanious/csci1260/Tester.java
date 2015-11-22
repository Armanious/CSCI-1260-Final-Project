package org.armanious.csci1260;

import java.io.File;
import java.io.IOException;

public class Tester {

	public static void main(String...unused) throws IOException {
		final String PROGRAM_TO_TEST_ON = "CSCI 1260 Final Project";
		File f = new File(new File(System.getProperty("user.dir")).getParentFile(), PROGRAM_TO_TEST_ON + File.separator + "bin");
		//File f = new File("/Users/david/Desktop/jbe/bin");
		String[] args = new String[]{f.toString(), 
				"run_output=true",
				"obfuscation.compress_output=true",
				"obfuscation.main_class=org.armanious.csci1260.Entry",
				"obfuscation.name_pattern=Il",
				"obfuscation.name_length=10",
				"obfuscation.use_obfuscation=true",
				"optimization.use_optimization=false"};
		Entry.main(args);
	}

}
