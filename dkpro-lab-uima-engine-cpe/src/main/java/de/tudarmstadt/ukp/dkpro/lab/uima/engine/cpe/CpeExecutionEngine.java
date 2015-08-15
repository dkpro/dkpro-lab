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
package de.tudarmstadt.ukp.dkpro.lab.uima.engine.cpe;

import static org.apache.uima.UIMAFramework.newDefaultResourceManager;
import static org.apache.uima.fit.factory.ExternalResourceFactory.bindResource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.fit.cpe.CpeBuilder;
import org.apache.uima.resource.ResourceManager;

import de.tudarmstadt.ukp.dkpro.lab.engine.ExecutionException;
import de.tudarmstadt.ukp.dkpro.lab.engine.LifeCycleException;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContextFactory;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskExecutionEngine;
import de.tudarmstadt.ukp.dkpro.lab.task.Task;
import de.tudarmstadt.ukp.dkpro.lab.uima.task.TaskContextProvider;
import de.tudarmstadt.ukp.dkpro.lab.uima.task.UimaTask;

/**
 * CPE-based execution engine. This engine will try to automatically create so many threads that
 * each CPU core will be utilized.
 * <p>
 * Refer to {@link CpeBuilder} for information about how aggregte analysis engines are treated.
 */
public class CpeExecutionEngine
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

			// Scan components that accept the service and bind it to them
			bindResource(analysisDesc, TaskContext.class, TaskContextProvider.class,
					TaskContextProvider.PARAM_FACTORY_NAME, contextFactory.getId(),
					TaskContextProvider.PARAM_CONTEXT_ID, ctx.getId());

			CpeBuilder mgr = new CpeBuilder();
			ctx.message("CPE will be using " + Runtime.getRuntime().availableProcessors()
					+ " parallel threads to optimally utilize your cpu cores");
			mgr.setMaxProcessingUnitThreadCount(Runtime.getRuntime().availableProcessors());
			mgr.setReader(configuration.getCollectionReaderDescription(ctx));
			mgr.setAnalysisEngine(analysisDesc);
			StatusCallbackListenerImpl status = new StatusCallbackListenerImpl(ctx);
			CollectionProcessingEngine engine = mgr.createCpe(status);

			// Now the setup is complete
			ctx.getLifeCycleManager().initialize(ctx, aConfiguration);

			// Start recording
			ctx.getLifeCycleManager().begin(ctx, aConfiguration);

			// Run the experiment
			engine.process();
			try {
				synchronized (status) {
					while (status.isProcessing) {
						status.wait();
					}
				}
			}
			catch (InterruptedException e) {
				ctx.message("CPE interrupted.");
			}

			if (status.exceptions.size() > 0) {
				throw status.exceptions.get(0);
			}

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
			if (ctx != null) {
				ctx.destroy();
			}
		}
	}

	@Override
	public void setContextFactory(TaskContextFactory aContextFactory)
	{
		contextFactory = aContextFactory;
	}

	private class StatusCallbackListenerImpl
		implements StatusCallbackListener
	{
		private final TaskContext context;
		private final List<Exception> exceptions = new ArrayList<Exception>();
		private boolean isProcessing = true;

		public StatusCallbackListenerImpl(TaskContext aContext)
		{
			context = aContext;
		}

		@Override
		public void entityProcessComplete(CAS arg0, EntityProcessStatus arg1)
		{
			if (arg1.isException()) {
				context.message("Entity processing complete: " + arg1.getStatusMessage());
				for (Exception e : arg1.getExceptions()) {
					context.message("Exception occured: " + e.getMessage());
					StringWriter w = new StringWriter();
					e.printStackTrace(new PrintWriter(w));
					context.message(w.toString());
					exceptions.add(e);
				}
			}
		}

		@Override
		public void aborted()
		{
			context.message("aborted");
			// logger.log(Level.SEVERE, cpe.getPerformanceReport().toString());
			synchronized (this) {
				if (isProcessing) {
					isProcessing = false;
					notify();
				}
			}
		}

		@Override
		public void batchProcessComplete()
		{
			// Do nothing
		}

		@Override
		public void collectionProcessComplete()
		{
			context.message("collection process complete");
			// logger.log(Level.INFO, cpe.getPerformanceReport().toString());
			synchronized (this) {
				if (isProcessing) {
					isProcessing = false;
					notify();
				}
			}
		}

		@Override
		public void initializationComplete()
		{
			// Do nothing
		}

		@Override
		public void paused()
		{
			// Do nothing
		}

		@Override
		public void resumed()
		{
			// Do nothing
		}
	}
}
