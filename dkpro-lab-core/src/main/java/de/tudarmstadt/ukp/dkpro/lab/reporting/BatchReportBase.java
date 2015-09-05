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
package de.tudarmstadt.ukp.dkpro.lab.reporting;

import de.tudarmstadt.ukp.dkpro.lab.task.BatchTask;
import de.tudarmstadt.ukp.dkpro.lab.task.TaskContextMetadata;

/**
 * Base class for reports on {@link BatchTask}s. Provides convenience methods to access the subtask
 * contexts.
 */
public abstract class BatchReportBase
	extends ReportBase
{
	/**
	 * Get the context IDs of the subtasks.
	 */
	protected String[] getSubtaskContextIds()
	{
		String allSubTasks = getProperties().get(BatchTask.SUBTASKS_KEY);
		// Remove the "[]" generated by Collection.toString()
		allSubTasks = allSubTasks.substring(1, allSubTasks.length() - 1);
		// Split into the separate ids
		String[] subTasks = allSubTasks.split("\\s*,\\s*");
		return subTasks;
	}

	/**
	 * Get the context meta data of the subtasks.
	 */
	protected TaskContextMetadata[] getSubtasks()
	{
		String[] subTaskIds = getSubtaskContextIds();
		TaskContextMetadata[] subTaskMeta = new TaskContextMetadata[subTaskIds.length];
		for (int i = 0; i < subTaskIds.length; i++) {
			subTaskMeta[i] = getContext().getStorageService().getContext(subTaskIds[i]);
			subTaskMeta[i].setLabel(getContextLabel(subTaskIds[i]));
		}
		return subTaskMeta;
	}
}