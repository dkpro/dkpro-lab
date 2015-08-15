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
package de.tudarmstadt.ukp.dkpro.lab.engine.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.io.Resource;

import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContextFactory;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskExecutionEngine;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskExecutionService;
import de.tudarmstadt.ukp.dkpro.lab.task.Task;

public class DefaultTaskExecutionService
	implements TaskExecutionService
{
    @Autowired
    private AutowireCapableBeanFactory beanFactory;
    
	private TaskContextFactory contextFactory;

	private final Map<Class<? extends Task>, Class<? extends TaskExecutionEngine>> map;

	{
		map = new HashMap<Class<? extends Task>, Class<? extends TaskExecutionEngine>>();
	}

	@Override
	public String run(Task aConfiguration)
		throws Exception
	{
		TaskExecutionEngine engine = createEngine(aConfiguration);
		return engine.run(aConfiguration);
	}

	@Override
	public TaskExecutionEngine createEngine(Task aConfiguration)
	{
		try {
			for (Class<? extends Task> taskClass : map.keySet()) {
				if (taskClass.isAssignableFrom(aConfiguration.getClass())) {
					TaskExecutionEngine engine = map.get(taskClass).newInstance();
					engine.setContextFactory(contextFactory);
					beanFactory.autowireBean(engine);
					return engine;
				}
			}
		}
		catch (InstantiationException e) {
			throw new RuntimeException(e);
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}

		throw new IllegalArgumentException("No engine registered for type ["
				+ aConfiguration.getClass().getName() + "]");
	}

	@SuppressWarnings("unchecked")
	public void setMappingDescriptors(Resource[] aResources)
		throws ClassNotFoundException, IOException
	{
		final ClassLoader cl = getClass().getClassLoader();
		for (final Resource res : aResources) {
			final Properties props = new Properties();
			props.load(res.getInputStream());
			for (final Object key : props.keySet()) {
				final String taskClass = (String) key;
				final String engineClass = props.getProperty(taskClass);
				map.put((Class<? extends Task>) Class.forName(taskClass, true, cl),
						(Class<? extends TaskExecutionEngine>) Class.forName(engineClass, true, cl));
			}
		}
	}
	
    public void registerEngine(Class<? extends Task> aTaskClazz,
            Class<? extends TaskExecutionEngine> aEngineClazz)
    {
        map.put(aTaskClazz, aEngineClazz);
    }
    
    public void unregisterEngine(Class<? extends Task> aTaskClazz)
    {
        map.remove(aTaskClazz);
    }
    
    public Class<? extends TaskExecutionEngine> getEngine(Class<? extends Task> aTaskClazz)
    {
        return map.get(aTaskClazz);
    }

	public void setContextFactory(TaskContextFactory aContextFactory)
	{
		contextFactory = aContextFactory;
	}

	@Override
	public TaskContextFactory getContextFactory()
	{
		return contextFactory;
	}
}
