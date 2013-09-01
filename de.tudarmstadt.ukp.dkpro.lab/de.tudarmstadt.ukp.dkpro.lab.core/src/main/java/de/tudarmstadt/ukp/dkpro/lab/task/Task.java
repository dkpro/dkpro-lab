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
package de.tudarmstadt.ukp.dkpro.lab.task;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;

import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.reporting.Report;

/**
 * Task configuration API.
 *
 * @author Richard Eckart de Castilho
 */
public interface Task
{
	public static final String PROPERTIES_KEY = "PROPERTIES.txt";
	public static final String DISCRIMINATORS_KEY = "DISCRIMINATORS.txt";

	String getType();

	/**
	 * Persist the task configuration to the given task context. This allows subclasses to store
	 * additional information depending on their needs.
	 *
	 * @param aContext a task context.
	 */
	void persist(TaskContext aContext) throws IOException;

	/**
	 * Set a task context attribute.
	 *
	 * @param aKey the attribute name.
	 * @param aValue the value.
	 */
	void setAttribute(String aKey, String aValue);

	String getAttribute(String aKey);

	public Map<String, String> getAttributes();

	/**
	 * Set a task descriminator.
	 *
	 * @param aKey the descriminator name.
	 * @param aValue the value.
	 * 
	 * @see #getDescriminators()
	 */
	void setDescriminator(String aKey, String aValue);

	String getDescriminator(String aKey);

	/**
	 * Get the discriminators for the task. Discriminators are used to determine if a task has
	 * already been executed for a particular parameter configuration. Normally the discriminators
	 * are the set of all parameters that affets a task's output and their parameter values.
	 */
	Map<String, String> getDescriminators();

	/**
	 * Get the discriminators for the task and all tasks it depends on.
	 * 
	 * @param aContext
	 *            context of the current task used to resolve the data-dependencies.
	 * @return merged set of discriminators of the current task and the tasks it depends on.
	 * @throws IllegalStateException
	 *             if there is a conflict between the discriminators of an imported task context and
	 *             the current discriminator values of the task.
	 */
	Map<String, String> getResolvedDescriminators(TaskContext aContext);

	/**
	 * Import the given key from some URI.
	 *
	 * @param aKey the local key
	 * @param aUri the URI to import from
	 * @deprecated Use {@link #addImport(URI, String)} or {@link #addImport(File, String)}.
	 */
	@Deprecated
	void addImport(String aKey, String aUri);

	/**
	 * @deprecated Use {@link #addImport(TaskContext, String, String)}.
	 */
    @Deprecated
	void addImportById(String aKey, String aUuid, String aSourceKey);

	/**
	 * Import the given key from the latest run of the specified task type.
	 *
	 * @param aKey the local key
	 * @param aSourceKey the key to import
	 * @param aType the task type to import from
	 * @deprecated Use {@link #addImport(Task, String)} or {@link #addImport(Task, String, String)}
	 * instead.
	 */
	@Deprecated
	void addImportLatest(String aKey, String aSourceKey, String aType);

	/**
     * @deprecated Use {@link #addImport(Task, String)} or {@link #addImport(Task, String, String)}
     * instead. This signature still exists because it is used internally.
	 */
	@Deprecated
	void addImportLatest(String aKey, String aSourceKey, String aType, String... aConstraints);

    /**
     * @deprecated Use {@link #addImport(Task, String)} or {@link #addImport(Task, String, String)}
     * instead. This signature still exists because it is used internally.
     */
    @Deprecated
	void addImportLatest(String aKey, String aSourceKey, String aType, Map<String, String> aConstraints);

    /**
     * Import the given URI under the given key to this task.
     *
     * @param aUri the URI to import
     * @param aKey the key to import as
     */
    void addImport(URI aUri, String aKey);

    /**
     * Import the given file/folder under the given key to this task.
     *
     * @param aFile the File to import
     * @param aKey the key to import as
     */
    void addImport(File aFile, String aKey);

    /**
     * Import the given key from the latest run of the specified task type.
     *
     * @param aTask the task to import from
     * @param aKey the key to import
     */
	void addImport(Task aTask, String aKey);

    /**
     * Import the given key from the latest run of the specified task type.
     *
     * @param aTask the task to import from.
     * @param aKey the key to import.
     * @param aAlias an alias by which the current task is going to access the key.
     */
	void addImport(Task aTask, String aKey, String aAlias);
	
    /**
     * Import the given key from the latest run of the specified task type.
     *
     * @param aTaskContext the task context to import from.
     * @param aKey the key to import.
     * @param aAlias an alias by which the current task is going to access the key.
     */
	void addImport(TaskContext aTaskContext, String aKey, String aAlias);

	Map<String, String> getImports();

	/**
	 * Add a report to be executed after running the task. Reports cannot be configured directly,
	 * but they may access information stored in the task properties or that is available via the
	 * storage service.
	 *
	 * @param aReport the report class to be executed.
	 */
	void addReport(Class<? extends Report> aReport);

	void removeReport(Class<? extends Report> aReport);

	Set<Class<? extends Report>> getReports();
}
