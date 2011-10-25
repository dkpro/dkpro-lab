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
package de.tudarmstadt.ukp.dkpro.lab.engine;

import de.tudarmstadt.ukp.dkpro.lab.task.Task;

/**
 * @author Richard Eckart de Castilho
 */
public interface TaskContextFactory
{
	/**
	 * Create a new context for the given task configuration.
	 *
	 * @param aConfiguration a task configuration.
	 * @return the new context.
	 */
	TaskContext createContext(Task aConfiguration);

	/**
	 * Get an existing context. This can return either a context that is currently being processed
	 * or a persisted context with the given id.
	 *
	 * @param aContextId the context id.
	 * @return the context.
	 */
	TaskContext getContext(String aContextId);

	void destroyContext(TaskContext aExperimentContext);

	/**
	 * An ID for this context factory for injection into UIMA components.
	 *
	 * @return the ID.
	 */
	String getId();
}
