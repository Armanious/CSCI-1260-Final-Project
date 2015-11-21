import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream.Filter;
import java.util.Stack;

public class Other {

	public static void main(String...args) throws IOException {
		System.out.println(
				countNumLines(new File(new File(System.getProperty("user.dir")).getParentFile(), "CSCI 1260 Final Project" + File.separator + "src"),
						(f)->!f.getName().contains("objectweb") && (f.isDirectory() || f.getName().endsWith(".java"))));
	}

	private static int countNumLines(File root, Filter<File> filter) throws IOException {
		int sum = 0;
		final Stack<File> stack = new Stack<>();
		stack.push(root);
		while(!stack.isEmpty()){
			final File file = stack.pop();
			if(filter == null || filter.accept(file)){
				if(file.isDirectory()){
					for(File child : file.listFiles()){
						stack.push(child);
					}
				}else{
					try(final BufferedReader br = new BufferedReader(new FileReader(file))){
						System.out.println(file);
						while(br.readLine() != null) sum++;
					}
				}
			}
		}
		return sum;
	}

}
