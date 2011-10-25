/*******************************************************************************
 * Copyright 2011
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *   
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *   
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.dkpro.lab;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.springframework.dao.DataAccessResourceFailureException;

import de.tudarmstadt.ukp.dkpro.lab.storage.StreamReader;
import de.tudarmstadt.ukp.dkpro.lab.task.Discriminable;

public class Util
{
	private static Map<URL, File> urlFileCache;

	static {
		urlFileCache = new HashMap<URL, File>();
	}

	/**
	 * Make the given URL available as a file. A temporary file is created and
	 * deleted upon a regular shutdown of the JVM. If the parameter {@code
	 * aCache} is {@code true}, the temporary file is remembered in a cache and
	 * if a file is requested for the same URL at a later time, the same file is
	 * returned again. If the previously created file has been deleted
	 * meanwhile, it is recreated from the URL.
	 *
	 * @param aUrl
	 *            the URL.
	 * @param aCache
	 *            use the cache or not.
	 * @return a file created from the given URL.
	 * @throws IOException
	 *             if the URL cannot be accessed to (re)create the file.
	 */
	public static synchronized File getUrlAsFile(URL aUrl, boolean aCache)
		throws IOException
	{
		// If the URL already points to a file, there is not really much to do.
		if ("file".equals(aUrl.getProtocol())) {
			return new File(aUrl.getPath());
		}

		// Lets see if we already have a file for this URL in our cache. Maybe
		// the file has been deleted meanwhile, so we also check if the file
		// actually still exists on disk.
		File file = urlFileCache.get(aUrl);
		if (!aCache || (file == null) || !file.exists()) {
			// Create a temporary file and try to preserve the file extension
			String suffix = ".temp";
			String name = new File(aUrl.getPath()).getName();
			int suffixSep = name.indexOf(".");
			if (suffixSep != -1) {
				suffix = name.substring(suffixSep + 1);
				name = name.substring(0, suffixSep);
			}

			// Get a temporary file which will be deleted when the JVM shuts
			// down.
			file = File.createTempFile(name, suffix);
			file.deleteOnExit();

			// Now copy the file from the URL to the file.
			shoveAndClose(aUrl.openStream(), new FileOutputStream(file));

			// Remember the file
			if (aCache) {
				urlFileCache.put(aUrl, file);
			}
		}

		return file;
	}

	/**
	 * Makes the given stream available as a file. The created file is temporary
	 * and deleted upon normal termination of the JVM. Still the file should be
	 * deleted as soon as possible if it is no longer required. In case the JVM
	 * crashes the file would not be deleted. The source stream is closed by
	 * this operation in all cases.
	 *
	 * @param is
	 *            the source.
	 * @return the file.
	 * @throws IOException
	 *             in case of read or write problems.
	 */
	public static File getStreamAsFile(final InputStream is)
		throws IOException
	{
		OutputStream os = null;
		try {
			final File f = File.createTempFile("lab_stream", "tmp");
			f.deleteOnExit();
			os = new FileOutputStream(f);
			shove(is, os);
			return f;
		}
		finally {
			close(os);
			close(is);
		}
	}

	/**
	 * Shove data from an {@link InputStream} into an {@link OutputStream}.
	 * Neither of the streams are closed after the operation.
	 *
	 * @param is the source.
	 * @param os the target.
	 * @throws IOException in case of read or write problems.
	 */
	public static void shove(final InputStream is, final OutputStream os)
		throws IOException
	{
		byte[] buffer = new byte[65536];
		int read;
		while (true) {
			read = is.read(buffer);
			if (read == -1) {
				break;
			}
			os.write(buffer, 0, read);
		}
		os.flush();
	}

	/**
	 * As {@link #shove(InputStream, OutputStream)} but the streams are closed
	 * at the end of the process.
	 *
	 * @param is the input stream.
	 * @param os the output stream.
	 * @throws IOException in case of read or write problems.
	 */
	public static
	void shoveAndClose(
			final InputStream is,
			final OutputStream os)
	throws IOException
	{
		try {
			shove(is, os);
		}
		finally {
			close(is);
			close(os);
		}
	}

	/**
	 * Close a {@link Closeable} object. This method is best used in {@code finally}
	 * sections.
	 *
	 * @param object the object to close.
	 */
	public static void close(final Closeable object)
	{
		if (object == null) {
			return;
		}

		try {
			object.close();
		}
		catch (IOException e) {
			// Ignore exceptions happening while closing.
		}
	}

	public static void delete(final File file)
	{
		if (file.isDirectory()) {
			if (file.exists()) {
				for (final File f : file.listFiles()) {
					delete(f);
				}
			}
		}
		file.delete();
	}

	/**
	 * Recursively copy files and directories. The target must not exist before this operation.
	 *
	 * @param aIn the source.
	 * @param aOut the target.
	 * @throws IOException if something goes wrong.
	 */
	public static void copy(File aIn, File aOut)
		throws IOException
	{
		if (aOut.exists()) {
			throw new IOException("Target ["+aOut+"] already exists");
		}

		if (aIn.isDirectory()) {
			aOut.mkdirs();
			for (File child : aIn.listFiles()) {
				copy(child, new File(aOut, child.getName()));
			}
		}
		else {
			copyFile(aIn, aOut);
		}
	}

	public static void copyFile(final File aIn, final File aOut)
		throws IOException
	{
		FileChannel inChannel = null;
		FileChannel outChannel = null;
		try {
			inChannel = new FileInputStream(aIn).getChannel();
			outChannel = new FileOutputStream(aOut).getChannel();
			inChannel.transferTo(0, inChannel.size(), outChannel);
		}
		finally {
			close(inChannel);
			close(outChannel);
		}
	}

	public static String toString(final Object aObject)
	{
		if (aObject == null) {
			return "null";
		}

		if (aObject.getClass().isArray()) {
			return new ToStringBuilder(aObject, LAB_STYLE).append(aObject).toString();
		}
		else if (aObject instanceof Discriminable) {
			return toString(((Discriminable) aObject).getDiscriminatorValue());
		}
		else {
			return String.valueOf(aObject);
		}
	}

	public static <T extends StreamReader> T retrieveBinary(final File aFile, final T aConsumer)
	{
		InputStream is = null;
		try {
			is = new FileInputStream(aFile);
			if (aFile.getName().toLowerCase().endsWith(".gz")) {
				is = new GZIPInputStream(is);
			}
			aConsumer.read(is);
			return aConsumer;
		}
		catch (IOException e) {
			throw new DataAccessResourceFailureException(e.getMessage(), e);
		}
		finally {
			Util.close(is);
		}
	}

	public static final ToStringStyle LAB_STYLE = new LabToStringStyle();

	/**
     * <p>Have to use a custom style here since Apache Commons Lang uses curly braces for arrays
     * and we traditionally use square brackets as is used in Java Collections toString() methods.
     * </p>
     */
    private static final class LabToStringStyle extends ToStringStyle {

        private static final long serialVersionUID = 1L;

        private LabToStringStyle() {
            super();
            setUseClassName(false);
            setUseIdentityHashCode(false);
            setUseFieldNames(false);
            setContentStart("");
            setContentEnd("");
            setArrayStart("[");
            setArrayEnd("]");
            setArraySeparator(", ");
        }

        private Object readResolve() {
            return Util.LAB_STYLE;
        }
    }
}
