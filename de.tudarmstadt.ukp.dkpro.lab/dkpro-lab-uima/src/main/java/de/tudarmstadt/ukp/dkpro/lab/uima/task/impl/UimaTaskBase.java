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

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.storage.StreamWriter;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.TaskBase;
import de.tudarmstadt.ukp.dkpro.lab.uima.reporting.UimaDescriptorsReport;
import de.tudarmstadt.ukp.dkpro.lab.uima.task.UimaTask;

public abstract class UimaTaskBase extends TaskBase
	implements UimaTask
{
	private TypeSystemDescription typeSystem;

	{
		addReport(UimaDescriptorsReport.class);
	}

	@Override
	public void persist(final TaskContext aContext)
		throws IOException
	{
		super.persist(aContext);

		aContext.storeBinary(COLLECTION_READER_DESC_KEY, new StreamWriter()
		{
			@Override
			public void write(OutputStream aStream)
				throws Exception
			{
				getCollectionReaderDescription(aContext).toXML(aStream);
			}
		});

		aContext.storeBinary(ANALYSIS_ENGINE_DESC_KEY, new StreamWriter()
		{
			@Override
			public void write(OutputStream aStream)
				throws Exception
			{
				AnalysisEngineDescription analysisDesc = getAnalysisEngineDescription(aContext);
				// FIXME should use the same resource manager here
				// as the engine uses!
				analysisDesc.resolveImports(UIMAFramework.newDefaultResourceManager());
				analysisDesc.toXML(aStream);
			}
		});
	}

	public AnalysisEngineDescription createEngine(Class<? extends AnalysisComponent> aComponentClass,
			Object... aConfigurationData)
		throws ResourceInitializationException
	{
		return createEngineDescription(aComponentClass, getCachedTypeSystem(), aConfigurationData);
	}

	public AnalysisEngineDescription createEngine(Collection<AnalysisEngineDescription> aDescs)
		throws ResourceInitializationException
	{
		return createEngineDescription(aDescs.toArray(new AnalysisEngineDescription[aDescs
				.size()]));
	}

	public AnalysisEngineDescription createEngine(AnalysisEngineDescription... aDescs)
		throws ResourceInitializationException
	{
		return createEngineDescription(aDescs);
	}

	public CollectionReaderDescription createReader(Class<? extends CollectionReader> aReaderClass,
			Object... aConfigurationData)
		throws ResourceInitializationException
	{
		return createReaderDescription(aReaderClass, getCachedTypeSystem(), aConfigurationData);
	}

	public CollectionReaderDescription createReader(Class<? extends CollectionReader> aReaderClass,
			TypePriorities aPriorities, Object... aConfigurationData)
		throws ResourceInitializationException
	{
		return createReaderDescription(aReaderClass, getCachedTypeSystem(), aPriorities, aConfigurationData);
	}

	protected TypeSystemDescription getCachedTypeSystem()
		throws ResourceInitializationException
	{
		if (typeSystem == null) {
	        typeSystem = getTypeSystem();
		}
		return typeSystem;
	}

	@Override
	public TypeSystemDescription getTypeSystem()
		throws ResourceInitializationException
	{
		return createTypeSystemDescription();
	}
}
