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
package de.tudarmstadt.ukp.dkpro.lab.uima.engine.uimaas.component;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAFramework;
import org.apache.uima.aae.client.UimaAsynchronousEngine;
import org.apache.uima.adapter.jms.client.BaseUIMAAsynchronousEngine_impl;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;

public class SimpleClient
	extends JmsComponent
{
	private final Log log = LogFactory.getLog(getClass());

	private UimaAsynchronousEngine uimaAsEngine;
	private CollectionReaderDescription collectionReaderDesc;

	private String endpoint;

	private int casPoolSize = 2;
	private int fsHeapSize = 2000000;
	private int timeout = 0;
	private int getmeta_timeout = 60;
	private int cpc_timeout = 0;

	public SimpleClient(final String aEndpoint)
	{
		endpoint = aEndpoint;
	}

	public SimpleClient(final String aEndpoint, CollectionReaderDescription aCollectionReaderDesc)
	{
		endpoint = aEndpoint;
		collectionReaderDesc = aCollectionReaderDesc;
	}

	/**
	 * Initialize the UIMA-AS client.
	 */
	public void start()
		throws ResourceInitializationException
	{
		uimaAsEngine = new BaseUIMAAsynchronousEngine_impl();

		Map<String, Object> clientCtx = new HashMap<String, Object>();
		clientCtx.put(UimaAsynchronousEngine.ServerUri, getBrokerUrl());
		clientCtx.put(UimaAsynchronousEngine.Endpoint, endpoint);
		clientCtx.put(UimaAsynchronousEngine.Timeout, timeout * 1000);
		clientCtx.put(UimaAsynchronousEngine.GetMetaTimeout, getmeta_timeout * 1000);
		clientCtx.put(UimaAsynchronousEngine.CpcTimeout, cpc_timeout * 1000);
		clientCtx.put(UimaAsynchronousEngine.CasPoolSize, casPoolSize);
		clientCtx.put(UIMAFramework.CAS_INITIAL_HEAP_SIZE, new Integer(fsHeapSize / 4).toString());

		// Add Collection Reader
		if (collectionReaderDesc != null) {
			uimaAsEngine.setCollectionReader(UIMAFramework
					.produceCollectionReader(collectionReaderDesc));
		}

		// Add status listener
		// uimaAsEngine.addStatusCallbackListener(new StatusCallbackListenerImpl(ctx));

		// Initialize the client
		uimaAsEngine.initialize(clientCtx);

		log.debug("UIMA AS client started");
	}

	public void process() throws ResourceProcessException
	{
		uimaAsEngine.process();
	}

	public void process(CAS cas) throws ResourceProcessException
	{
		uimaAsEngine.sendAndReceiveCAS(cas);
	}

	public void stop()
		throws ResourceInitializationException
	{
		try {
			uimaAsEngine.stop();
		}
		catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
	}
}
