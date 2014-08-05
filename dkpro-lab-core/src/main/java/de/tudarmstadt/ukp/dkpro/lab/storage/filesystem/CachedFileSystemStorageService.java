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

import java.io.File;
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
import de.tudarmstadt.ukp.dkpro.lab.task.Task;
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
	}

	@Override
	public void delete(String aContextId)
	{
		super.delete(aContextId);

		contexts.remove(aContextId);
		discriminators.remove(aContextId);
	}

	@Override
	public TaskContextMetadata getContext(String aContextId)
	{
		return contexts.get(aContextId);
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
	public List<TaskContextMetadata> getContexts(String aTaskType, Map<String, String> aConstraints)
	{
		List<TaskContextMetadata> contextList = new ArrayList<TaskContextMetadata>();

		nextContext: for (TaskContextMetadata meta : getContexts()) {
			// Ignore those that do not match the type
			if (!aTaskType.equals(meta.getType())) {
				continue;
			}

			// Check the constraints if there are any
			if (aConstraints.size() > 0) {
				Map<String, String> properties = getDiscriminators(meta.getId());
				if (!matchConstraints(properties, aConstraints, true)) {
					continue nextContext;
				}
			}
			contextList.add(meta);
		}

		return contextList;
	}

	@Override
	public boolean containsContext(String aContextId)
	{
		return contexts.containsKey(aContextId);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends StreamReader> T retrieveBinary(String aContextId, String aKey, T aConsumer)
	{
		if (aConsumer instanceof TaskContextMetadata
		        && aKey.equals(TaskContextMetadata.METADATA_KEY)) {
			return (T) getContext(aContextId);
		}
		else if (aConsumer instanceof PropertiesAdapter && aKey.equals(Task.DISCRIMINATORS_KEY)) {
			Properties props = new Properties();
			props.putAll(getDiscriminators(aContextId));
			((PropertiesAdapter) aConsumer).setProperties(props);
			return aConsumer;
		}

		return super.retrieveBinary(aContextId, aKey, aConsumer);
	}

	@Override
	public void storeBinary(String aContextId, String aKey, StreamWriter aProducer)
	{
		super.storeBinary(aContextId, aKey, aProducer);

		if (aProducer instanceof TaskContextMetadata
		        && aKey.equals(TaskContextMetadata.METADATA_KEY)) {
			contexts.put(aContextId, (TaskContextMetadata) aProducer);
		}
		else if (aProducer instanceof PropertiesAdapter && aKey.equals(Task.DISCRIMINATORS_KEY)) {
			discriminators.put(aContextId, ((PropertiesAdapter) aProducer).getMap());
		}
	}

	@Override
	public void copy(String aContextId, String aKey, StorageKey aResolvedKey, AccessMode aMode)
	{
		super.copy(aContextId, aKey, aResolvedKey, aMode);

		if (isStorageFolder(aResolvedKey.contextId, aResolvedKey.key)) {
			if (aResolvedKey.key.equals(TaskContextMetadata.METADATA_KEY)
			        && aKey.equals(TaskContextMetadata.METADATA_KEY)) {
				contexts.put(aContextId, getContext(aResolvedKey.contextId));
			}
			else if (aResolvedKey.key.equals(Task.DISCRIMINATORS_KEY)
			        && aKey.equals(Task.DISCRIMINATORS_KEY)) {
				discriminators.put(aContextId, getDiscriminators(aResolvedKey.contextId));
			}
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

	private boolean isStorageFolder(String aContextId, String aKey)
	{
		return new File(getContextFolder(aContextId, false), aKey).isDirectory();
	}

	private Map<String, String> getDiscriminators(String aContextId)
	{
		return discriminators.get(aContextId);
	}
}
