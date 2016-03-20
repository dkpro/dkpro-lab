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
package org.dkpro.lab.reporting;

import org.dkpro.lab.engine.TaskContext;
import org.dkpro.lab.engine.TaskExecutionEngine;
import org.dkpro.lab.task.Task;

/**
 * Reports can be attached to task configurations. They are  simple tasks that directly provide an
 * {@link #execute()} method instead of relying on a {@link TaskExecutionEngine}. Reports are not
 * configurable as such. However, a particular {@link Task} configuration may provide external
 * bindings, storage keys or imports that are particularly targeted at configuring a report or a
 * report may depend on certain storage keys, imports or external bindings to be present to do its
 * work properly.
 */
public interface Report
{
	public static final String TASK_LABEL_FUNC_PROP = "TaskLabelFunctionProperty";

	/**
	 * Set the task context.
	 *
	 * @param aContext the task context.
	 */
	void setContext(TaskContext aContext);

	/**
	 * Execute the report.
	 *
	 * @throws Exception if something goes haywire.
	 */
	void execute() throws Exception;
}
