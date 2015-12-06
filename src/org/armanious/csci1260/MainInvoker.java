package org.armanious.csci1260;

import java.io.File;

public class MainInvoker {

	private final File parentDirectory;
	private final File jarFile;
	private final boolean isJar;
	private final String mainClassName;
	//private Method mainToInvoke;

	public MainInvoker(File directoryOrJar, String mainClassInternalName) {
		isJar = directoryOrJar.getName().endsWith(".jar");
		parentDirectory = isJar ? directoryOrJar.getParentFile() : directoryOrJar;
		jarFile = isJar ? directoryOrJar : null;
		
		mainClassName = mainClassInternalName.replace('/', '.');
		/*
		if(mainClassInternalName != null){
			String s = mainClassInternalName.replace('/', '.');
			try{
				final Class<?> clazz = super.loadClass(s);
				mainToInvoke = clazz.getDeclaredMethod("main", new Class[]{String[].class});
			}catch(Throwable t){
				mainClassInternalName = null;
			}
		}
		if(mainClassInternalName == null){
			if(parentDirectory.isDirectory()){
				final Stack<File> stack = new Stack<>();
				stack.push(parentDirectory);
				while(!stack.isEmpty()){
					final File cur = stack.pop();
					if(cur.isDirectory()){
						for(File child : cur.listFiles()){
							stack.push(child);
						}
					}else{
						if(cur.getName().endsWith(".class")){
							String className = cur.getPath().substring(parentDirectory.toString().length() + 1);
							className = className.substring(0, className.length() - 6);
							try {
								Class<?> clazz = super.loadClass(className.replace('/', '.'));
								Method t = clazz.getDeclaredMethod("main", new Class[]{String[].class});
								if(t != null && Modifier.isStatic(t.getModifiers()) && 
										Modifier.isPublic(t.getModifiers()) && mainToInvoke == null){
									mainToInvoke = t;
								}
							}catch(NoSuchMethodException ignored){
							}catch(Error | Exception e){
								if(e instanceof InvocationTargetException){
									throw e.getCause();
								}
								e.printStackTrace();
								System.out.println(cur);
								print(cur);
								mainToInvoke = null;
								break;
							}
						}
					}
				}
			}else if(parentDirectory.getName().endsWith(".jar")){
				try(final JarFile jar = new JarFile(parentDirectory)){
					final Enumeration<JarEntry> entries = jar.entries();
					while(entries.hasMoreElements()){
						final JarEntry cur = entries.nextElement();
						if(cur.getName().endsWith(".class")){
							String className = cur.getName().substring(0, cur.getName().length() - 6);
							try {
								Class<?> clazz = super.loadClass(className.replace('/', '.'));
								Method t = clazz.getDeclaredMethod("main", new Class[]{String[].class});
								if(t != null && Modifier.isStatic(t.getModifiers()) && 
										Modifier.isPublic(t.getModifiers())){
									mainToInvoke = t;
									break;
								}
							}catch(NoSuchMethodException ignored){
							}catch(Error | Exception e){
								if(e instanceof InvocationTargetException){
									throw e.getCause();
								}
								e.printStackTrace();
								System.out.println(cur);
								print(jar.getInputStream(cur));
								mainToInvoke = null;
								break;
							}
						}
					}
				}
			}
		}*/
	}
	
	public boolean runMain() {
		try {
			ProcessBuilder builder = new ProcessBuilder().inheritIO().directory(parentDirectory);
			if(isJar){
				builder.command("java", "-jar", jarFile.getName());
			}else{
				builder.command("java", mainClassName);
			}
			//mainToInvoke.invoke(null, new Object[]{new String[0]});
			builder.start().waitFor();
			return true;
		} catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}

}
