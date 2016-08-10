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

import org.apache.activemq.broker.BrokerService;
import org.apache.uima.resource.ResourceInitializationException;

public class SimpleBroker extends JmsComponent
{
	private BrokerService broker;

	public SimpleBroker()
	{
		// Nothing to do
	}

	public void start() throws ResourceInitializationException
	{
		try {
			broker = new BrokerService();
			broker.addConnector(getBrokerUrl());
			broker.setPersistent(false);
			broker.start();
		}
		catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
	}

	public void stop() throws ResourceInitializationException
	{
		try {
			broker.stop();
		}
		catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
	}
}
