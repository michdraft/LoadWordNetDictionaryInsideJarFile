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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import edu.mit.jwi.data.ContentType;
import edu.mit.jwi.data.DataProviderClosedException;
import edu.mit.jwi.data.DataType;
import edu.mit.jwi.data.IContentType;
import edu.mit.jwi.data.IDataProvider;
import edu.mit.jwi.data.IDataSource;
import edu.mit.jwi.data.IDataType;
import edu.mit.jwi.data.parse.ILineParser;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IVersion;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Synset;

/**
 * A basic implementation of the {@code IDataProvider} interface for Wordnet
 * that uses files in a JAR file to back instances of {@code IDataSource}. It
 * takes a {@code URL} to a JAR directory as its path argument, and uses the
 * hints from the {@link IDataType#getResourceNameHints()} and
 * {@link POS#getResourceNameHints()} interfaces to examine the filenames in the
 * that directory to determine which files contain which data.
 * 
 * @author Markus HAENSE
 */
public class JarFileProvider implements IDataProvider {
	public static final String PROTOCOL_FILE = "jar";

	public static String WORDNET_PATH = "WordNet-3.0/dict/";

	private URL _jUrl = null;
	private IVersion _jVersion = null;
	private Set<IContentType<?>> _jSearchTypes = null;
	private Map<IContentType<?>, IDataSource<?>> _jFileMap = null;
	private Collection<IDataSource<?>> _jSources = null;

	/**
	 * Constructs the file provider pointing to the resource indicated by the
	 * path.
	 * 
	 * @param url
	 *            A file URL in UTF-8 decodable format
	 */
	public JarFileProvider(URL url) {
		this(url, ContentType.values());
	}

	/**
	 * Allows specification of the content types that this file provider should
	 * load in the form of a an array. Duplicate content types will be ignored.
	 */
	public JarFileProvider(URL url, IContentType<?>... types) {
		this(url, Arrays.asList(types));
	}

