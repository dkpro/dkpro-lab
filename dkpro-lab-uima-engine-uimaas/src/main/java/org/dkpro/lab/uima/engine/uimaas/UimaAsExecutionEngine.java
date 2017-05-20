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
package org.dkpro.lab.uima.engine.uimaas;

import static org.dkpro.lab.Util.getUrlAsFile;
import static org.apache.uima.UIMAFramework.newDefaultResourceManager;
import static org.apache.uima.fit.factory.ExternalResourceFactory.bindResource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAFramework;
import org.apache.uima.aae.client.UimaAsBaseCallbackListener;
import org.apache.uima.aae.client.UimaAsynchronousEngine;
import org.apache.uima.adapter.jms.client.BaseUIMAAsynchronousEngine_impl;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resourceSpecifier.factory.DeploymentDescriptorFactory;
import org.apache.uima.resourceSpecifier.factory.ServiceContext;
import org.apache.uima.resourceSpecifier.factory.UimaASPrimitiveDeploymentDescriptor;
import org.apache.uima.resourceSpecifier.factory.impl.ServiceContextImpl;
import org.dkpro.lab.engine.ExecutionException;
import org.dkpro.lab.engine.LifeCycleException;
import org.dkpro.lab.engine.TaskContext;
import org.dkpro.lab.engine.TaskContextFactory;
import org.dkpro.lab.engine.TaskExecutionEngine;
import org.dkpro.lab.task.Task;
import org.dkpro.lab.uima.task.TaskContextProvider;
import org.dkpro.lab.uima.task.UimaTask;

/**
 * UIMA AS-based execution engine. An {@link UimaTask} is be executed using the UIMA AS framework.
 * This is currently a simple proof-of-concept implementation.
 */
