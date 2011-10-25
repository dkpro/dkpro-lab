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

import java.io.IOException;
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
	 * @throws IOException
	 */
	void persist(TaskContext aContext) throws IOException;

	/**
	 * Set a task context property.
	 *
	 * @param aKey the property name.
	 * @param aValue the value.
	 */
	void setProperty(String aKey, String aValue);

	String getProperty(String aKey);

	public Map<String, String> getProperties();

	/**
	 * Set a task descriminator.
	 *
	 * @param aKey the descriminator name.
	 * @param aValue the value.
	 */
	void setDescriminator(String aKey, String aValue);

	String getDescriminator(String aKey);

	Map<String, String> getDescriminators();

	Map<String, String> getResolvedDescriminators(TaskContext aContext);

	/**
	 * Import the given key from some URI.
	 *
	 * @param aKey the local key
	 * @param aUri the URI to import from
	 */
	void addImport(String aKey, String aUri);

	void addImportById(String aKey, String aUuid, String aSourceKey);

	/**
	 * Import the given key from the latest run of the specified task type.
	 *
	 * @param aKey the local key
	 * @param aSourceKey the key to import
	 * @param aType the task type to import from
	 */
	void addImportLatest(String aKey, String aSourceKey, String aType);

	void addImportLatest(String aKey, String aSourceKey, String aType, String... aConstraints);

	void addImportLatest(String aKey, String aSourceKey, String aType, Map<String, String> aConstraints);

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
