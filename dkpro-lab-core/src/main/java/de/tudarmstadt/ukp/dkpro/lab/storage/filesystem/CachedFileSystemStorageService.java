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

import static de.tudarmstadt.ukp.dkpro.lab.task.Task.DISCRIMINATORS_KEY;
import static de.tudarmstadt.ukp.dkpro.lab.task.TaskContextMetadata.METADATA_KEY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import de.tudarmstadt.ukp.dkpro.lab.storage.StreamReader;
import de.tudarmstadt.ukp.dkpro.lab.storage.StreamWriter;
import de.tudarmstadt.ukp.dkpro.lab.storage.impl.PropertiesAdapter;
import de.tudarmstadt.ukp.dkpro.lab.task.TaskContextMetadata;

/**
 * File system-based storage service which caches task contexts and discriminators.
 *
 * @author Richard Eckart de Castilho
 * @author Erik-Lân Do Dinh
 */
public class CachedFileSystemStorageService
    extends FileSystemStorageService
{
	private Map<String, TaskContextMetadata> contexts;
	private Map<String, Map<String, String>> discriminators;

	public CachedFileSystemStorageService()
	{
		contexts = new HashMap<String, TaskContextMetadata>();
		discriminators = new HashMap<String, Map<String, String>>();

		// If we are using a "recycled" run, we need to have all the context metadata available. To
		// avoid pulling these from the FS every time we need the list, we fetch them once at
		// instantion.
		// If new contexts are added, it is ensured by storeBinary that those are put into the
		// cache.
		for (TaskContextMetadata meta : super.getContexts()) {
			contexts.put(meta.getId(), meta);
		}
	}

	@Override
	public void delete(String aContextId)
	{
		super.delete(aContextId);

		contexts.remove(aContextId);
		discriminators.remove(aContextId);
	}

	@Override
	public List<TaskContextMetadata> getContexts()
	{
		List<TaskContextMetadata> contextList = new ArrayList<TaskContextMetadata>(
		        contexts.values());
		Collections.sort(contextList, new Comparator<TaskContextMetadata>()
		{
			@Override
			public int compare(TaskContextMetadata aO1, TaskContextMetadata aO2)
			{
				return Long.signum(aO2.getEnd() - aO1.getEnd());
			}
		});
		return contextList;
	}

	@Override
	public boolean containsContext(String aContextId)
	{
		return contexts.containsKey(aContextId) || super.containsContext(aContextId);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends StreamReader> T retrieveBinary(String aContextId, String aKey, T aConsumer)
	{
		T consumer = null;
		// Get the consumer from cache if it is a TaskContextMetadata or PropertiesAdapter.
		if (aConsumer instanceof TaskContextMetadata && aKey.equals(METADATA_KEY)) {
			consumer = (T) contexts.get(aContextId);
		}
		else if (aConsumer instanceof PropertiesAdapter && aKey.equals(DISCRIMINATORS_KEY)) {
			Properties props = new Properties();
			Map<String, String> discs = discriminators.get(aContextId);
			if (discs != null) {
				props.putAll(discs);
				((PropertiesAdapter) aConsumer).setProperties(props);
				consumer = aConsumer;
			}
		}

		// If the consumer is not a TaskContextMetadata/PropertiesAdapter or is not cached, retrieve
		// it from file and store it in the cache.
		if (consumer == null) {
			consumer = super.retrieveBinary(aContextId, aKey, aConsumer);
			storeInCache(aContextId, aKey, consumer);
		}

		return consumer;
	}

	@Override
	public void storeBinary(String aContextId, String aKey, StreamWriter aProducer)
	{
		super.storeBinary(aContextId, aKey, aProducer);
		storeInCache(aContextId, aKey, aProducer);
	}

	@Override
	public void copy(String aContextId, String aKey, StorageKey aResolvedKey, AccessMode aMode)
	{
		super.copy(aContextId, aKey, aResolvedKey, aMode);

		if (isStorageFolder(aResolvedKey.contextId, aResolvedKey.key)) {
			if (aResolvedKey.key.equals(METADATA_KEY) && aKey.equals(METADATA_KEY)) {
				contexts.put(aContextId, getContext(aResolvedKey.contextId));
			}
			else if (aResolvedKey.key.equals(DISCRIMINATORS_KEY) && aKey.equals(DISCRIMINATORS_KEY)) {
				discriminators.put(aContextId, getDiscriminators(aResolvedKey.contextId));
			}
		}
	}

	/**
	 * Stores a TaskContextMetadata or the contents of a PropertiesAdapter in cache.
	 *
	 * @param aContextId
	 *            the id of the context to which this aMeta belongs.
	 * @param aKey
	 *            the key of the object.
	 * @param aMeta
	 *            the TaskContextMetadata or the PropertiesAdapter that should be cached.
	 */
	private void storeInCache(String aContextId, String aKey, Object aMeta)
	{
		if (aMeta instanceof TaskContextMetadata && aKey.equals(METADATA_KEY)) {
			contexts.put(aContextId, (TaskContextMetadata) aMeta);
		}
		else if (aMeta instanceof PropertiesAdapter && aKey.equals(DISCRIMINATORS_KEY)) {
			discriminators.put(aContextId, ((PropertiesAdapter) aMeta).getMap());
		}
	}

	private Map<String, String> getDiscriminators(String aContextId)
	{
		return retrieveBinary(aContextId, DISCRIMINATORS_KEY, new PropertiesAdapter()).getMap();
	}
}
