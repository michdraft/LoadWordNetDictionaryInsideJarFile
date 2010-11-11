
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Class gathering utilities related to JAR files.
 * 
 * @author Markus HAENSE
 */
public class JarUtil {
	public static <T> String readTextFromResourceFileInJarFile(Class<T> type,
			String path, boolean skipComments) throws IOException {
		// Reading resources of JAR file
		InputStream is = type.getResourceAsStream(path);

		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		StringBuilder text = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			if (line.startsWith("#")) {
				continue;
			} else {
				text.append(line + System.getProperty("line.separator"));
			}
		}
		br.close();
		isr.close();
		is.close();

		return text.toString();
	}

	public static <T> InputStream getResourceFileInputStreamInJarFile(
			Class<T> type, String path) {
		return type.getResourceAsStream(path);
	}

	public static List<File> listFilesInJarFile(File directory)
			throws IOException {
		List<File> files = new ArrayList<File>();
		JarFile jarFile = new JarFile(directory);
		Enumeration<JarEntry> enums = jarFile.entries();
		while (enums.hasMoreElements()) {
			JarEntry entry = enums.nextElement();
			try {
				File file = new File(JarFileProvider.class.getResource(
						System.getProperty("file.separator") + entry.getName())
						.toURI());
				files.add(file);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}

		return files;
	}
}