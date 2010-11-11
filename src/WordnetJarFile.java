/********************************************************************************
 * MIT Java Wordnet Interface (JWI)
 * Copyright (c) 2007-2008 Massachusetts Institute of Technology
 *
 * This is the non-commercial version of JWI.  This version may *not* be used
 * for commercial purposes.
 * 
 * This program and the accompanying materials are made available by the MIT
 * Technology Licensing Office under the terms of the MIT Java Wordnet Interface 
 * Non-Commercial License.  The MIT Technology Licensing Office can be reached 
 * at 617-253-6966 for further inquiry.
 *******************************************************************************/

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

import edu.mit.jwi.data.IContentType;
import edu.mit.jwi.data.IDataSource;
import edu.mit.jwi.data.compare.ICommentDetector;
import edu.mit.jwi.item.IVersion;
import edu.mit.jwi.item.Version;

/**
 * Abstract superclass of wordnet data files.
 * 
 * @author Markus HAENSE
 */
public abstract class WordnetJarFile<T> implements IDataSource<T> {

	private final String fName;
	private final IVersion fVersion;
	private final IContentType<T> fContentType;
	private final ICommentDetector fDetector;

	private final static int BUFSIZE = 8096;

	protected final ByteBuffer fBuffer;

	/**
	 * Constructs an instance of this class backed by the specified java
	 * {@code File} object, with the particular content type. No effort is made
	 * to ensure that the data in the specified file is actually formatted in
	 * the proper manner for the line parser associated with the content type's
	 * data type. If these are mismatched, this will result in
	 * {@code MisformattedLineExceptions} in later calls.
	 */
	public WordnetJarFile(InputStream is, String name,
			IContentType<T> contentType) throws IOException {
		fName = name;
		fContentType = contentType;
		fDetector = fContentType.getLineComparator().getCommentDetector();

		ByteArrayOutputStream out = new ByteArrayOutputStream(BUFSIZE);

		byte[] tmp = new byte[BUFSIZE];

		while (true) {
			int r = is.read(tmp);
			if (r == -1)
				break;

			out.write(tmp, 0, r);
		}
		out.close();
		is.close();

		fBuffer = ByteBuffer.wrap(out.toByteArray());
		fVersion = Version.extractVersion(fContentType, fBuffer);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.mit.jwi.data.IDataSource#getVersion()
	 */
	public IVersion getVersion() {
		return fVersion;
	}

	/**
	 * Returns the String from the current position up to, and including, the
	 * next newline
	 */
	public static String getLine(ByteBuffer buf) {
		StringBuilder input = new StringBuilder();
		char c;
		boolean eol = false;
		int limit = buf.limit();

		while (!eol && buf.position() < limit) {
			c = (char) buf.get();
			switch (c) {
			case '\n':
				eol = true;
				break;
			case '\r':
				eol = true;
				int cur = buf.position();
				c = (char) buf.get();
				if (c != '\n') {
					buf.position(cur);
				}
				break;
			default:
				input.append(c);
				break;
			}
		}

		return (buf.position() == limit && input.length() == 0) ? null : input
				.toString();
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see edu.mit.jwi.data.IDataSource#getContentType()
	 */
	public IContentType<T> getContentType() {
		return fContentType;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see edu.mit.jwi.data.IDataSource#getName()
	 */
	public String getName() {
		return fName;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see edu.mit.jwi.data.IDataSource#iterator()
	 */
	public abstract Iterator<String> iterator();

	/**
	 * (non-Javadoc)
	 * 
	 * @see edu.mit.jwi.data.IDataSource#iterator(java.lang.String)
	 */
	public abstract Iterator<String> iterator(String key);

	/**
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + fContentType.hashCode();
		return result;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final WordnetJarFile<?> other = (WordnetJarFile<?>) obj;
		if (!fContentType.equals(other.fContentType))
			return false;
		return true;
	}

	/**
	 * Used to iterate over lines in a file. It is a look-ahead iterator.
	 */
	protected abstract class LineIterator implements Iterator<String> {

		ByteBuffer fMyBuffer;
		String previous, next;

		public LineIterator(ByteBuffer file) {
			this(file, null);
		}

		public LineIterator(ByteBuffer buffer, String key) {
			fMyBuffer = buffer.asReadOnlyBuffer();
			if (key == null) {
				advance();
				return;
			}

			key = key.trim();

			if (key.length() == 0) {
				advance();
			} else {
				findFirstLine(key);
			}
		}

		protected abstract void findFirstLine(String key);

		/**
		 * (non-Javadoc)
		 * 
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext() {
			return next != null;
		}

		/**
		 * Skips over comment lines to find the next line that would be returned
		 * by the iterator in a call to next()
		 */
		protected void advance() {
			next = null;
			String line;
			do {
				line = getLine(fMyBuffer);
			} while (fDetector != null && fDetector.isCommentLine(line));
			next = line;
		}

		/**
		 * (non-Javadoc)
		 * 
		 * @see java.util.Iterator#next()
		 */
		public String next() {
			if (next == null)
				throw new NoSuchElementException();
			previous = next;
			advance();
			return previous;
		}

		/**
		 * (non-Javadoc)
		 * 
		 * @see java.util.Iterator#remove()
		 */
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}