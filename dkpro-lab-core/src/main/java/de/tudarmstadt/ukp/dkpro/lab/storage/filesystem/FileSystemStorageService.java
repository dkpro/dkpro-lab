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
package de.tudarmstadt.ukp.dkpro.lab.storage.filesystem;

import static de.tudarmstadt.ukp.dkpro.lab.engine.impl.ImportUtil.matchConstraints;
import static de.tudarmstadt.ukp.dkpro.lab.task.TaskContextMetadata.METADATA_KEY;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessResourceFailureException;

import de.tudarmstadt.ukp.dkpro.lab.Util;
import de.tudarmstadt.ukp.dkpro.lab.engine.impl.ImportUtil;
import de.tudarmstadt.ukp.dkpro.lab.storage.StorageService;
import de.tudarmstadt.ukp.dkpro.lab.storage.StreamReader;
import de.tudarmstadt.ukp.dkpro.lab.storage.StreamWriter;
import de.tudarmstadt.ukp.dkpro.lab.storage.impl.PropertiesAdapter;
import de.tudarmstadt.ukp.dkpro.lab.task.Task;
import de.tudarmstadt.ukp.dkpro.lab.task.TaskContextMetadata;

/**
 * Simple but effective file system-based storage service.
 *
 * @author Richard Eckart de Castilho
 */
