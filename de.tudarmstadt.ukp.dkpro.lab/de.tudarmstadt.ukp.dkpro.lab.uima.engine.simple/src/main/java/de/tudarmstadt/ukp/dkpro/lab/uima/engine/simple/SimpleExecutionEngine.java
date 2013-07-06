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
package de.tudarmstadt.ukp.dkpro.lab.uima.engine.simple;

import static org.apache.uima.UIMAFramework.newConfigurationManager;
import static org.apache.uima.UIMAFramework.newDefaultResourceManager;
import static org.apache.uima.UIMAFramework.newUimaContext;
import static org.apache.uima.UIMAFramework.produceCollectionReader;
import static org.apache.uima.fit.factory.ExternalResourceFactory.bindResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.UimaContextAdmin;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.impl.AggregateAnalysisEngine_impl;
import org.apache.uima.analysis_engine.impl.PrimitiveAnalysisEngine_impl;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.Logger;
import org.apache.uima.util.Progress;

import de.tudarmstadt.ukp.dkpro.lab.engine.ExecutionException;
import de.tudarmstadt.ukp.dkpro.lab.engine.LifeCycleException;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContextFactory;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskExecutionEngine;
import de.tudarmstadt.ukp.dkpro.lab.task.Task;
import de.tudarmstadt.ukp.dkpro.lab.uima.task.TaskContextProvider;
import de.tudarmstadt.ukp.dkpro.lab.uima.task.UimaTask;
import de.tudarmstadt.ukp.dkpro.lab.uima.task.impl.UimaLoggingAdapter;

/**
 * UUTUC-based execution engine. An {@link UimaTask} is be executed using a simple single-threaded
 * approach. Useful for fool-proof setups and quick testing.
 *
 * @author Richard Eckart de Castilho
 */
public class SimpleExecutionEngine
	implements TaskExecutionEngine
{
	private TaskContextFactory contextFactory;

	@Override
	public String run(Task aConfiguration)
		throws ExecutionException, LifeCycleException
	{
		if (!(aConfiguration instanceof UimaTask)) {
			throw new ExecutionException("This engine can only execute ["
					+ UimaTask.class.getName() + "]");
		}

		UimaTask configuration = (UimaTask) aConfiguration;

		// Create persistence service for injection into analysis components
		TaskContext ctx = contextFactory.createContext(aConfiguration);
		try {
			ResourceManager resMgr = newDefaultResourceManager();

			// Make sure the descriptor is fully resolved. It will be modified and
			// thus should not be modified again afterwards by UIMA.
			AnalysisEngineDescription analysisDesc = configuration
					.getAnalysisEngineDescription(ctx);
			analysisDesc.resolveImports(resMgr);
			if (analysisDesc.getMetaData().getName() == null) {
				analysisDesc.getMetaData().setName("Analysis for "+aConfiguration.getType());
			}

			// Scan components that accept the service and bind it to them
			bindResource(analysisDesc, TaskContext.class, TaskContextProvider.class,
					TaskContextProvider.PARAM_FACTORY_NAME, contextFactory.getId(),
					TaskContextProvider.PARAM_CONTEXT_ID, ctx.getId());

			// Set up UIMA context & logging
			Logger logger = new UimaLoggingAdapter(ctx);
			UimaContextAdmin uimaCtx = newUimaContext(logger, resMgr, newConfigurationManager());

			// Set up reader
			CollectionReaderDescription readerDesc = configuration
					.getCollectionReaderDescription(ctx);
			if (readerDesc.getMetaData().getName() == null) {
				readerDesc.getMetaData().setName("Reader for "+aConfiguration.getType());
			}
			Map<String, Object> addReaderParam = new HashMap<String, Object>();
			addReaderParam.put(Resource.PARAM_UIMA_CONTEXT, uimaCtx);
			addReaderParam.put(Resource.PARAM_RESOURCE_MANAGER, resMgr);
			CollectionReader reader = produceCollectionReader(readerDesc, resMgr, addReaderParam);

			// Set up analysis engine
			AnalysisEngine engine;
			if (analysisDesc.isPrimitive()) {
				engine = new PrimitiveAnalysisEngine_impl();
			}
			else {
				engine = new AggregateAnalysisEngine_impl();
			}
			Map<String, Object> addEngineParam = new HashMap<String, Object>();
			addReaderParam.put(Resource.PARAM_UIMA_CONTEXT, uimaCtx);
			addReaderParam.put(Resource.PARAM_RESOURCE_MANAGER, resMgr);
			engine.initialize(analysisDesc, addEngineParam);

			// Now the setup is complete
			ctx.getLifeCycleManager().initialize(ctx, aConfiguration);

			// Start recording
			ctx.getLifeCycleManager().begin(ctx, aConfiguration);

			// Run the experiment
			// Apply the engine to all documents provided by the reader
			List<ResourceMetaData> metaData = new ArrayList<ResourceMetaData>();
			metaData.add(reader.getMetaData());
			metaData.add(engine.getMetaData());
			CAS cas = CasCreationUtils.createCas(metaData);

			while (reader.hasNext()) {
				reader.getNext(cas);
				engine.process(cas);
				cas.reset();

				Progress[] progresses = reader.getProgress();
				if (progresses != null) {
					for (Progress p : progresses) {
						ctx.message("Progress " + readerDesc.getImplementationName() + " "
								+ p.getCompleted() + "/" + p.getTotal() + " " + p.getUnit());
					}
				}
			}

			// Shut down engine and reader
			engine.collectionProcessComplete();
			reader.close();
			engine.destroy();
			reader.destroy();

			// End recording
			ctx.getLifeCycleManager().complete(ctx, aConfiguration);

			return ctx.getId();
		}
		catch (LifeCycleException e) {
			ctx.getLifeCycleManager().fail(ctx, aConfiguration, e);
			throw e;
		}
		catch (Throwable e) {
			ctx.getLifeCycleManager().fail(ctx, aConfiguration, e);
			throw new ExecutionException(e);
		}
		finally {
			ctx.destroy();
		}
	}

	@Override
	public void setContextFactory(TaskContextFactory aContextFactory)
	{
		contextFactory = aContextFactory;
	}
}
