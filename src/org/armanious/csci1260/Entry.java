package org.armanious.csci1260;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.armanious.csci1260.obfuscation.DataCompressionObfuscator;
import org.armanious.csci1260.obfuscation.ObfuscationManager;
import org.armanious.csci1260.optimization.OptimizationManager;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;

public class Entry {

	/*
	 * 

		File runThrice = new File("lol.txt");
		runThrice.delete();
		if(!runThrice.exists()){
			try(BufferedWriter out = new BufferedWriter(new FileWriter(runThrice))){
				out.write("1");
				out.flush();
			}
		}else{
			int count = 0;
			try(BufferedReader in = new BufferedReader(new FileReader(runThrice))){
				count = Integer.parseInt(in.readLine());
				if(count == 3){
					return;
				}else{
					count = count + 1;
				}
			}
			try(BufferedWriter out = new BufferedWriter(new FileWriter(runThrice))){
				out.write(String.valueOf(count));
				out.flush();
			}
		}


	 */

	public static void main(String[] args) throws IOException {

		System.out.println(Arrays.toString(args));

		if(args == null || args.length == 0){
			final String PROGRAM_TO_TEST_ON = "CSCI 1260 Tests";
			File f = new File(new File(System.getProperty("user.dir")).getParentFile(), PROGRAM_TO_TEST_ON + File.separator + "bin");
			args = new String[]{f.toString(), 
					"run_output=true",
					"obfuscation.compress_output=false",
					//"obfuscation.main_class=test.hi.Hello",
					//"obfuscation.name_pattern=ATCG",
					//"obfuscation.name_length=4",
			/*"obfuscation.use_obfuscation=false"*/};
		}

		if(args == null || args.length == 0) printUsage();
		if(args[0].equalsIgnoreCase("-defaults")) printDefaults();
		final File file = new File(args[0]);
		if(!file.exists()) printUsage();

		parseArguments(args);

		final ArrayList<ClassNode> classDatas = new ArrayList<>();
		final boolean isJar;

		if(file.isDirectory()){
			readClassDatasRecursively(classDatas, file.listFiles());
			if(output_directory == null)
				output_directory = new File(file.getParentFile(), "obfuscated");
			isJar = false;
		}else if(args[0].endsWith(".jar")){
			readJarFile(classDatas, file);
			if(output_directory == null)
				output_directory = file.getParentFile();
			isJar = true;
		}else if(args[0].endsWith(".class")){
			readClassDatasRecursively(classDatas, new File[]{file});
			if(output_directory == null)
				output_directory = file.getParentFile();
			isJar = false;
		}else{
			printUsage();
			isJar = false;
		}


		/*for(ClassNode cn : classDatas){
			cn.accept(new TraceClassVisitor(new PrintWriter(System.out)));
			for(MethodNode mn : cn.methods){
				if(mn.name.equals("readClassDatasRecursively")){
					Textifier t = new Textifier();
					mn.accept(new TraceMethodVisitor(t));
					for(int i = 0; i < t.text.size(); i++){
						System.out.print(i + ": " + t.text.get(i));
					}
					System.err.println("Heyyy");
					mn.instructions.createGraph();
					System.out.println(mn.instructions.getFirstBlock());
				}
			}
		}

		System.exit(0);*/

		if(classDatas.size() == 0) printUsage();

		ClassNode main_class_reference = null;

		if(main_class != null){
			for(ClassNode cn : classDatas){
				if(cn.name.replace('/', '.').equals(main_class)){
					main_class_reference = cn;
					break;
				}
			}
			if(main_class_reference == null){
				printUsage();
			}
		}else{
			if(compress_output){
				printUsage();
			}
		}
		System.out.println("Loaded " + classDatas.size() + " classes.");

		DataManager dm = new DataManager(classDatas);



		//not compressed or encrypted
		//TODO remove me and ask if would like to override previous output
		if(output_directory.exists()){
			final Stack<File> toDelete = new Stack<>();
			toDelete.push(output_directory);
			while(!toDelete.isEmpty()){
				final File cur = toDelete.pop();
				if(cur.isDirectory()){
					if(cur.list().length == 0){
						cur.delete();
					}else{
						toDelete.push(cur);
						for(File child : cur.listFiles()){
							toDelete.push(child);
						}
					}
				}else{
					cur.delete();
				}
			}
		}


		//dm.getClassNode("test/hi/Helllllooo").accept(new TraceClassVisitor(new PrintWriter(System.out)));

		//System.exit(0);
		if(use_optimization){
			OptimizationManager.run(dm, classDatas, inline_methods);
		}
		if(use_obfuscation){
			if(name_remapping_file == null){
				name_remapping_file = new File(output_directory, "obfuscation_map.txt");
			}
			ObfuscationManager.run(dm, classDatas, name_pattern, name_length, /*use_names_linearly,*/
					preserve_package_structure, name_remapping_file,
					use_stack_manipulation);
		}
		//if(encrypt_output){
		//else{

		//TODO fora Temporary that has operands qith some equal to each other, store into
		//local variable and reload


		//The name of the main_class may have been obfuscated; instead we keep track of the ClassNode

		if(compress_output){
			DataCompressionObfuscator.goCrazyWildAndFree(dm, main_class_reference.name, output_directory);
		}else{
			if(isJar){
				outputClassesAsJar(classDatas, file.getName().replace(".jar", "_obfuscated.jar"), output_directory);
			}else{
				outputClasses(classDatas, output_directory);
			}
		}

		if(run_output){
			final File fileToUse;
			if(compress_output){
				fileToUse = new File(output_directory, "obfuscated.jar");
			}else if(isJar){
				fileToUse = new File(file.getName().replace(".jar", "_obfuscated.jar"));
			}else{
				fileToUse = output_directory;
			}
			try(final CustomClassLoader ccl = new CustomClassLoader(fileToUse,
					main_class_reference == null ? null : main_class_reference.name)){
				System.out.println("Attempting to run obfuscated classes.");
				if(!ccl.runMain()){
					System.err.println("Unable to find main class");
				}else{
					System.out.println("Test has been run successfully.");
				}
			}catch(Throwable e){
				System.err.println("Unable to test output.");
				e.printStackTrace();
			}
		}
	}

