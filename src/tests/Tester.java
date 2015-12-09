package tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import org.armanious.csci1260.Entry;

public class Tester {
	
	private static final String[] NO_INPUT_NECESSARY_TESTS = {
			"tests/decaf/Animals",
			"tests/decaf/ansi",
			"tests/decaf/c",
			"tests/decaf/Cars",
			"tests/decaf/Comments",
			"tests/decaf/Fib",
			"tests/decaf/HashTable",
			"tests/decaf/Hello",
			"tests/decaf/lc",
			"tests/decaf/Morgan",
			"tests/decaf/Pascal",
			"tests/decaf/PrintArgs",
			"tests/decaf/Protect",
			"tests/decaf/Short",
			"tests/decaf/SuperTest",
	};
	static {
		Arrays.sort(NO_INPUT_NECESSARY_TESTS);
	}
	
	public static void main(String...unused) throws Throwable {
		final String classPath = System.getProperty("java.class.path");
		final File personalTestsParent = new File(classPath, "tests" + File.separator + "personal");
		String[] args = new String[]{personalTestsParent.toString(), 
				"run_output=true",
				"obfuscation.compress_output=true",
				"obfuscation.main_class=tests.personal.Hello",
				"obfuscation.name_pattern=Il",
				"obfuscation.name_length=10",
				"obfuscation.use_obfuscation=true",
				"optimization.use_optimization=true"};
		Entry.main(args);
		
		System.out.println("\n\n\n\n\n\n\n");
		
		final File decafTestsParent = new File(classPath, "tests" + File.separator + "decaf");
		args = new String[]{decafTestsParent.toString(), 
				"run_output=false",
				"obfuscation.compress_output=false",
				"obfuscation.name_pattern=Il",
				"obfuscation.name_length=10",
				"obfuscation.use_obfuscation=true",
				"optimization.use_optimization=true"};
		Entry.main(args);
		
		
		final File obfuscatedOutputDirectory = new File(classPath, "tests" + File.separator + "obfuscated");
		final File obfuscationNameMap = new File(obfuscatedOutputDirectory, "obfuscation_map.txt");
		
		final URLClassLoader cl = new URLClassLoader(new URL[]{obfuscatedOutputDirectory.toURI().toURL()});
		
		try(final BufferedReader br = new BufferedReader(new FileReader(obfuscationNameMap))){
			String line;
			while((line = br.readLine()) != null){
				if(line.length() == 0 || Character.isWhitespace(line.charAt(0)) || line.indexOf('$') >= 0) continue;
				final int idx = line.indexOf('-');
				final String unobfuscated = line.substring(0, idx - 1);
				final String obfuscatedBinary = line.substring(idx + 4).replace('/', '.');
				if(Arrays.binarySearch(NO_INPUT_NECESSARY_TESTS, unobfuscated) >= 0){
					final Class<?> clazz = cl.loadClass(obfuscatedBinary);
					final Method main = clazz.getMethod("main", new Class[]{String[].class});
					System.out.println("Running " + clazz.getName() + " (" + unobfuscated + "):");
					main.invoke(null, new Object[]{new String[]{}});
					System.out.println("\n");
				}
			}
		}
		cl.close();
	}

}
