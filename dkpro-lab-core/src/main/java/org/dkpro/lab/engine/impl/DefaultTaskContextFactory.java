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

import static org.dkpro.lab.storage.StorageService.CONTEXT_ID_SCHEME;
import static org.dkpro.lab.storage.StorageService.LATEST_CONTEXT_SCHEME;

import java.net.URI;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dkpro.lab.conversion.ConversionService;
import org.dkpro.lab.engine.LifeCycleManager;
import org.dkpro.lab.engine.TaskContext;
import org.dkpro.lab.engine.TaskContextFactory;
import org.dkpro.lab.engine.TaskExecutionService;
import org.dkpro.lab.logging.LoggingService;
import org.dkpro.lab.storage.StorageService;
import org.dkpro.lab.storage.UnresolvedImportException;
import org.dkpro.lab.task.Task;
import org.dkpro.lab.task.TaskContextMetadata;
import org.safehaus.uuid.UUIDGenerator;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;

public class DefaultTaskContextFactory
    implements BeanNameAware, TaskContextFactory
{
    private final Log log = LogFactory.getLog(getClass());

    private final Map<String, TaskContext> contexts;
    private StorageService storageService;
    private LoggingService loggingService;
    private ConversionService conversionService;
    private LifeCycleManager lifeCycleManager;
    private TaskExecutionService executionService;

    private String beanName;

    @Value(value = "${context-id-pattern}")
    private String contextIdPattern;
    
    {
        contexts = new ConcurrentHashMap<String, TaskContext>();
    }

    @Override
    public void destroyContext(TaskContext aExperimentContext)
    {
        unregisterContext(aExperimentContext);
    }

    @Override
    public TaskContext getContext(String aInstanceId)
    {
        if (contexts.containsKey(aInstanceId)) {
            return contexts.get(aInstanceId);
        }
        else if (getStorageService().containsContext(aInstanceId)) {
            TaskContextMetadata metadata = getStorageService().getContext(aInstanceId);
            TaskContext ctx = createContext(metadata);
            contexts.put(ctx.getId(), ctx);
            return ctx;
        }
        else {
            return null;
        }
    }

    @Override
    public TaskContext createContext(Task aConfiguration)
    {
        TaskContextMetadata metadata = new TaskContextMetadata();
        metadata.setId(nextId(aConfiguration));
        metadata.setType(aConfiguration.getType());
        metadata.setImports(aConfiguration.getImports());

        TaskContext ctx = createContext(metadata);
        resolveImports(ctx);

        registerContext(ctx);
        return ctx;
    }

    @Override
    public String getId()
    {
        return beanName;
    }

    public void registerContext(TaskContext aContext)
    {
        contexts.put(aContext.getId(), aContext);
    }

    public void unregisterContext(TaskContext aContext)
    {
        contexts.remove(aContext.getId());
    }

    /**
     * This can be overwritten by subclasses to create different {@link TaskContext}
     * implementations.
     */
    protected TaskContext createContext(TaskContextMetadata aMetadata)
    {
        DefaultTaskContext ctx = new DefaultTaskContext(this);
        ctx.setLifeCycleManager(getLifeCycleManager());
        ctx.setStorageService(getStorageService());
        ctx.setLoggingService(getLoggingService());
        ctx.setConversionService(getConversionService());
        ctx.setExecutionService(getExecutionService());
        ctx.setMetadata(aMetadata);
        return ctx;
    }

    protected void resolveImports(TaskContext aContext)
    {
        for (Entry<String, String> e : aContext.getMetadata().getImports().entrySet()) {
            URI uri = URI.create(e.getValue());
            // Try resolving by type
            if (LATEST_CONTEXT_SCHEME.equals(uri.getScheme())
                    || CONTEXT_ID_SCHEME.equals(uri.getScheme())) {
                String uuid;
                uuid = aContext.resolve(uri).getId();
                if (!getStorageService().containsKey(uuid, uri.getPath())) {
                    throw new UnresolvedImportException(aContext, e.getKey(), e.getValue(),
                            "Key not found");
                }

                String resolvedUri = CONTEXT_ID_SCHEME + "://" + uuid + uri.getPath();
                log.debug("Resolved import [" + e.getValue() + "] -> [" + resolvedUri + "]");
                e.setValue(resolvedUri);
            }
        }
    }

    protected String nextId(Task aConfiguration)
    {
        String shortName = aConfiguration.getType();
        if (shortName.lastIndexOf('.') > -1) {
            shortName = shortName.substring(shortName.lastIndexOf('.') + 1);
        }
        
        String uuid = UUIDGenerator.getInstance().generateTimeBasedUUID().toString();

        String time = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
        
        return MessageFormat.format(contextIdPattern, shortName, time, uuid);
    }

    @Override
    public void setBeanName(String aName)
    {
        try {
            if (beanName != null) {
                StaticContext.unbind(beanName);
            }
            beanName = aName;
            if (beanName != null) {
                StaticContext.bind(beanName, this);
            }
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Required
    public void setLifeCycleManager(LifeCycleManager aLifeCycleManager)
    {
        lifeCycleManager = aLifeCycleManager;
    }

    public LifeCycleManager getLifeCycleManager()
    {
        return lifeCycleManager;
    }

    @Required
    public void setStorageService(StorageService aStorageService)
    {
        storageService = aStorageService;
    }

    public StorageService getStorageService()
    {
        return storageService;
    }

    @Required
    public void setConversionService(ConversionService aConversionService)
    {
        conversionService = aConversionService;
    }
    
    public ConversionService getConversionService()
    {
        return conversionService;
    }

    @Required
    public void setLoggingService(LoggingService aLoggingService)
    {
        loggingService = aLoggingService;
    }

    public LoggingService getLoggingService()
    {
        return loggingService;
    }

    @Required
    public void setExecutionService(TaskExecutionService aExecutionService)
    {
        executionService = aExecutionService;
    }

    public TaskExecutionService getExecutionService()
    {
        return executionService;
    }
}
