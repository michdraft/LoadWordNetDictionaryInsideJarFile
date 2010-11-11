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

import java.net.URL;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.data.IDataProvider;

/**
 * Basic implementation of the {@code IDictionary} interface. A path to the
 * Wordnet dictionary in the JAR files must be provided. If no
 * {@code IDataProvider} is specified, it uses the default implementation
 * provided with the distribution.
 * <p>
 * This dictionary caches items it retrieves. The cache is limited in size by
 * default. See {@link edu.mit.jwi.ItemCache#DEFAULT_MAXIMUM_CAPACITY}. If you
 * find this default maximum size does suit your purposes, you can retrieve the
 * cache via the {@link #getCache()} method and set the maximum cache size via
 * the {@link edu.mit.jwi.ItemCache#setMaximumCapacity(int)} method. If you have
 * a specialized implementation for your cache, you can subclass the
 * {@code Dictionary} class and override the {@link #createCache()} method.
 * 
 * @author Markus HAENSE
 */
public class JarDictionary extends Dictionary {
	public JarDictionary(URL url) {
		super(new JarFileProvider(url));
	}

	public JarDictionary(IDataProvider provider) {
		super(provider);
	}
}