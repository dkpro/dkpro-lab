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
package de.tudarmstadt.ukp.dkpro.lab.uima.task.impl;

import static org.uimafit.factory.AnalysisEngineFactory.createAggregateDescription;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;

/**
 * Simple UIMA task configuration which allows you to set pre-generated
 * {@link CollectionReaderDescription} and {@link AnalysisEngineDescription}s. In most cases you
 * would want to extend {@link UimaTaskBase} and just override
 * {@link UimaTaskBase#getCollectionReaderDescription()} and
 * {@link UimaTaskBase#getAnalysisEngineDescription()}.
 *
 * @author Richard Eckart de Castilho
 */
public class DefaultUimaTask
	extends UimaTaskBase
{
	private CollectionReaderDescription readerDesc;
	private AnalysisEngineDescription analysisDesc;

	public DefaultUimaTask()
	{
		// Do nothing
	}

	public DefaultUimaTask(CollectionReaderDescription aReader, AnalysisEngineDescription... aAes)
		throws ResourceInitializationException
	{
		setReaderDescription(aReader);
		if (aAes.length == 1) {
			setAnalysisEngineDescription(aAes[0]);
		}
		else {
			setAnalysisEngineDescription(createAggregateDescription(aAes));
		}
	}

	@Override
	public CollectionReaderDescription getCollectionReaderDescription(TaskContext aContext)
		throws ResourceInitializationException
	{
		return readerDesc;
	}

	public void setReaderDescription(CollectionReaderDescription aReaderDesc)
	{
		readerDesc = aReaderDesc;
	}

	@Override
	public AnalysisEngineDescription getAnalysisEngineDescription(TaskContext aContext)
		throws ResourceInitializationException

	{
		return analysisDesc;
	}

	public void setAnalysisEngineDescription(
			AnalysisEngineDescription aAnalysisDesc)
	{
		analysisDesc = aAnalysisDesc;
	}
}
