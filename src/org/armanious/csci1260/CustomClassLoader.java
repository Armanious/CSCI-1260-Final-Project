package org.armanious.csci1260;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Stack;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

public class CustomClassLoader extends URLClassLoader {

	private final File parentDirectory;

	private Method mainToInvoke;

	public CustomClassLoader(File directoryOrJar, String mainClassInternalName) throws Throwable {

		super(new URL[]{directoryOrJar.toURI().toURL()});
		parentDirectory = directoryOrJar;

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
		}
	}

	private static void print(File file){
		try {
			print(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private static void print(InputStream in){
		try {
			new ClassReader(new BufferedInputStream(in)).accept(new TraceClassVisitor(new PrintWriter(System.out)), 0);
		} catch (IOException e2) {
			e2.printStackTrace();
		}
	}
	
	private sun.misc.Unsafe getUnsafe(){
		try {
			final Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			return (sun.misc.Unsafe) field.get(null);
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
		}
		return null;
	}


	public boolean runMain() {
		if(mainToInvoke != null){
			try {
				mainToInvoke.invoke(null, new Object[]{new String[0]});
				return true;
			} catch(InvocationTargetException ite){
//				print(new File(parentDirectory, ite.getCause().getStackTrace()[0].getClassName().replace('.', File.separatorChar).concat(".class")));
				ite.getCause().printStackTrace();
			} catch (ReflectiveOperationException e){
				e.printStackTrace();
			} 
		}
		return false;
	}

}
