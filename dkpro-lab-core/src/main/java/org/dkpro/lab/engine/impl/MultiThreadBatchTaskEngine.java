/*
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
 */
package org.dkpro.lab.engine.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dkpro.lab.engine.ExecutionException;
import org.dkpro.lab.engine.LifeCycleException;
import org.dkpro.lab.engine.TaskContext;
import org.dkpro.lab.engine.TaskExecutionEngine;
import org.dkpro.lab.engine.TaskExecutionService;
import org.dkpro.lab.storage.UnresolvedImportException;
import org.dkpro.lab.task.BatchTask;
import org.dkpro.lab.task.Task;
import org.dkpro.lab.task.TaskContextMetadata;
import org.springframework.beans.factory.annotation.Value;

public class MultiThreadBatchTaskEngine
    extends BatchTaskEngine
{
    private final Log log = LogFactory.getLog(getClass());
    
    public static final String PROP_THREADS = "engine.batch.maxThreads";
    
    @Value("#{ @Properties['" + PROP_THREADS + "'] }")
    private int maxThreads = Runtime.getRuntime().availableProcessors() - 1;
    
    /**
     * Explicit no-args constructor
     */
    public MultiThreadBatchTaskEngine()
    {
        // Nothing to do.
    }
    
    /**
     * Constructor with number of threads.
     * 
     * @param aNThreads
     *            The number of threads to use for the MultiThreadBatchTask.
     */
    public MultiThreadBatchTaskEngine(int aNThreads)
    {
        setMaxThreads(aNThreads);
    }

    public void setMaxThreads(int aNThreads)
    {
        maxThreads = aNThreads;
    }

    @Override
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
        Set<String> scope = new HashSet<>();
        if (aConfiguration.getScope() != null) {
            scope.addAll(aConfiguration.getScope());
        }

        // Configure subtasks
        for (Task task : aConfiguration.getTasks()) {
            // Now the setup is complete
            aContext.getLifeCycleManager().configure(aContext, task, aConfig);
        }

        Queue<Task> queue = new LinkedList<>(aConfiguration.getTasks());
        // keeps track of the execution threads; 
        // TODO MW: do we really need this or can we work with the futures list only?
        Map<Task, ExecutionThread> threads = new HashMap<>();              
        // keeps track of submitted Futures and their associated tasks
        Map<Future<?>, Task> futures = new HashMap<Future<?>, Task>();     
        // will be instantiated with all exceptions from current loop
        ConcurrentMap<Task, Throwable> exceptionsFromLastLoop = null;      
        ConcurrentMap<Task, Throwable> exceptionsFromCurrentLoop = new ConcurrentHashMap<>();

        int outerLoopCounter = 0;

        // main loop
        do {
            outerLoopCounter++;

            threads.clear();
            futures.clear();
            ExecutorService executor = Executors.newFixedThreadPool(maxThreads);

            // set the exceptions from the last loop
            exceptionsFromLastLoop = new ConcurrentHashMap<>(exceptionsFromCurrentLoop);

            // Fix MW: Clear exceptionsFromCurrentLoop; otherwise the loop with run at most twice.
            exceptionsFromCurrentLoop.clear();
            
            // process all tasks from the queue
            while (!queue.isEmpty()) {
                Task task = queue.poll();

                TaskContextMetadata execution = getExistingExecution(aConfiguration, aContext,
                        task, aConfig, aExecutedSubtasks);

                // Check if a subtask execution compatible with the present configuration has
                // does already exist ...
                if (execution == null) {
                    // ... otherwise execute it with the present configuration
                    log.info("Executing task [" + task.getType() + "]");

                    // set scope here so that the inherited scopes are considered
                    if (task instanceof BatchTask) {
                        ((BatchTask) task).setScope(scope);
                    }

                    ExecutionThread thread = new ExecutionThread(aContext, task, aConfig,
                            aExecutedSubtasks);
                    threads.put(task, thread);

                    futures.put(executor.submit(thread), task);
                }
                else {
                    log.debug("Using existing execution [" + execution.getId() + "]");

                    // Record new/existing execution
                    aExecutedSubtasks.add(execution.getId());
                    scope.add(execution.getId());
                }
            }

            // try and get results from all futures to check for failed executions
            for(Map.Entry<Future<?>, Task> entry : futures.entrySet()){
                try {
                    entry.getKey().get();
                }
                catch(java.util.concurrent.ExecutionException ex) {
                    Task task = entry.getValue();
                    // TODO MW: add a retry-counter here to prevent endless loops?
                    log.info("Task exec failed for [" + task.getType() + "]");
                    // record the failed task, so that it can be re-added to the queue
                    exceptionsFromCurrentLoop.put(task, ex);
                }
                catch(InterruptedException ex){
                    // thread interrupted, exit
                    throw new RuntimeException(ex);
                }
            }

            log.debug("Calling shutdown");
            executor.shutdown();
            log.debug("All threads finished");

            // collect the results
            for (Map.Entry<Task, ExecutionThread> entry : threads.entrySet()) {
                Task task = entry.getKey();
                ExecutionThread thread = entry.getValue();
                TaskContextMetadata execution = thread.getTaskContextMetadata();

                // probably failed
                if (execution == null) {
                    Throwable exception = exceptionsFromCurrentLoop.get(task);
                    if (!(exception instanceof UnresolvedImportException)
                            && !(exception instanceof java.util.concurrent.ExecutionException)) {
                        throw new RuntimeException(exception);
                    }
                    exceptionsFromCurrentLoop.put(task, exception);

                    // re-add to the queue
                    queue.add(task);
                }
                else {

                    // Record new/existing execution
                    aExecutedSubtasks.add(execution.getId());
                    scope.add(execution.getId());
                }
            }

        }
        // finish if the same tasks failed again
        while (!exceptionsFromCurrentLoop.keySet().equals(exceptionsFromLastLoop.keySet())); 
        // END OF DO; finish if the same tasks failed again

        if (!exceptionsFromCurrentLoop.isEmpty()) {
            // collect all details
            StringBuilder details = new StringBuilder();
            for (Throwable throwable : exceptionsFromCurrentLoop.values()) {
                details.append("\n -");
                details.append(throwable.getMessage());
            }

            // we re-throw the first exception
            Throwable next = exceptionsFromCurrentLoop.values().iterator().next();
            if (next instanceof RuntimeException) {
                throw (RuntimeException) next;
            }

            // otherwise wrap it
            throw new RuntimeException(details.toString(), next);
        }
        log.info("MultiThreadBatchTask completed successfully. Total number of outer loop runs: "
                + outerLoopCounter);
    }

    /**
     * Represents a task's execution thread,
     * together with the associated context, config and scope.
     */
    protected class ExecutionThread
            extends Thread
    {

        private final TaskContext aContext;
        private final Task task;
        private final Map<String, Object> aConfig;
        private final Set<String> scope;

        private TaskContextMetadata taskContextMetadata;

        public ExecutionThread(TaskContext aContext, Task aTask, Map<String, Object> aConfig,
                Set<String> aScope)
        {
            this.aContext = aContext;
            this.task = aTask;
            this.aConfig = aConfig;
            this.scope = aScope;
        }

        @Override public void run()
        {
            TaskExecutionService execService = aContext.getExecutionService();
            TaskExecutionEngine engine = execService.createEngine(task);
            engine.setContextFactory(new ScopedTaskContextFactory(execService
                    .getContextFactory(), aConfig, scope));
            String uuid;
            try {
                uuid = engine.run(task);
            }
            catch (ExecutionException | LifeCycleException e) {
                throw new RuntimeException(e);
            }

            taskContextMetadata = aContext.getStorageService().getContext(uuid);
        }

        /**
         * Returns the result of the run.
         */
        public TaskContextMetadata getTaskContextMetadata()
        {
            return taskContextMetadata;
        }
    }
}
