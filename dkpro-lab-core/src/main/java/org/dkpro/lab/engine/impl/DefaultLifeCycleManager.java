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
package org.dkpro.lab.engine.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.dkpro.lab.engine.LifeCycleException;
import org.dkpro.lab.engine.LifeCycleManager;
import org.dkpro.lab.engine.TaskContext;
import org.dkpro.lab.reporting.Report;
import org.dkpro.lab.task.ConfigurationAware;
import org.dkpro.lab.task.Task;
import org.dkpro.lab.task.TaskContextMetadata;
import org.dkpro.lab.task.impl.ParameterUtil;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.dao.DataAccessResourceFailureException;

public class DefaultLifeCycleManager
	implements LifeCycleManager
{
    @Override
    public void configure(TaskContext aParentContext, Task aTask, Map<String, Object> aConfiguration)
    {
        PropertyAccessor paBean = PropertyAccessorFactory.forBeanPropertyAccess(aTask);
        PropertyAccessor paDirect = PropertyAccessorFactory.forDirectFieldAccess(aTask);
        for (Entry<String, Object> property : aConfiguration.entrySet()) {
            String key = property.getKey();
            Object value = property.getValue();

            // Find all fields that are annotated with a discriminator/property that have
            // a non-default name and might apply.
            for (String prop : ParameterUtil.findBeanPropertiesWithName(aTask, key)) {
                // Try setter - there may be extra logic in the setter
                if (paBean.isWritableProperty(prop)) {
                    paBean.setPropertyValue(prop, value);
                }
                // Otherwise try direct access
                else if (paDirect.isWritableProperty(prop)) {
                    paDirect.setPropertyValue(prop, value);
                }
            }

            // And try once again for all fields where the name is not explicitly set
            
            // Try setter - there may be extra logic in the setter
            if (paBean.isWritableProperty(key)) {
                paBean.setPropertyValue(key, value);
            }
            // Otherwise try direct access
            else if (paDirect.isWritableProperty(key)) {
                paDirect.setPropertyValue(key, value);
            }
        }
        
        if (aTask instanceof ConfigurationAware) {
            ((ConfigurationAware) aTask).setConfiguration(aConfiguration);
        }

        if (aParentContext != null) {
            aParentContext.message("Injected parameters into ["+aTask.getType()+"]");
        }
    }
    
	@Override
    public void initialize(TaskContext aContext, Task aConfiguration)
		throws LifeCycleException
	{
        // Preparation hook for batch task in case it wants to do anything to itself
        // before the subtasks are executed (e.g. adding subtasks or a parameter space)
	    aConfiguration.initialize(aContext);

        aContext.message("Initialized task ["+aConfiguration.getType()+"]");
        
        aConfiguration.analyze();
        
        aContext.message("Analyzed task configuration ["+aConfiguration.getType()+"]");

		try {
			aConfiguration.persist(aContext);
	        aContext.message("Persisted task configuration ["+aConfiguration.getType()+"]");
		}
		catch (IOException e) {
			throw new LifeCycleException(e);
		}
	}

	@Override
	public void begin(TaskContext aContext,
			Task aConfiguration)
	{
		for (int g = 0; g < 3; g++) {
			System.gc();
		}
		aContext.getMetadata().setStart(System.currentTimeMillis());
		aContext.message("Starting task ["+aConfiguration.getType()+"]");
	}

	@Override
	public void complete(TaskContext aContext, Task aConfiguration)
		throws LifeCycleException
	{
		aContext.getMetadata().setEnd(System.currentTimeMillis());
		aContext.message("Completing task ["+aConfiguration.getType()+"]");
		aContext.message("Running reports for task ["+aConfiguration.getType()+"]");
		List<Report> reports = new ArrayList<Report>(
				aConfiguration.getReports());
		int i = 1;
		for (Report report : reports) {
			for (int g = 0; g < 3; g++) {
				System.gc();
			}
			try {
				aContext.message("Starting report [" + report.getClass().getName() + "] (" + i + "/"
						+ reports.size() + ")");
				report.setContext(aContext);
				report.execute();
				aContext.message("Report complete [" + report.getClass().getName() + "] (" + i + "/"
						+ reports.size() + ")");
			}
			catch (Exception e) {
				aContext.error("Report failed [" + report.getClass().getName() + "] (" + i + "/"
						+ reports.size() + ")", e);
				throw new LifeCycleException(e);
			}
			finally {
				i++;
			}
		}
		
		// This is a critical file as it marks if a task has completed successfully or not. If
		// this file cannot be created properly, e.g. because the disk is full, then there will be
		// subsequent and hard to debug errors. Thus, if the file cannot be created properly, any
		// potentially incomplete version of this file has to be deleted.
		try {
			aContext.storeBinary(TaskContextMetadata.METADATA_KEY, aContext.getMetadata());
		}
		catch (Throwable e) {
			aContext.getStorageService().delete(aContext.getId(), TaskContextMetadata.METADATA_KEY);
			throw new LifeCycleException("Unable to write [" + TaskContextMetadata.METADATA_KEY
					+ "] to mark context as complete.", e);
		}
		aContext.message("Completed task ["+aConfiguration.getType()+"]");
	}

	@Override
	public void fail(TaskContext aContext, Task aConfiguration, Throwable aCause)
		throws LifeCycleException
	{
		try {
			aContext.getStorageService().delete(aContext.getId());
		}
		catch (DataAccessResourceFailureException e) {
			aContext.error("Unable to clean up context after failure. Some data may remain in " +
					"the context.", e);
		}
		aContext.error("Task failed ["+aConfiguration.getType()+"]", aCause);
	}

	@Override
	public void destroy(TaskContext aContext, Task aConfiguration)
	{
	    aConfiguration.destroy(aContext);
	    
        if (aConfiguration.isInitialized()) {
            throw new IllegalStateException(
                    "Task not destroyed. Maybe forgot to call super.destroy(ctx) in ["
                            + getClass().getName() + "]?");
        }
        
	    aContext.destroy();
		aContext.message("Shut down task");
	}
}
