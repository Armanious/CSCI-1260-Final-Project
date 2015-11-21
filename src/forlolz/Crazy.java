package forlolz;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Crazy extends ClassLoader {

	public static void main(String[] args) throws IOException, ReflectiveOperationException {
		InputStream resource = ClassLoader.getSystemResourceAsStream("dontlookatme");
		if(resource == null){
			System.err.println("Can't find dontlookatme: " + ClassLoader.getSystemClassLoader().getClass());
			System.exit(1);
		}
		Crazy wild = new Crazy(new ZipInputStream(new BufferedInputStream(resource)));
		Class<?> clazz = wild.loadClass("REPLACE ME WITH NAME OF MAIN CLASS");
		Method main = clazz.getMethod("main", new Class[]{String[].class});
		main.invoke(null, new Object[]{args});
		/*new Crazy(new ZipInputStream(new BufferedInputStream(ClassLoader.getSystemResourceAsStream("/dontlookatme"))))
		.loadClass("REPLACE ME WITH NAME OF MAIN CLASS")
		.getMethod("main", new Class[]{String[].class})
		.invoke(null, new Object[]{new String[]{}});*/
	}
	
	private final HashMap<String, byte[]> map;

	private Crazy(ZipInputStream zis) throws IOException {
		map = new HashMap<>();
		ZipEntry entry;
		while((entry = zis.getNextEntry()) != null){
			final BufferedInputStream br = new BufferedInputStream(zis);
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final byte[] buf = new byte[4096];
			int read;
			while((read = br.read(buf, 0, 4096)) != -1){
				baos.write(buf, 0, read);
			}
			map.put(entry.getName(), baos.toByteArray());
		}
		zis.close();
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		byte[] data;
		if((data = map.get(name)) != null){
			return defineClass(name, data, 0, data.length);
		}
		return super.findClass(name);
	}

}