	/**
	 * Allows specification of the content types that this file provider should
	 * load in the form of a {@code Collection}. Duplicate content types will be
	 * ignored.
	 */
	public JarFileProvider(URL url, Collection<? extends IContentType<?>> types) {
		setSource(url);
		_jSearchTypes = new HashSet<IContentType<?>>(types);
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see edu.mit.jwi.data.IDataProvider#getFile(edu.mit.jwi.content.IContentType)
	 */
	@SuppressWarnings("unchecked")
	// no way to safely cast; must rely on registerSource method to assure
	// compliance
	public <T> IDataSource<T> getSource(IContentType<T> type) {
		checkOpen();
		return (IDataSource<T>) _jFileMap.get(type);
	}

	@Override
	public Collection<IDataSource<?>> getSources() {
		checkOpen();
		if (_jSources == null) {
			_jSources = Collections.unmodifiableCollection(_jFileMap.values());
		}
		return _jSources;
	}

	protected void checkOpen() {
		if (!isOpen()) {
			throw new DataProviderClosedException();
		}
	}

	/**
	 * Translates the source URL into a java {@code File} object for access to
	 * the JAR file. The URL must be in a UTF-8 compatible format as specified
	 * in {@link java.net.URLDecoder}
	 */
	public File getDirectoryHandle() throws URISyntaxException {
		File moduleFile = new File(JarFileProvider.class.getProtectionDomain()
				.getCodeSource().getLocation().toURI());
		return moduleFile;
	}

	/**
	 * @see edu.mit.jwi.data.IDataProvider#open()
	 * @throws IOException
	 *             if the dictionary directory does not exist or the directory
	 *             is empty, or there is a problem with a file
	 */
	@Override
	public void open() throws IOException {
		File directory = null;
		try {
			directory = getDirectoryHandle();

			// Find the WordNet files in JAR file
			List<InputStream> inputStreamList = new ArrayList<InputStream>();
			List<String> nameList = new ArrayList<String>();
			JarFile jarFile = new JarFile(directory);
			Enumeration<JarEntry> enums = jarFile.entries();

			while (enums.hasMoreElements()) {
				JarEntry entry = enums.nextElement();
				if (!entry.isDirectory()
						&& entry.getName().startsWith(WORDNET_PATH)) {

					InputStream is = JarUtil
							.getResourceFileInputStreamInJarFile(
									JarFileProvider.class,
									System.getProperty("file.separator")
											+ entry.getName());

					nameList.add(entry.getName().replaceAll(WORDNET_PATH, ""));
					inputStreamList.add(is);
				}
			}

			if (inputStreamList.size() == 0) {
				throw new IOException("No files found in " + directory);
			}

			_jFileMap = new HashMap<IContentType<?>, IDataSource<?>>();

			for (IContentType<?> type : _jSearchTypes) {
				IDataType<?> fileType = type.getDataType();

				Set<String> typePatterns = fileType.getResourceNameHints();

				Set<String> posPatterns = type.getPOS() != null ? type.getPOS()
						.getResourceNameHints() : Collections
						.<String> emptySet();

				int count = 0;
				for (Iterator<InputStream> i = inputStreamList.iterator(); i
						.hasNext();) {
					InputStream is = i.next();
					String name = nameList.get(count++);
					if (containsOneOf(name, typePatterns)
							& containsOneOf(name, posPatterns)) {
						_jFileMap.put(type, createDataSource(is, name, type));
						break;
					}
				}
			}

		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		_jVersion = determineVersion();
		return;
	}

	/**
	 * Allows subclasses to change the data source implementation.
	 * 
	 * @throws FileNotFoundException
	 *             if the file is not found
	 * @see edu.mit.jwi.data.FileProvider#createDataSource(java.io.File,
	 *      edu.mit.jwi.data.IContentType)
	 */
	protected <T> IDataSource<T> createDataSource(InputStream is, String name,
			IContentType<T> type) throws IOException {

		if (type.getDataType() == DataType.DATA) {
			IDataSource<T> src = new DirectAccessWordnetJarFile<T>(is, name,
					type);

			// check to see if direct access works with the file
			// often people will extract the files incorrectly on windows
			// machines
			// and the binary files will be corrupted with extra CRs

			// get first line
			Iterator<String> itr = src.iterator();
			String firstLine = itr.next();
			if (firstLine == null)
				return src;

			// extract key
			ILineParser<T> parser = type.getDataType().getParser();
			ISynset s = (ISynset) parser.parseLine(firstLine);
			String key = Synset.zeroFillOffset(s.getOffset());

			// try to find line by direct access
			String soughtLine = src.getLine(key);
			if (soughtLine != null)
				return src;

			System.err.println(System.currentTimeMillis()
					+ " - Error on direct access in "
					+ type.getPOS().toString()
					+ " data file: check CR/LF endings");
		}

		return new BinarySearchWordnetJarFile<T>(is, name, type);
	}

	protected IVersion determineVersion() {
		IVersion ver = null;
		for (IDataSource<?> dataSrc : _jFileMap.values()) {

			// if no version to set, ignore
			if (dataSrc.getVersion() == null) {
				continue;
			}

			// init version
			if (ver == null) {
				ver = dataSrc.getVersion();
				continue;
			}

			// if version different from current
			if (!ver.equals(dataSrc.getVersion())) {
				return null;
			}
		}

		return ver;
	}

	/**
	 * Checks to see if one of the string patterns specified in the set of
	 * strings is found in the specified target string. If the pattern set is
	 * empty or null, returns <code>true</code>. If a pattern is found in the
	 * target string, returns <code>true</code>. Otherwise, returns
	 * <code>false</code>.
	 */
	protected boolean containsOneOf(String target, Set<String> patterns) {
		if (patterns == null || patterns.size() == 0) {
			return true;
		}
		for (String pattern : patterns) {
			if (target.indexOf(pattern) > -1) {
				return true;
			}
		}
		return false;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see edu.mit.jwi.item.IHasVersion#getVersion()
	 */
	@Override
	public IVersion getVersion() {
		return _jVersion;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see edu.mit.jwi.data.IDataProvider#isOpen()
	 */
	@Override
	public boolean isOpen() {
		return _jFileMap != null;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see edu.mit.jwi.data.IDataProvider#close()
	 */
	@Override
	public void close() {
		_jFileMap = null;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see edu.mit.jwi.data.IDataProvider#getSource()
	 */
	@Override
	public URL getSource() {
		return _jUrl;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see edu.mit.jwi.data.IDataProvider#setSource(java.net.URL)
	 */
	@Override
	public void setSource(URL url) {
		_jUrl = url;
	}

	public static void main(String[] args) {
		IDataProvider provider = new JarFileProvider(
				JarFileProvider.class.getResource(""));
		try {
			provider.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}