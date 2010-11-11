import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;

import edu.mit.jwi.data.IContentType;

/**
 * Basic implementation of the {@code IDataSource} interface, intended for use
 * with the Wordnet distributions. This particular type of data source is for
 * files on disk, and directly accesses the appropriate byte offset in the file
 * to find requested lines. It is appropriate for Wordnet data files.
 * 
 * @author Markus HAENSE
 */
public class DirectAccessWordnetJarFile<T> extends WordnetJarFile<T> {

	public DirectAccessWordnetJarFile(InputStream is, String name,
			IContentType<T> contentType) throws IOException {
		super(is, name, contentType);
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see edu.mit.wordnet.core.file.IDictionaryFile#getLine(java.lang.String)
	 */
	public String getLine(String key) {
		synchronized (fBuffer) {
			try {
				Integer byteOffset = Integer.parseInt(key);
				if (fBuffer.limit() <= byteOffset)
					return null;
				fBuffer.position(byteOffset);
				String line = getLine(fBuffer);
				return line.startsWith(key) ? line : null;
			} catch (NumberFormatException e) {
				return null;
			}
		}
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see edu.mit.jwi.data.WordnetFile#iterator()
	 */
	public Iterator<String> iterator() {
		return new DirectLineIterator(fBuffer);
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see edu.mit.jwi.data.WordnetFile#iterator(java.lang.String)
	 */
	public Iterator<String> iterator(String key) {
		return new DirectLineIterator(fBuffer, key);
	}

	/**
	 * Used to iterate over lines in a file. It is a look-ahead iterator.
	 */
	public class DirectLineIterator extends LineIterator {

		ByteBuffer fMyBuffer;
		String previous, next;

		public DirectLineIterator(ByteBuffer file) {
			this(file, null);
		}

		public DirectLineIterator(ByteBuffer buffer, String key) {
			super(buffer, key);
		}

		protected void findFirstLine(String key) {
			synchronized (fMyBuffer) {
				try {
					Integer byteOffset = Integer.parseInt(key);
					if (fBuffer.limit() <= byteOffset)
						return;
					fMyBuffer.position(byteOffset);
					next = getLine(fMyBuffer);
				} catch (NumberFormatException e) {
					// Ignore
				}
			}
		}
	}
}