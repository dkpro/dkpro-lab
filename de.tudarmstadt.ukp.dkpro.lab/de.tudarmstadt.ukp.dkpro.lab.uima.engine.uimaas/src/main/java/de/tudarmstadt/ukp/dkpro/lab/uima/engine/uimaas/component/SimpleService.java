package de.tudarmstadt.ukp.dkpro.lab.uima.engine.uimaas.component;

import static de.tudarmstadt.ukp.dkpro.lab.Util.getUrlAsFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.aae.client.UimaAsynchronousEngine;
import org.apache.uima.adapter.jms.client.BaseUIMAAsynchronousEngine_impl;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.lab.uima.engine.uimaas.AsDeploymentDescription;

public class SimpleService extends JmsComponent
{
	private final Log log = LogFactory.getLog(getClass());

	private UimaAsynchronousEngine uimaAsEngine;

	private String endpoint;
	private AnalysisEngineDescription aeDesc;
	private String serviceId;

	public SimpleService(final String aEndpoint, final AnalysisEngineDescription aAeDesc)
	{
		endpoint = aEndpoint;
		aeDesc = aAeDesc;
	}

	/**
	 * Initialize the UIMA-AS client.
	 */
	public void start()
		throws ResourceInitializationException
	{
		uimaAsEngine = new BaseUIMAAsynchronousEngine_impl();

		Map<String, Object> serviceCtx = new HashMap<String, Object>();
		File deploymentDescriptionFile;

		try {
			// Create service descriptor
			AsDeploymentDescription deploymentDescription = new AsDeploymentDescription(
					aeDesc, endpoint, getBrokerUrl());

			deploymentDescriptionFile = File.createTempFile(getClass().getSimpleName(), ".xml");
			deploymentDescriptionFile.deleteOnExit();
			deploymentDescription.toXML(deploymentDescriptionFile);
			deploymentDescription.toXML(System.out);

			serviceCtx.put(UimaAsynchronousEngine.DD2SpringXsltFilePath, getUrlAsFile(
					getClass().getResource("/uima-as/dd2spring.xsl"), true).getAbsolutePath());
			serviceCtx.put(UimaAsynchronousEngine.SaxonClasspath, getClass().getResource(
					"/uima-as/saxon8.jar").toString());
		}
		catch (IOException e) {
			throw new ResourceInitializationException(e);
		}

		try {
			serviceId = uimaAsEngine.deploy(deploymentDescriptionFile.getAbsolutePath(), serviceCtx);
			log.debug("UIMA AS service deployed: [" + serviceId + "]");
		}
		catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
	}

	public void stop() throws ResourceInitializationException
	{
		try {
			uimaAsEngine.undeploy(serviceId);
		}
		catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
	}

	public String getServiceId()
	{
		return serviceId;
	}
}
