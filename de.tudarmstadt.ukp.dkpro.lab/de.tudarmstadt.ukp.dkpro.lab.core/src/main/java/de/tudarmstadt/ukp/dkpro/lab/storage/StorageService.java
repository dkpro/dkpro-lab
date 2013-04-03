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
package de.tudarmstadt.ukp.dkpro.lab.storage;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import de.tudarmstadt.ukp.dkpro.lab.reporting.Report;
import de.tudarmstadt.ukp.dkpro.lab.task.Task;
import de.tudarmstadt.ukp.dkpro.lab.task.TaskContextMetadata;

/**
 * Task context storage API. Data generated during the execution of a {@link Task} is persisted using
 * the storage API and can be accessed easily by following tasks or {@link Report}s.
 *
 * @author Richard Eckart de Castilho
 * @since 0.2.0
 */
public interface StorageService
{
	/**
	 * @since 0.2.0
	 */
	public static enum AccessMode
	{
		/**
		 * @since 0.2.0
		 */
		READONLY,

		/**
		 * @since 0.2.0
		 */
		READWRITE,

		/**
		 * Only add new files, but do not modify existing files.
		 * 
		 * @since 0.8.0
		 */
		ADD_ONLY
	}

	/**
	 * @since 0.2.0
	 */
	public static final String CONTEXT_ID_SCHEME = "task-id";

	/**
	 * @since 0.2.0
	 */
	public static final String LATEST_CONTEXT_SCHEME = "task-latest";

	/**
	 * Check if the context with the given ID is available.
	 *
	 * @param aContextId a context ID.
	 * @return if the context is available.
	 * @since 0.2.0
	 */
	boolean containsContext(String aContextId);

	/**
	 * Check if the context with the given ID provides the given storage key.
	 *
	 * @param aContextId a context ID.
	 * @param aKey a storage key.
	 * @return if the context provides the key.
	 * @since 0.2.0
	 */
	boolean containsKey(String aContextId, String aKey);

	/**
	 * Get metadata associated with the given context ID.
	 *
	 * @param aContextId a context ID.
	 * @return the context metadata.
	 * @since 0.2.0
	 */
	TaskContextMetadata getContext(String aContextId);

	/**
	 * Get the task metadata for the last execution of the specified task type. Optionally further
	 * contstraints in addition to the type are possible. A single contstraint is represented by a
	 * pair of regular expressions, the first of which matches against a discriminator key, the
	 * second against a discriminator value. The key can be written in a short form or in a long
	 * form containing the full class name of the task contributing the discriminator followed by
	 * a pipe symbol (<code>|</code>) followed by the discriminator name. If the short form is used,
	 * the contstraint will match against any discriminator with the specified name, regardless of
	 * which task is contributing it.
	 *
	 * @param aTaskType the task type.
	 * @param aConstraints a set of further constraints in addition to the type.
	 * @return the metadata.
	 * @since 0.3.0
	 * @throws TaskContextNotFoundException if a matching task context could not be found.
	 */
	TaskContextMetadata getLatestContext(String aTaskType, Map<String, String> aConstraints);

	/**
	 * Get a list of all persisted task contexts. The list is sorted with the most recent contexts
	 * first and the oldest ones last.
	 *
	 * @return a list of all persisted task contexts.
	 * @since 0.2.0
	 */
	List<TaskContextMetadata> getContexts();

	/**
	 * Get all executions of the given type matching the given constraints sorted in chronological
	 * order. The list is sorted with the most recent contexts first and the oldest ones last.
	 */
	List<TaskContextMetadata> getContexts(String aTaskType, Map<String, String> aConstraints);

	/**
	 * @since 0.2.0
	 */
	void delete(String aContextId);

	/**
	 * @since 0.2.0
	 */
	void delete(String aContextId, String aKey);

	void copy(String aTargetContextId, String aTargetKey, StorageKey aSourceKey, AccessMode aMode);

	/**
	 * Sometimes data cannot be conveniently stored via a stream, e.g. when using Lucene, it
	 * has to be provided with a location where it can store its data. This method allows to get
	 * a storage location identified by a storage key within the specified context.
	 *
	 * @param aContextId a context ID.
	 * @param aKey a storage key.
	 * @return a location.
	 * @since 0.2.0
	 */
	File getStorageFolder(String aContextId, String aKey);

	/**
	 * Read a binary stream. If the path ends in ".gz" the stream is uncompressed upon reading.
	 */
	<T extends StreamReader> T retrieveBinary(String aContextId, String aKey, T aConsumer);

	/**
	 * Store all data available from the given stream into the storage. The
	 * stream is closed afterwards, even in case an exception is thrown.
	 * If the key ends in ".gz" the stream is stored compressed.
	 * @since 0.2.0
	 */
	void storeBinary(String aContextId, String aKey, InputStream aStream);

	/**
	 * Store all data available from the given stream producer into the storage.
	 * The stream is closed afterwards, even in case an exception is thrown. If
	 * the key ends in ".gz" the stream is stored compressed.
	 * @since 0.2.0
	 */
	void storeBinary(String aContextId, String aKey, StreamWriter aStreamProducer);

	public static class StorageKey {
		public String contextId;
		public String key;

		public StorageKey(
				String aContextId,
				String aKey)
		{
			contextId = aContextId;
			key = aKey;
		}
	}
}
