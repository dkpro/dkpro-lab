/*******************************************************************************
 * Copyright 2015
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

import static de.tudarmstadt.ukp.dkpro.lab.engine.impl.ImportUtil.extractConstraints;
import static de.tudarmstadt.ukp.dkpro.lab.storage.StorageService.CONTEXT_ID_SCHEME;
import static de.tudarmstadt.ukp.dkpro.lab.storage.StorageService.LATEST_CONTEXT_SCHEME;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessResourceFailureException;

import de.tudarmstadt.ukp.dkpro.lab.ProgressMeter;
import de.tudarmstadt.ukp.dkpro.lab.Util;
import de.tudarmstadt.ukp.dkpro.lab.engine.ExecutionException;
import de.tudarmstadt.ukp.dkpro.lab.engine.LifeCycleException;
import de.tudarmstadt.ukp.dkpro.lab.engine.LifeCycleManager;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContextFactory;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskExecutionEngine;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskExecutionService;
import de.tudarmstadt.ukp.dkpro.lab.logging.LoggingService;
import de.tudarmstadt.ukp.dkpro.lab.storage.StorageService;
import de.tudarmstadt.ukp.dkpro.lab.storage.TaskContextNotFoundException;
import de.tudarmstadt.ukp.dkpro.lab.storage.UnresolvedImportException;
import de.tudarmstadt.ukp.dkpro.lab.storage.impl.PropertiesAdapter;
import de.tudarmstadt.ukp.dkpro.lab.task.BatchTask;
import de.tudarmstadt.ukp.dkpro.lab.task.Dimension;
import de.tudarmstadt.ukp.dkpro.lab.task.FixedSizeDimension;
import de.tudarmstadt.ukp.dkpro.lab.task.ParameterSpace;
import de.tudarmstadt.ukp.dkpro.lab.task.Task;
import de.tudarmstadt.ukp.dkpro.lab.task.TaskContextMetadata;
import de.tudarmstadt.ukp.dkpro.lab.task.TaskFactory;

public class BatchTaskEngine
    implements TaskExecutionEngine
{    
    private TaskContextFactory contextFactory;

    private final Log log = LogFactory.getLog(getClass());

    /**
     * The subtask context IDs produced by this batch task in the order of their production.
     */
    public static final String SUBTASKS_KEY = "Subtasks";

    @Override
    public String run(Task aConfiguration)
        throws ExecutionException, LifeCycleException
    {
        if (!(aConfiguration instanceof BatchTask)) {
            throw new ExecutionException("This engine can only execute ["
                    + BatchTask.class.getName() + "]");
        }

        // Create persistence service for injection into analysis components
        TaskContext ctx = null;
        try {
            ctx = contextFactory.createContext(aConfiguration);

            // Now the setup is complete
            ctx.getLifeCycleManager().initialize(ctx, aConfiguration);

            // Start recording
            ctx.getLifeCycleManager().begin(ctx, aConfiguration);

            try {
                BatchTask cfg = (BatchTask) aConfiguration;
                ParameterSpace parameterSpace = cfg.getParameterSpace();
                
                // Try to calculate the parameter space size.
                int estimatedSize = 1;
                for (Dimension<?> d : parameterSpace.getDimensions()) {
                    if (d instanceof FixedSizeDimension) {
                        FixedSizeDimension fsd = (FixedSizeDimension) d;
                        if (fsd.size() > 0) {
                            estimatedSize *= fsd.size();
                        }
                    }
                }

                // A subtask execution may apply to multiple parameter space coordinates!
                Set<String> executedSubtasks = new LinkedHashSet<String>();
                
                ProgressMeter progress = new ProgressMeter(estimatedSize);
                for (Map<String, Object> config : parameterSpace) {
                    if (cfg.getConfiguration() != null) {
                        for (Entry<String, Object> e : cfg.getConfiguration().entrySet()) {
                            if (!config.containsKey(e.getKey())) {
                                config.put(e.getKey(), e.getValue());
                            }
                        }
                    }
                    
                    log.info("== Running new configuration [" + ctx.getId() + "] ==");
                    List<String> keys = new ArrayList<String>(config.keySet());
                    for (String key : keys) {
                        log.info("[" + key + "]: ["
                                + StringUtils.abbreviateMiddle(Util.toString(config.get(key)), "…", 150)
                                + "]");
                    }
                    
                    executeConfiguration(cfg, ctx, config, executedSubtasks);

                    progress.next();
                    log.info("Completed configuration " + progress);
                }

                // Set the subtask property and persist again, so the property is available to reports
                cfg.setAttribute(SUBTASKS_KEY, executedSubtasks.toString());
                cfg.persist(ctx);
            }
            catch (LifeCycleException e) {
                ctx.getLifeCycleManager().fail(ctx, aConfiguration, e);
                throw e;
            }
            catch (UnresolvedImportException e) {
                // HACK - pass unresolved import exceptions up to the outer batch task
                ctx.getLifeCycleManager().fail(ctx, aConfiguration, e);
                throw e;
            }
            catch (Throwable e) {
                ctx.getLifeCycleManager().fail(ctx, aConfiguration, e);
                throw new ExecutionException(e);
            }

            // End recording (here the reports will nbe done)
            ctx.getLifeCycleManager().complete(ctx, aConfiguration);

            return ctx.getId();
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
    
    /**
     * Locate the latest task execution compatible with the given task configuration.
     * 
     * @param aContext
     *            the context of the current batch task.
     * @param aConfig
     *            the current parameter configuration.
     * @param aExecutedSubtasks
     *            already executed subtasks.
     */
    protected void executeConfiguration(BatchTask aConfiguration, TaskContext aContext,
            Map<String, Object> aConfig, Set<String> aExecutedSubtasks)
        throws ExecutionException, LifeCycleException
    {
        if (log.isTraceEnabled()) {
            // Show all subtasks executed so far
            for (String est : aExecutedSubtasks) {
                log.trace("-- Already executed: " + est);
            }
        }

        // Set up initial scope used by sub-batch-tasks using the inherited scope. The scope is
        // extended as the subtasks of this batch are executed with the present configuration.
        // FIXME: That means that sub-batch-tasks in two different configurations cannot see
        // each other. Is that intended? Mind that the "executedSubtasks" set is intentionally
        // maintained *across* configurations, so maybe the scope should also be maintained
        // *across* configurations? - REC 2014-06-15
        Set<String> scope = new HashSet<String>();
        if (aConfiguration.getScope() != null) {
            scope.addAll(aConfiguration.getScope());
        }

        // Configure subtasks
        for (Task task : aConfiguration.getTasks()) {
            TaskFactory.configureTask(task, aConfig);
        }

        Queue<Task> queue = new LinkedList<Task>(aConfiguration.getTasks());
        Set<Task> loopDetection = new HashSet<Task>();

        List<UnresolvedImportException> deferralReasons = new ArrayList<UnresolvedImportException>();
        while (!queue.isEmpty()) {
            Task task = queue.poll();

            try {
                // Check if a subtask execution compatible with the present configuration has
                // does already exist ...
                TaskContextMetadata execution = getExistingExecution(aConfiguration, aContext,
                        task, aConfig, aExecutedSubtasks);
                if (execution == null) {
                    // ... otherwise execute it with the present configuration
                    log.info("Executing task [" + task.getType() + "]");
                    
                    // set scope here so that the inherited scopes are considered 
                    // set scope here so that tasks added to scope in this loop are considered
                    if (task instanceof BatchTask) {
                        ((BatchTask) task).setScope(scope);
                    }
                    
                    execution = runNewExecution(aContext, task, aConfig, aExecutedSubtasks);
                }                    
                else {
                    log.debug("Using existing execution [" + execution.getId() + "]");
                }
                
                // Record new/existing execution
                aExecutedSubtasks.add(execution.getId());
                scope.add(execution.getId());
                loopDetection.clear();
                deferralReasons.clear();
            }
            catch (UnresolvedImportException e) {
                // Add task back to queue
                log.debug("Deferring execution of task [" + task.getType() + "]: "
                        + e.getMessage());
                queue.add(task);
                
                // Detect endless loop
                if (loopDetection.contains(task)) {
                    StringBuilder details = new StringBuilder();
                    for (UnresolvedImportException r : deferralReasons) {
                        details.append("\n -");
                        details.append(r.getMessage());
                    }

                    // throw an UnresolvedImportException in case there is an outer BatchTask which needs to be executed first 
                    throw new UnresolvedImportException(e, details.toString());
                }
                
                // Record failed execution
                loopDetection.add(task);
                deferralReasons.add(e);
            }
        }
    }
    
    /**
     * Locate the latest task execution compatible with the given task configuration.
     * 
     * @param aContext
     *            the context of the current batch task.
     * @param aType
     *            the type of the task context to find.
     * @param aDiscriminators
     *            the discriminators of the task context to find.
     * @param aConfig
     *            the current parameter configuration.
     * @throws TaskContextNotFoundException
     *             if a matching task context could not be found.
     * @see ImportUtil#matchConstraints(Map, Map, boolean)
     */
    private TaskContextMetadata getLatestExecution(TaskContext aContext, String aType,
            Map<String, String> aDiscriminators, Map<String, Object> aConfig)
    {
        // Convert parameter values to strings
        Map<String, String> config = new HashMap<String, String>();
        for (Entry<String, Object> e : aConfig.entrySet()) {
            config.put(e.getKey(), Util.toString(e.getValue()));
        }

        StorageService storage = aContext.getStorageService();
        List<TaskContextMetadata> metas = storage.getContexts(aType, aDiscriminators);
        for (TaskContextMetadata meta : metas) {
            Map<String, String> discriminators = storage.retrieveBinary(meta.getId(),
                    Task.DISCRIMINATORS_KEY, new PropertiesAdapter()).getMap();
            // Check if the task is compatible with the current configuration. To do this, we
            // interpret the discriminators as constraints on the current configuration.
            if (ImportUtil.matchConstraints(discriminators, config, false)) {
                return meta;
            }
        }
        throw ImportUtil.createContextNotFoundException(aType, aDiscriminators);

    }

    /**
     * Execute the given task with the given task configuration.
     * 
     * @param aContext
     *            the context of the current batch task.
     * @param aTask
     *            the the task whose task to be executed.
     * @param aConfig
     *            the current parameter configuration.
     * @return the context meta data.
     */
    protected TaskContextMetadata runNewExecution(TaskContext aContext, Task aTask, Map<String, Object> aConfig,
            Set<String> aScope)
        throws ExecutionException, LifeCycleException
    {
        TaskExecutionService execService = aContext.getExecutionService();
        TaskExecutionEngine engine = execService.createEngine(aTask);
        engine.setContextFactory(new ScopedTaskContextFactory(execService
                .getContextFactory(), aConfig, aScope));
        String uuid = engine.run(aTask);
        return aContext.getStorageService().getContext(uuid);
    }
    
    /**
     * Locate the latest task execution compatible with the given task configuration.
     * 
     * @param aContext
     *            the context of the current batch task.
     * @param aTask
     *            the the task whose task context should be found.
     * @param aConfig
     *            the current parameter configuration.
     * @return {@code null} if the context could not be found.
     */
    protected TaskContextMetadata getExistingExecution(BatchTask aConfiguration,
            TaskContext aContext, Task aTask, Map<String, Object> aConfig, Set<String> aScope)
    {
        // Batch tasks are always run again since we do not store discriminators for them
        if (aTask instanceof BatchTask) {
            return null;
        }

        try {
            TaskContextMetadata meta = getLatestExecution(aContext, aTask.getType(),
                    aTask.getDescriminators(), aConfig);

            // If the task was already executed within the scope of this aggregate, do not execute
            // it again. Catching this here saves us from running tasks with the same configuration
            // more than once per aggregate.
            if (aScope.contains(meta.getId())) {
                return meta;
            }

            switch (aConfiguration.getExecutionPolicy()) {
            case RUN_AGAIN:
                // Always run the task again
                return null;
            case USE_EXISTING:
                // If the task was ever executed, do not run it again.
                return meta;
            case ASK_EXISTING:
                if (ask(meta)) {
                    // Execute again - act as if the context was not found
                    return null;
                }
                else {
                    // Use existing context
                    return meta;
                }
            default:
                throw new IllegalStateException("Unknown executionPolicy ["
                        + aConfiguration.getExecutionPolicy() + "]");
            }
        }
        catch (TaskContextNotFoundException e) {
            // Task context not found in storage
            return null;
        }
    }

    private boolean ask(TaskContextMetadata aMeta)
    {
        try {
            boolean execute = true;
            InputStreamReader converter = new InputStreamReader(System.in);
            BufferedReader in = new BufferedReader(converter);
            String line = "";
            while (line != null) {
                System.out.println("\n\n[" + aMeta.getType() + "] has already been executed in"
                        + " this configuration. Do you wish to execute it again? (y/n)");
                line = in.readLine().toLowerCase();
                if ("y".equals(line)) {
                    execute = true;
                    break;
                }
                if ("n".equals(line)) {
                    execute = false;
                    break;
                }
            }
            return execute;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected class ScopedTaskContextFactory
        extends DefaultTaskContextFactory
    {
        private final DefaultTaskContextFactory contextFactory;
        private final Map<String, Object> config;
        private final Set<String> scope;

        public ScopedTaskContextFactory(TaskContextFactory aContextFactory,
                Map<String, Object> aConfig, Set<String> aScope)
        {
            contextFactory = (DefaultTaskContextFactory) aContextFactory;
            config = aConfig;
            scope = aScope;
        }

        @Override
        protected TaskContext createContext(TaskContextMetadata aMetadata)
        {
            ScopedTaskContext ctx = new ScopedTaskContext(contextFactory);
            ctx.setExecutionService(getExecutionService());
            ctx.setLifeCycleManager(getLifeCycleManager());
            ctx.setStorageService(getStorageService());
            ctx.setLoggingService(getLoggingService());
            ctx.setMetadata(aMetadata);
            ctx.setConfig(config);
            ctx.setScope(scope);
            return ctx;
        }

        @Override
        public void registerContext(TaskContext aContext)
        {
            contextFactory.registerContext(aContext);
        }

        @Override
        public void unregisterContext(TaskContext aContext)
        {
            contextFactory.unregisterContext(aContext);
        }

        @Override
        public String getId()
        {
            return contextFactory.getId();
        }

        @Override
        public LifeCycleManager getLifeCycleManager()
        {
            return contextFactory.getLifeCycleManager();
        }

        @Override
        public LoggingService getLoggingService()
        {
            return contextFactory.getLoggingService();
        }

        @Override
        public StorageService getStorageService()
        {
            return contextFactory.getStorageService();
        }

        @Override
        public TaskExecutionService getExecutionService()
        {
            return contextFactory.getExecutionService();
        }
    }

    private class ScopedTaskContext
        extends DefaultTaskContext
    {
        private Map<String, Object> config;
        private Set<String> scope;

        public ScopedTaskContext(TaskContextFactory aOwner)
        {
            super(aOwner);
        }

        public void setConfig(Map<String, Object> aConfig)
        {
            config = aConfig;
        }

        public void setScope(Set<String> aScope)
        {
            scope = aScope;
        }

        @Override
        public TaskContextMetadata resolve(URI aUri)
        {
            TaskContextMetadata meta;
            StorageService storage = getStorageService();
            if (LATEST_CONTEXT_SCHEME.equals(aUri.getScheme())) {
                Map<String, String> constraints = extractConstraints(aUri);
                try {
                    meta = getLatestExecution(this, aUri.getAuthority(), constraints, config);
                }
                catch (TaskContextNotFoundException e) {
                    throw new UnresolvedImportException(this, aUri.toString(), e);
                }
            }
            else if (CONTEXT_ID_SCHEME.equals(aUri.getScheme())) {
                try {
                    meta = storage.getContext(aUri.getAuthority());
                }
                catch (TaskContextNotFoundException e) {
                    throw new UnresolvedImportException(this, aUri.toString(), e);
                }
            }
            else {
                throw new DataAccessResourceFailureException("Unknown scheme in import [" + aUri
                        + "]");
            }

            if (!scope.contains(meta.getId())) {
                throw new UnresolvedImportException(this, aUri.toString(), "Resolved context ["
                        + meta.getId() + "] not in scope " + scope);
            }

            return meta;
        }
    }
}
