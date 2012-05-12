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
 * Manages the task life-cycle. This represents a strategy pattern.
 *
 * @author Richard Eckart de Castilho
 */
public interface LifeCycleManager
{
	/**
	 * Called by an {@link TaskExecutionEngine} when the task is initialized.
	 *
	 * @param aContext the task context.
	 * @param aConfiguration the task configuration.
	 * @throws LifeCycleException if something goes wrong.
	 */
	void initialize(TaskContext aContext, Task aConfiguration)
		throws LifeCycleException;

	/**
	 * Called by an {@link TaskExecutionEngine} just before the main execution step of a task.
	 *
	 * @param aContext the task context.
	 * @param aConfiguration the task configuration.
	 * @throws LifeCycleException if something goes wrong.
	 */
	void begin(TaskContext aContext, Task aConfiguration)
		throws LifeCycleException;

	/**
	 * Called by an {@link TaskExecutionEngine} just after the end of  the main execution step of a
	 * task.
	 *
	 * @param aContext the task context.
	 * @param aConfiguration the task configuration.
	 * @throws LifeCycleException if something goes wrong.
	 */
	void complete(TaskContext aContext, Task aConfiguration)
		throws LifeCycleException;

	/**
	 * Called by an {@link TaskExecutionEngine} when the execution of the main task has failed.
	 *
	 * @param aContext the task context.
	 * @param aConfiguration the task configuration.
	 * @throws LifeCycleException if something goes wrong.
	 */
	void fail(TaskContext aContext, Task aConfiguration, Throwable aCause)
		throws LifeCycleException;

	/**
	 * Called by an {@link TaskExecutionEngine} just before the {@link TaskContext} is destroyed.
	 *
	 * @param aContext the task context.
	 */
	void destroy(TaskContext aContext);
}