public class FileSystemStorageService
    implements StorageService
{
	private final Log log = LogFactory.getLog(getClass());

	private static final int MAX_RETRIES = 100;
	private static final long SLEEP_TIME = 1000;

	private File storageRoot;

	public void setStorageRoot(File aStorageRoot)
	{
		storageRoot = aStorageRoot;
	}

	public File getStorageRoot()
	{
		return storageRoot;
	}

	@Override
	public void delete(String aContextId)
	{
		try {
			FileUtils.deleteDirectory(getContextFolder(aContextId, false));
		}
		catch (IOException e) {
			throw new DataAccessResourceFailureException(e.getMessage(), e);
		}
	}

	@Override
	public void delete(String aContextId, String aKey)
	{
		try {
			FileUtils.deleteDirectory(new File(getContextFolder(aContextId, false), aKey));
		}
		catch (IOException e) {
			throw new DataAccessResourceFailureException(e.getMessage(), e);
		}
	}

	@Override
	public TaskContextMetadata getContext(String aContextId)
	{
		return retrieveBinary(aContextId, METADATA_KEY, new TaskContextMetadata());
	}

	@Override
	public List<TaskContextMetadata> getContexts()
	{
		List<TaskContextMetadata> contexts = new ArrayList<TaskContextMetadata>();
		for (File child : storageRoot.listFiles()) {
			if (new File(child, METADATA_KEY).exists()) {
				contexts.add(retrieveBinary(child.getName(), METADATA_KEY,
				        new TaskContextMetadata()));
			}
		}

		Collections.sort(contexts, new Comparator<TaskContextMetadata>()
		{
			@Override
			public int compare(TaskContextMetadata aO1, TaskContextMetadata aO2)
			{
				return Long.signum(aO2.getEnd() - aO1.getEnd());
			}
		});

		return contexts;
	}

	@Override
	public List<TaskContextMetadata> getContexts(String aTaskType, Map<String, String> aConstraints)
	{
		List<TaskContextMetadata> contexts = new ArrayList<TaskContextMetadata>();

		nextContext: for (TaskContextMetadata e : getContexts()) {
			// Ignore those that do not match the type
			if (!aTaskType.equals(e.getType())) {
				continue;
			}

			// Check the constraints if there are any
			if (aConstraints.size() > 0) {
				final Map<String, String> properties = retrieveBinary(e.getId(),
				        Task.DISCRIMINATORS_KEY, new PropertiesAdapter()).getMap();

				if (!matchConstraints(properties, aConstraints, true)) {
					continue nextContext;
				}
			}

			contexts.add(e);
		}

		Collections.sort(contexts, new Comparator<TaskContextMetadata>()
		{
			@Override
			public int compare(TaskContextMetadata aO1, TaskContextMetadata aO2)
			{
				return Long.signum(aO2.getEnd() - aO1.getEnd());
			}
		});

		return contexts;
	}

	@Override
	public TaskContextMetadata getLatestContext(String aTaskType, Map<String, String> aConstraints)
	{
		List<TaskContextMetadata> contexts = getContexts(aTaskType, aConstraints);

		if (contexts.size() == 0) {
			throw ImportUtil.createContextNotFoundException(aTaskType, aConstraints);
		}

		return contexts.get(0);
	}

	@Override
	public boolean containsContext(String aContextId)
	{
		return getContextFolder(aContextId, false).isDirectory();
	}

	@Override
	public boolean containsKey(String aContextId, String aKey)
	{
		return new File(getContextFolder(aContextId, false), aKey).exists();
	}

	@Override
	public <T extends StreamReader> T retrieveBinary(String aContextId, String aKey, T aConsumer)
	{
		InputStream is = null;
		int currentTry = 1;
		IOException lastException = null;

		while (currentTry <= MAX_RETRIES) {
			try {
				is = new FileInputStream(new File(getContextFolder(aContextId, true), aKey));
				if (aKey.endsWith(".gz")) {
					is = new GZIPInputStream(is);
				}
				aConsumer.read(is);
				return aConsumer;
			}
			catch (IOException e) {
				// https://code.google.com/p/dkpro-lab/issues/detail?id=64
				// may be related to a concurrent access so try again after some time
				lastException = e;

				currentTry++;
				log.debug(currentTry + ". try accessing " + aKey + " in context " + aContextId);

				try {
					Thread.sleep(SLEEP_TIME);
				}
				catch (InterruptedException e1) {
					// we should probably abort the whole thing
					currentTry = MAX_RETRIES;
				}
			}
			catch (Throwable e) {
				throw new DataAccessResourceFailureException("Unable to load [" + aKey
				        + "] from context [" + aContextId + "]", e);
			}
			finally {
				Util.close(is);
			}
		}

		throw new DataAccessResourceFailureException("Unable to access [" + aKey + "] in context ["
		        + aContextId + "]", lastException);
	}

	@Override
	public void storeBinary(String aContextId, String aKey, StreamWriter aProducer)
	{
		File context = getContextFolder(aContextId, false);
		File tmpFile = new File(context, aKey + ".tmp");
		File finalFile = new File(context, aKey);

		OutputStream os = null;
		try {
			tmpFile.getParentFile().mkdirs(); // Necessary if the key addresses a sub-directory
			log.debug("Storing to: " + finalFile);
			os = new FileOutputStream(tmpFile);
			if (aKey.endsWith(".gz")) {
				os = new GZIPOutputStream(os);
			}
			aProducer.write(os);
		}
		catch (Exception e) {
			tmpFile.delete();
			throw new DataAccessResourceFailureException(e.getMessage(), e);
		}
		finally {
			Util.close(os);
		}

		// On some platforms, it is not possible to rename a file to another one which already
		// exists. So try to delete the target file before renaming.
		if (finalFile.exists()) {
			boolean deleteSuccess = finalFile.delete();
			if (!deleteSuccess) {
				throw new DataAccessResourceFailureException("Unable to delete [" + finalFile
				        + "] in order to replace it with an updated version.");
			}
		}

		// Make sure the file is only visible under the final name after all data has been
		// written into it.
		boolean renameSuccess = tmpFile.renameTo(finalFile);
		if (!renameSuccess) {
			throw new DataAccessResourceFailureException("Unable to rename [" + tmpFile + "] to ["
			        + finalFile + "]");
		}
	}

	@Override
	public void storeBinary(String aContextId, String aKey, final InputStream aStream)
	{
		try {
			storeBinary(aContextId, aKey, new StreamWriter()
			{
				@Override
				public void write(OutputStream aOs)
				    throws Exception
				{
					Util.shoveAndClose(aStream, aOs);
				}
			});
		}
		finally {
			Util.close(aStream);
		}
	}

    @Override
    public File locateKey(String aContextId, String aKey)
    {
        return new File(getContextFolder(aContextId, false), aKey);
    }
	
	@Override
	public File getStorageFolder(String aContextId, String aKey)
	{
		File folder = new File(getContextFolder(aContextId, false), aKey);
		folder.mkdirs();
		return folder;
	}

	public static boolean isStaticImport(URI uri)
	{
		if (LATEST_CONTEXT_SCHEME.equals(uri.getScheme())) {
			return false;
		}
		else if (CONTEXT_ID_SCHEME.equals(uri.getScheme())) {
			return false;
		}
		else {
			return true;
		}
	}

	@Override
	public void copy(String aContextId, String aKey, StorageKey aResolvedKey, AccessMode aMode)
	{
		StorageKey key = aResolvedKey;
		// If the resource is imported from another context and will be modified and it is a
		// folder, we have to copy it to the current context now, since we cannot do a
		// copy-on-write strategy as for streams.
		if (isStorageFolder(key.contextId, key.key)
		        && (aMode == AccessMode.READWRITE || aMode == AccessMode.ADD_ONLY)) {
			try {
				File source = new File(getContextFolder(key.contextId, false), key.key);
				File target = new File(getContextFolder(aContextId, false), aKey);

				if (Util.isSymlinkSupported() && aMode == AccessMode.ADD_ONLY) {
					log.info("Write access to imported storage folder [" + aKey
					        + "] was requested. Linking to current context");
					Util.copy(source, target, true);
				}
				else {
					log.info("Write access to imported storage folder [" + aKey
					        + "] was requested. Copying to current context");
					Util.copy(source, target, false);
				}
			}
			catch (IOException e) {
				throw new DataAccessResourceFailureException(e.getMessage(), e);
			}

			// Key should point to the local context now
			key = new StorageKey(aContextId, aKey);
		}
	}

	private File getContextFolder(String aContextId, boolean create)
	{
		File folder = new File(getStorageRoot(), aContextId);
		if (create) {
			folder.mkdirs();
		}
		return folder;
	}

	protected boolean isStorageFolder(String aContextId, String aKey)
	{
		return new File(getContextFolder(aContextId, false), aKey).isDirectory();
	}
}
