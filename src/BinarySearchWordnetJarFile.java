import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.Iterator;

import edu.mit.jwi.data.IContentType;

/**
 * Basic implementation of the {@code IDataSource} interface, intended for use
 * with the Wordnet distributions. This particular type of data source is for
 * files on disk, and uses a binary search algorithm to find requested lines. It
 * is appropriate for alphabetically-ordered Wordnet files.
 * 
 * @author Markus HAENSE
 */
public class BinarySearchWordnetJarFile<T> extends WordnetJarFile<T> {

	protected final Comparator<String> fComparator;

	public BinarySearchWordnetJarFile(InputStream is, String name,
			IContentType<T> contentType) throws IOException {
		super(is, name, contentType);
		fComparator = getContentType().getLineComparator();
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see edu.mit.wordnet.core.file.IDictionaryFile#getLine(java.lang.String)
	 */
	public String getLine(String key) {
		synchronized (fBuffer) {
			int start = 0;
			int stop = fBuffer.limit();
			int midpoint = (stop + start) / 2;
			int compare;
			String line;
			while (start < midpoint | stop - start > 1) {

				midpoint = (start + stop) / 2;
				fBuffer.position(midpoint);
				line = getLine(fBuffer);
				if (midpoint > 0)
					line = getLine(fBuffer);

				// Fix for Bug 005 ============
				if (line == null || line.length() == 0) {
					// we have reached the last line of the file, so return
					// the last line if it matches
					fBuffer.position(start);
					line = getLine(fBuffer);
					String newline = getLine(fBuffer);
					while (newline != null) {
						line = newline;
						newline = getLine(fBuffer);
					}
					return fComparator.compare(line, key) == 0 ? line : null;
				}
				// =============================
				compare = fComparator.compare(line, key);
				if (compare == 0) {
					return line;
				} else if (compare > 0) {
					stop = midpoint;
				} else {
					start = midpoint;
				}
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.mit.wordnet.core.file.IDictionaryFile#iterator()
	 */
	public Iterator<String> iterator() {
		return new BinarySearchLineIterator(fBuffer);
	}

	/*
	 * (non-Javadoc) @see
	 * edu.mit.jwi.data.IDataSource#iterator(java.lang.String)
	 */
	public Iterator<String> iterator(String key) {
		return new BinarySearchLineIterator(fBuffer, key);
	}

	/**
	 * Used to iterate over lines in a file. It is a look-ahead iterator.
	 */
	public class BinarySearchLineIterator extends LineIterator {

		public BinarySearchLineIterator(ByteBuffer file) {
			this(file, null);
		}

		public BinarySearchLineIterator(ByteBuffer buffer, String key) {
			super(buffer, key);

		}

		protected void findFirstLine(String key) {
			synchronized (fMyBuffer) {
				int lastOffset = -1;
				int start = 0;
				int stop = fMyBuffer.limit();
				int offset, midpoint = -1;
				int compare;
				String line;
				while (start + 1 < stop) {
					midpoint = (start + stop) / 2;
					fMyBuffer.position(midpoint);
					line = getLine(fMyBuffer);
					offset = fMyBuffer.position();
					line = getLine(fMyBuffer);

					// Fix for Bug009: If the line is null, we've reached
					// the end of the file, so just advance to the first line
					if (line == null) {
						fMyBuffer.position(fMyBuffer.limit());
						return;
					}

					compare = fComparator.compare(line, key);
					// if the key matches exactly, we know we have found
					// the start of this pattern in the file
					if (compare == 0) {
						next = line;
						return;
					} else if (compare > 0) {
						stop = midpoint;
					} else {
						start = midpoint;
					}
					// if the key starts a line, remember it, because
					// it may be the first occurrence
					if (line.startsWith(key)) {
						lastOffset = offset;
					}
				}

				// Getting here means that we didn't find an exact match
				// to the key, so we take the last line that started
				// with the pattern
				if (lastOffset > -1) {
					fMyBuffer.position(lastOffset);
					next = getLine(fMyBuffer);
					return;
				}

				// If we didn't have any lines that matched the pattern
				// then just advance to the first non-comment
				fMyBuffer.position(fMyBuffer.limit());
			}
		}
	}
}