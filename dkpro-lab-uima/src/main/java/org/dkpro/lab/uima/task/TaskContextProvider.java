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
package org.dkpro.lab.uima.task;

import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContextAdmin;
import org.apache.uima.fit.component.initialize.ConfigurationParameterInitializer;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResourceLocator;
import org.apache.uima.resource.ConfigurationManager;
import org.apache.uima.resource.CustomResourceSpecifier;
import org.apache.uima.resource.Parameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.Resource_ImplBase;
import org.dkpro.lab.engine.TaskContextFactory;
import org.dkpro.lab.engine.impl.StaticContext;

/**
 * Allows access to the experiment context from with a UIMA component. The context can be injected
 * as an external resource.
 */
public class TaskContextProvider
	extends Resource_ImplBase
	implements ExternalResourceLocator
{
	public static final String PARAM_FACTORY_NAME = "FactoryName";
	@ConfigurationParameter(name = PARAM_FACTORY_NAME, mandatory = true)
	private String factoryName;

	public static final String PARAM_CONTEXT_ID = "InstanceId";
	@ConfigurationParameter(name = PARAM_CONTEXT_ID, mandatory = true)
	private String instanceId;

	@Override
	public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
		throws ResourceInitializationException
	{
		if (!super.initialize(aSpecifier, aAdditionalParams)) {
			return false;
		}

		// Create synthetic context to be able to use InitializeUtil.
		UimaContextAdmin context = UIMAFramework.newUimaContext(UIMAFramework.getLogger(),
				UIMAFramework.newDefaultResourceManager(), UIMAFramework.newConfigurationManager());
		ConfigurationManager cfgMgr = context.getConfigurationManager();
		cfgMgr.setSession(context.getSession());
		CustomResourceSpecifier spec = (CustomResourceSpecifier) aSpecifier;
		for (Parameter p : spec.getParameters()) {
			cfgMgr.setConfigParameterValue(context.getQualifiedContextName() + p.getName(), p
					.getValue());
		}
		ConfigurationParameterInitializer.initialize(this, context);

		return true;
	}

	@Override
	public Object getResource()
	{
		TaskContextFactory f = (TaskContextFactory) StaticContext.lookup(factoryName);
		return f.getContext(instanceId);
	}
}