	private static void outputClassesAsJar(ArrayList<ClassNode> classDatas, String jarName, File directory){
		//TODO FIXME, esp. Manifest
		outputClasses(classDatas, new File(directory, jarName.replace(".jar", "")));
	}

	private static void outputClasses(ArrayList<ClassNode> classDatas, File directory){
		for(ClassNode cn : classDatas){
			final File fileForClass = new File(directory, cn.name.replace('/', File.separatorChar) + ".class");
			if(!fileForClass.getParentFile().exists()){
				fileForClass.getParentFile().mkdirs();
			}
			final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS){
				private final Map<String, ClassNode> map = classDatas.stream().collect(Collectors.toMap((ClassNode cn) -> cn.name, (cn) -> cn));
				private final Map<String, String[]> classHierarchyCache = new HashMap<>();
				//TODO replace with DataManager
				private String[] getClassHierarchyBeta(String type){
					String[] cached = classHierarchyCache.get(type);
					if(cached == null){
						final ClassNode cn = map.get(type);
						String[] superHierarchy;
						if(cn == null){
							try {
								Class<?> clazz = Class.forName(type.replace('/', '.'));
								if(clazz == Object.class){
									return new String[]{"java/lang/Object"};
								}else if(clazz.getSuperclass() == null){
									return new String[]{type.replace('/', '.'), "java/lang/Object"};
								}
								superHierarchy = getClassHierarchyBeta(clazz.getSuperclass().getName().replace('.', '/'));
							} catch (ClassNotFoundException e) {
								throw new RuntimeException(e);
							}
						}else{
							superHierarchy = getClassHierarchyBeta(cn.superName);
						}
						cached = new String[superHierarchy.length + 1];
						System.arraycopy(superHierarchy, 0, cached, 1, superHierarchy.length);
						cached[0] = type;
						classHierarchyCache.put(type, cached);
					}
					return cached;
				}
				@Override
				protected String getCommonSuperClass(String type1, String type2) {
					if(type1.equals(type2)) return type1;

					String[] ch1 = getClassHierarchyBeta(type1);
					String[] ch2 = getClassHierarchyBeta(type2);
					for(int i = 0; i < ch1.length; i++){
						for(int j = 0; j < ch2.length; j++){
							if(ch1[i].equals(ch2[j])){
								return ch1[i];
							}
						}
					}

					return null;
				}
			};
			try{
				cn.accept(cw);

				//cn.accept(new TraceClassVisitor(new PrintWriter(System.out)));
			}catch(Exception e){
				//cn.accept(new TraceClassVisitor(new PrintWriter(System.out)));
			}
			try(final FileOutputStream fos = new FileOutputStream(fileForClass)){
				fos.write(cw.toByteArray());
				fos.flush();
			} catch (IOException e) {
				System.err.println("Error writing class " + cn.name.replace('/', '.') + " to file. Stack trace: ");				e.printStackTrace();
			}
		}
	}

	private static ClassNode getClassNodeFromData(byte[] data){
		final ClassReader cr = new ClassReader(data);
		final ClassNode cn = new ClassNode();
		cr.accept(cn, /*ClassReader.SKIP_FRAMES | */ClassReader.SKIP_DEBUG);
		return cn;
	}

	private static void readClassDatasRecursively(ArrayList<ClassNode> classDatas, File[] files){
		for(File file : files){
			if(file.isDirectory()){
				readClassDatasRecursively(classDatas, file.listFiles());
			}else if(file.getName().endsWith(".class")){
				final byte[] data = readClassData(file);
				if(data != null){
					classDatas.add(getClassNodeFromData(data));
				}
			}
		}
	}

	private static byte[] readClassData(File file){
		try {
			return readInputStream(new BufferedInputStream(new FileInputStream(file)));
		} catch (IOException e) {
			System.err.println("Error reading file: " + file + ". Stack trace: ");
			e.printStackTrace();
			return null;
		}
	}

	private static void readJarFile(ArrayList<ClassNode> classDatas, File file){
		try(final JarFile jar = new JarFile(file)){
			final Enumeration<JarEntry> entries = jar.entries();
			while(entries.hasMoreElements()){
				final JarEntry next = entries.nextElement();
				try {
					final byte[] data = readInputStream(new BufferedInputStream(jar.getInputStream(next)));
					classDatas.add(getClassNodeFromData(data));
				} catch(IOException e){
					System.err.println("Error reading JarEntry: " + next.getName() + ". Stack trace: ");
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			System.err.println("Could not open JarFile: " + file + ". Stack trace: ");
			e.printStackTrace();
		}
	}

	private static byte[] readInputStream(InputStream in) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final byte[] data = new byte[4096];
		int bytesRead;
		while((bytesRead = in.read(data, 0, 4096)) != -1){
			baos.write(data, 0, bytesRead);
		}
		return baos.toByteArray();
	}

	private static void parseArguments(String[] args){
		for(int i = 1; i < args.length; i++){
			final int idx = args[i].indexOf("=");
			if(idx == -1) printUsage();
			final String name = args[i].substring(0, idx);
			final String value = args[i].substring(idx + 1);
			switch(name){
			case "output_directory":
				output_directory = new File(value);
				break;
			case "run_output":
				boolean boolTmp = value.equalsIgnoreCase("true");
				if(!boolTmp && !value.equalsIgnoreCase("false")) printUsage();
				run_output = boolTmp;
				break;
			case "obfuscation.use_obfuscation":
				boolTmp = value.equalsIgnoreCase("true");
				if(!boolTmp && !value.equalsIgnoreCase("false")) printUsage();
				use_obfuscation = boolTmp;
				break;
			case "obfuscation.name_pattern":
				name_pattern = value; //TODO check validity
				break;
			case "obfuscation.name_length":
				int intTmp = Integer.parseInt(value);
				//exception handled outside of switch, leads to printUsage()
				if(intTmp < 1 || intTmp > 64) printUsage();
				name_length = intTmp;
				break;
				/*case "obfuscation.name_overflow_pattern":
				name_overflow_pattern = value;
				break;*/
				/*case "obfuscation.use_names_linearly":
				boolTmp = value.equalsIgnoreCase("true");
				if(!boolTmp && !value.equalsIgnoreCase("false")) printUsage();
				use_names_linearly = boolTmp;
				break;*/
			case "obfuscation.preserve_package_structure":
				boolTmp = value.equalsIgnoreCase("true");
				if(!boolTmp && !value.equalsIgnoreCase("false")) printUsage();
				preserve_package_structure = boolTmp;
				break;
			case "obfuscation.name_remapping_file":
				name_remapping_file = new File(value);
				break;
			case "obfuscation.use_stack_manipulation":
				boolTmp = value.equalsIgnoreCase("true");
				if(!boolTmp && !value.equalsIgnoreCase("false")) printUsage();
				use_stack_manipulation = boolTmp;
				break;
			case "obfuscation.compress_output":
				boolTmp = value.equalsIgnoreCase("true");
				if(!boolTmp && !value.equalsIgnoreCase("false")) printUsage();
				compress_output = boolTmp;
				break;
				/*case "obfuscation.encrypt_output":
				boolTmp = value.equalsIgnoreCase("true");
				if(!boolTmp && !value.equalsIgnoreCase("false")) printUsage();
				encrypt_output = boolTmp;
				break;*/
			case "obfuscation.main_class":
				main_class = value;
				break;
			case "optimization.use_optimization":
				boolTmp = value.equalsIgnoreCase("true");
				if(!boolTmp && !value.equalsIgnoreCase("false")) printUsage();
				use_optimization = boolTmp;
				break;
			case "optimization.inline_methods":
				boolTmp = value.equalsIgnoreCase("true");
				if(!boolTmp && !value.equalsIgnoreCase("false")) printUsage();
				inline_methods = boolTmp;
				break;
				/*case "optimization.flow_analysis_optimizations":
				boolTmp = value.equalsIgnoreCase("true");
				if(!boolTmp && !value.equalsIgnoreCase("false")) printUsage();
				flow_analysis_optimizations = boolTmp;
				break;*/
			default:
				printUsage();
			}
		}
	}

	private static File output_directory = null;
	private static boolean run_output = false;

	private static boolean use_obfuscation = true;
	private static String name_pattern = "Il1";
	private static int name_length = 8;
	//private static String name_overflow_pattern = "lI";
	//private static boolean use_names_linearly = false;
	private static boolean preserve_package_structure = true;
	private static File name_remapping_file;
	private static boolean use_stack_manipulation = true;
	private static boolean compress_output = false;
	private static String main_class = null;
	//private static boolean encrypt_output = true;

	private static boolean use_optimization = true;
	private static boolean inline_methods = true;
	//privates static boolean flow_analysis_optimizations = true;

	private static void printUsage(){
		System.err.print("Arguments should be in the form of: {[.class file] | [.jar file] | [folder with .class files]}\n"
				+ "\nTo view default options, pass \"-defaults\" as the first argument\n"
				+ "\n"
				+ "\toutput_directory={dir}\n"
				+ "\trun_output={true|false}\n"
				+ "\n"
				+ "\tobfuscation.use_obfuscation={true|false}\n"
				+ "\tobfuscation.name_pattern={set of valid characters}\n"
				+ "\tobfuscation.name_length={1-64}\n"
				//+ "\tobfuscation.name_overflow_pattern={pattern}\n"
				+ "\tobfuscation.use_names_linearly={true|false}\n"
				+ "\tobfuscation.preserve_package_structure={true|false}\n"
				+ "\tobfuscation.name_remapping_file={file}\n"
				+ "\tobfuscation.use_stack_manipulation={true|false}\n"
				+ "\tobfuscation.compress_output={true|false}\n"
				+ "\tobfuscation.main_class={Binary class name with main(String[]args) method}\n\t\tRequired if obfuscation.compress_output=true\n"
				//+ "\tobfuscation.encrypt_output={true|false}\n"
				+ "\n"
				+ "\toptimization.use_optimization={true|false}\n"
				+ "\toptimization.inline_methods={true|false}\n"
				//+ "\toptimization.flow_analysis_optimizations={true|false}");
				);
		System.exit(1);
	}

	private static void printDefaults(){
		System.out.print(
				"\toutput_directory={input file's parent}\n"
						+ "\trun_output=" + run_output + "\n"
						+ "\n"
						+ "\tobfuscation.use_obfuscation=" + use_obfuscation + "\n"
						+ "\tobfuscation.name_pattern=" + name_pattern + "\n"
						+ "\tobfuscation.name_length=" + name_length + "\n"
						//+ "\tobfuscation.name_overflow_pattern=" + name_overflow_pattern + "\n"
						//+ "\tobfuscation.use_names_linearly=" + use_names_linearly + "\n"
						+ "\tobfuscation.preserve_package_structure=" + preserve_package_structure + "\n"
						+ "\tobfuscation.name_remapping_file={\"obfuscation_map.txt\" within output_directory}" + "\n"
						+ "\tobfuscation.use_stack_manipulation=" + use_stack_manipulation + "\n"
						+ "\tobfuscation.compress_output=" + compress_output + "\n"
						+ "\tobfuscation.main_class=" + main_class + "\n"
						//+ "\tobfuscation.encrypt_output=" + encrypt_output + "\n"
						+ "\n"
						+ "\toptimization.use_optimization=" + use_optimization + "\n"
						+ "\toptimization.inline_methods=" + inline_methods + "\n"
						//+ "\toptimization.flow_analysis_optimizations=" + flow_analysis_optimizations + "\n"
				);
		System.exit(1);
	}

}