public class UimaAsExecutionEngine
	implements TaskExecutionEngine
{
	private final Log log = LogFactory.getLog(getClass());

	private TaskContextFactory contextFactory;

	private String brokerUrl = "tcp://localhost:61616";

	private String endpoint = "experiment";

	private int casPoolSize = 2;

	private int fsHeapSize = 2000000;

	private int timeout = 0;

	private int getmeta_timeout = 60;

	private int cpc_timeout = 0;

	private TaskContext ctx;
	private UimaAsynchronousEngine uimaAsEngine;
	private String serviceId;
	private UimaTask configuration;

	@Override
	public String run(Task aConfiguration)
		throws ExecutionException, LifeCycleException
	{
		if (!(aConfiguration instanceof UimaTask)) {
			throw new ExecutionException("This engine can only execute ["
					+ UimaTask.class.getName() + "]");
		}

		configuration = (UimaTask) aConfiguration;

		ctx = contextFactory.createContext(aConfiguration);
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
			ctx.message("Bound external resources");

			// Now the setup is complete
			ctx.getLifeCycleManager().initialize(ctx, aConfiguration);

			// Deploy experiment as UIMA-AS service
			initializeService();

			// Initialize the client
			initializeClient();

			// Start recording
			ctx.getLifeCycleManager().begin(ctx, aConfiguration);

			// Run the experiment
			process();

			// End recording
			ctx.getLifeCycleManager().complete(ctx, aConfiguration);

			return ctx.getId();
		}
		catch (LifeCycleException e) {
			ctx.getLifeCycleManager().fail(ctx, aConfiguration, e);
			throw e;
		}
		catch (Exception e) {
			ctx.getLifeCycleManager().fail(ctx, aConfiguration, e);
			throw new ExecutionException(e);
		}
		finally {
			destroy();
		}
	}

	protected void initializeService() throws Exception
	{
		// Create Asynchronous Engine API
		uimaAsEngine = new BaseUIMAAsynchronousEngine_impl();

		// Save the AED to a file because UIMA-AS cannot have an AED direclty embedded in its
		// descriptor
		AnalysisEngineDescription topDescriptor = configuration.getAnalysisEngineDescription(ctx);
		ResourceMetaData topMetaData = topDescriptor.getMetaData();
		File topDescriptorFile = File.createTempFile(getClass()
                .getSimpleName(), ".xml");
        topDescriptorFile.deleteOnExit();
        try (OutputStream os = new FileOutputStream(topDescriptorFile)) {
            topDescriptor.toXML(os);
        }

        // Create service descriptor
        ServiceContext context = new ServiceContextImpl(topMetaData.getName(),
                topMetaData.getDescription(), topDescriptorFile.getAbsolutePath(), endpoint,
                brokerUrl);
        UimaASPrimitiveDeploymentDescriptor dd = DeploymentDescriptorFactory
                .createPrimitiveDeploymentDescriptor(context);

        // Store service descriptor also to a temporary file
		File deploymentDescriptionFile = File.createTempFile(getClass().getSimpleName(), ".xml");
		deploymentDescriptionFile.deleteOnExit();
		dd.save(deploymentDescriptionFile);

		Map<String, Object> serviceCtx = new HashMap<String, Object>();
		serviceCtx.put(UimaAsynchronousEngine.DD2SpringXsltFilePath, getUrlAsFile(
				getClass().getResource("/uima-as/dd2spring.xsl"), true).getAbsolutePath());
		serviceCtx.put(UimaAsynchronousEngine.SaxonClasspath, getClass().getResource(
				"/uima-as/saxon8.jar").toString());
		serviceId = uimaAsEngine.deploy(deploymentDescriptionFile.getAbsolutePath(), serviceCtx);
		ctx.message("Deployed experiment as UIMA-AS service: [" + serviceId + "]");
	}

	/**
	 * Initialize the UIMA-AS client.
	 */
	protected void initializeClient()
		throws ResourceInitializationException, IOException
	{
		Map<String, Object> clientCtx = new HashMap<String, Object>();
		clientCtx.put(UimaAsynchronousEngine.ServerUri, brokerUrl);
		clientCtx.put(UimaAsynchronousEngine.ENDPOINT, endpoint);
		clientCtx.put(UimaAsynchronousEngine.Timeout, timeout * 1000);
		clientCtx.put(UimaAsynchronousEngine.GetMetaTimeout, getmeta_timeout * 1000);
		clientCtx.put(UimaAsynchronousEngine.CpcTimeout, cpc_timeout * 1000);
		clientCtx.put(UimaAsynchronousEngine.CasPoolSize, casPoolSize);
		clientCtx.put(UIMAFramework.CAS_INITIAL_HEAP_SIZE, new Integer(fsHeapSize / 4).toString());

		// Add Collection Reader
		uimaAsEngine.setCollectionReader(UIMAFramework.produceCollectionReader(configuration
				.getCollectionReaderDescription(ctx)));

		// Add status listener
		uimaAsEngine.addStatusCallbackListener(new StatusCallbackListenerImpl(ctx));

		// Initialize the client
		uimaAsEngine.initialize(clientCtx);
		ctx.message("Initialized asynchronous client");
	}

	protected void process() throws ResourceProcessException
	{
		uimaAsEngine.process();
	}

	/**
	 * Shut the UIMA-AS client down.
	 */
	protected void shutdownClient()
	{
		if (uimaAsEngine != null) {
			try {
				uimaAsEngine.stop();
				ctx.message("Shut down asynchronous client");
			}
			catch (Exception e) {
				log.error("Error shutting down asynchronous client", e);
			}
			uimaAsEngine = null;
		}
	}

	/**
	 * Un-deploy the experiment service.
	 */
	protected void shutdownService()
	{
		if (serviceId != null) {
			try {
				// Undeploy experiment
				uimaAsEngine.undeploy(serviceId);
				ctx.message("Undeployed experiment service ["+serviceId+"]");
			}
			catch (Exception e) {
				log.error("Error undeploying experiment service", e);
			}
			serviceId = null;
		}
	}

	/**
	 * Release all resources.
	 */
	protected void destroy()
	{
		shutdownService();
		shutdownClient();

		if (ctx != null) {
			ctx.destroy();
		}
	}

	@Override
	public void setContextFactory(TaskContextFactory aContextFactory)
	{
		contextFactory = aContextFactory;
	}

    @Override
    public TaskContextFactory getContextFactory()
    {
        return contextFactory;
    }

	/**
	 * Callback Listener. Receives event notifications from UIMA AS.
	 */
	static class StatusCallbackListenerImpl
		extends UimaAsBaseCallbackListener
	{
		private final TaskContext ctx;

		int entityCount = 0;

		public StatusCallbackListenerImpl(final TaskContext aCtx)
		{
			ctx = aCtx;
		}

		/**
		 * Called when the initialization is completed.
		 */
		@Override
		public void initializationComplete(EntityProcessStatus aStatus)
		{
			if (aStatus != null && aStatus.isException()) {
				ctx.message("Error on getMeta call to remote service:");
				List<Exception> exceptions = aStatus.getExceptions();
				for (int i = 0; i < exceptions.size(); i++) {
					((Throwable) exceptions.get(i)).printStackTrace();
				}
			}
			ctx.message("UIMAEE Initialization Complete");
		}

		/**
		 * Called when the collection processing is completed.
		 */
		@Override
		public void collectionProcessComplete(EntityProcessStatus aStatus)
		{
			if (aStatus != null && aStatus.isException()) {
				ctx.message("Error on collection process complete call to remote service:");
				List<Exception> exceptions = aStatus.getExceptions();
				for (int i = 0; i < exceptions.size(); i++) {
					((Throwable) exceptions.get(i)).printStackTrace();
				}
			}
			ctx.message("Completed " + entityCount + " documents");
		}

		/**
		 * Called when the processing of a Document is completed. <br>
		 * The process status can be looked at and corresponding actions taken.
		 *
		 * @param aCas
		 *            CAS corresponding to the completed processing
		 * @param aStatus
		 *            EntityProcessStatus that holds the status of all the events for aEntity
		 */
		@Override
		public void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus)
		{
			if (aStatus != null && aStatus.isException()) {
				System.err.println("Error on process CAS call to remote service:");
				List<Exception> exceptions = aStatus.getExceptions();
				for (int i = 0; i < exceptions.size(); i++) {
					((Throwable) exceptions.get(i)).printStackTrace();
				}
			}
		}
	}
}
