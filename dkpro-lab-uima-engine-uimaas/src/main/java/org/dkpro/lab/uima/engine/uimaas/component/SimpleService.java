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
package org.dkpro.lab.uima.engine.uimaas.component;

import static org.dkpro.lab.Util.getUrlAsFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.aae.client.UimaAsynchronousEngine;
import org.apache.uima.adapter.jms.client.BaseUIMAAsynchronousEngine_impl;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resourceSpecifier.factory.DeploymentDescriptorFactory;
import org.apache.uima.resourceSpecifier.factory.ServiceContext;
import org.apache.uima.resourceSpecifier.factory.UimaASPrimitiveDeploymentDescriptor;
import org.apache.uima.resourceSpecifier.factory.impl.ServiceContextImpl;
import org.xml.sax.SAXException;

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
	        // Save the AED to a file because UIMA-AS cannot have an AED direclty embedded in its
	        // descriptor
	        ResourceMetaData topMetaData = aeDesc.getMetaData();
	        File topDescriptorFile = File.createTempFile(getClass()
	                .getSimpleName(), ".xml");
	        topDescriptorFile.deleteOnExit();
	        try (OutputStream os = new FileOutputStream(topDescriptorFile)) {
	            aeDesc.toXML(os);
	        }
            catch (SAXException e) {
                throw new ResourceInitializationException(e);
            }

	        // Create service descriptor
	        ServiceContext context = new ServiceContextImpl(topMetaData.getName(),
	                topMetaData.getDescription(), topDescriptorFile.getAbsolutePath(), endpoint,
	                getBrokerUrl());
	        UimaASPrimitiveDeploymentDescriptor dd = DeploymentDescriptorFactory
	                .createPrimitiveDeploymentDescriptor(context);
		    

			deploymentDescriptionFile = File.createTempFile(getClass().getSimpleName(), ".xml");
			deploymentDescriptionFile.deleteOnExit();
			try {
			    dd.save(deploymentDescriptionFile);
			}
			catch (Exception e) {
                throw new ResourceInitializationException(e);
			}

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
