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
package de.tudarmstadt.ukp.dkpro.lab.uima.task;

import java.io.IOException;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.task.Task;

/**
 * Task configuration for an UIMA-based task. Any UIMA task always requires a
 * {@link CollectionReaderDescription} and a {@link AnalysisEngineDescription}.
 *
 * @author Richard Eckart de Castilho
 */
public interface UimaTask
	extends Task
{
	public static final String COLLECTION_READER_DESC_KEY = "CollectionReaderDescription.xml";
	public static final String ANALYSIS_ENGINE_DESC_KEY = "AnalysisEngineDescription.xml";

	CollectionReaderDescription getCollectionReaderDescription(TaskContext aContext)
		throws ResourceInitializationException, IOException;

	AnalysisEngineDescription getAnalysisEngineDescription(TaskContext aContext)
		throws ResourceInitializationException, IOException;

	public TypeSystemDescription getTypeSystem()
		throws ResourceInitializationException;
}
