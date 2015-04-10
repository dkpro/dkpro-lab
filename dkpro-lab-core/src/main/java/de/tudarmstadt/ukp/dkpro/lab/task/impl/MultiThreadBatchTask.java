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
package de.tudarmstadt.ukp.dkpro.lab.task.impl;

import de.tudarmstadt.ukp.dkpro.lab.engine.*;
import de.tudarmstadt.ukp.dkpro.lab.engine.ExecutionException;
import de.tudarmstadt.ukp.dkpro.lab.task.Task;
import de.tudarmstadt.ukp.dkpro.lab.task.TaskContextMetadata;
import de.tudarmstadt.ukp.dkpro.lab.task.TaskFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author Ivan Habernal
 */
public class MultiThreadBatchTask
        extends BatchTask
{
    private final Log log = LogFactory.getLog(getClass());

    protected void executeConfiguration(TaskContext aContext, Map<String, Object> aConfig,
            Set<String> aExecutedSubtasks)
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
        if (inheritedScope != null) {
            scope.addAll(inheritedScope);
        }

        // Configure subtasks
        for (Task task : tasks) {
            TaskFactory.configureTask(task, aConfig);
        }

        Queue<Task> queue = new LinkedList<>(tasks);
        //        Set<Task> loopDetection = new HashSet<>();
        //        List<UnresolvedImportException> deferralReasons = new ArrayList<>();

        ConcurrentMap<Task, Throwable> exceptionsFromLastLoop;
        ConcurrentMap<Task, Throwable> exceptionsFromCurrentLoop = new ConcurrentHashMap<>();

        ThreadPoolExecutorFactoryBean factory = new ThreadPoolExecutorFactoryBean();
        factory.setCorePoolSize(4);

        // main loop
        do {
            Map<Task, ExecutionThread> threads = new HashMap<>();

//            ExecutorService executor = Executors.newFixedThreadPool(2);

            // set the exceptions from the last loop
            exceptionsFromLastLoop = new ConcurrentHashMap<>(exceptionsFromCurrentLoop);

            // process all tasks from the queue
            while (!queue.isEmpty()) {
                Task task = queue.poll();

                TaskContextMetadata execution = getExistingExecution(aContext, task, aConfig,
                        aExecutedSubtasks);

                // Check if a subtask execution compatible with the present configuration has
                // does already exist ...
                if (execution == null) {
                    // ... otherwise execute it with the present configuration
                    log.info("Executing task [" + task.getType() + "]");

                    // set scope here so that the inherited scopes are considered
                    // set scope here so that tasks added to scope in this loop are considered
                    if (task instanceof BatchTask) {
                        ((BatchTask) task).setScope(scope);
                    }

                    //                    try {
                    //                    execution = runNewExecution(aContext, task, aConfig, aExecutedSubtasks);


//                    ExecutionThread thread = (ExecutionThread) factory.newThread(new ExecutionThread(aContext, task, aConfig,
//                            aExecutedSubtasks));

                    ExecutionThread thread = new ExecutionThread(aContext, task, aConfig,
                            aExecutedSubtasks);

                    TaskUncaughtExceptionHandler exceptionHandler = new TaskUncaughtExceptionHandler(
                            exceptionsFromCurrentLoop, task);
                    thread.setUncaughtExceptionHandler(exceptionHandler);

                    threads.put(task, thread);

                    // TODO xxx
                                        thread.start();
//                    executor.execute(thread);

                    //                    // Record new/existing execution
                    //                    aExecutedSubtasks.add(execution.getId());
                    //                    scope.add(execution.getId());
                    //                    loopDetection.clear();
                    //                    deferralReasons.clear();
                    //                    }
                    //                    catch (UnresolvedImportException e) {
                    //                        Add task back to queue
                    //                        log.debug("Deferring execution of task [" + task.getType() + "]: "
                    //                                + e.getMessage());
                    //                        queue.add(task);
                    //
                    // Detect endless loop
                    //                        if (loopDetection.contains(task)) {
                    //                            StringBuilder details = new StringBuilder();
                    //                            for (UnresolvedImportException r : deferralReasons) {
                    //                                details.append("\n -");
                    //                                details.append(r.getMessage());
                    //                            }
                    //
                    //                            throw an UnresolvedImportException in case there is an outer BatchTask which needs to be executed first
                    //                            throw new UnresolvedImportException(e, details.toString());
                    //                        }
                    // Record failed execution
                    //                        loopDetection.add(task);
                    //                        deferralReasons.add(e);
                    //                    }
                }
                else {
                    log.debug("Using existing execution [" + execution.getId() + "]");

                    // Record new/existing execution
                    aExecutedSubtasks.add(execution.getId());
                    scope.add(execution.getId());
                    //                    loopDetection.clear();
                    //                    deferralReasons.clear();
                }
            }


            // TODO xxx
//            executor.shutdown();
//            while (!executor.isTerminated()) {
//                empty
//            }

            // wait for completing all threads
            for (ExecutionThread thread : threads.values()) {
                try {
                    thread.join();
                }
                catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }

            // empty the queue
            queue.clear();

            System.out.println("All threads finished");

            // collect the results
            for (Map.Entry<Task, ExecutionThread> entry : threads.entrySet()) {
                ExecutionThread thread = entry.getValue();
                Task task = entry.getKey();
                TaskContextMetadata execution = thread.getTaskContextMetadata();

                // probably failed
                if (execution == null) {
                    Throwable exception = exceptionsFromCurrentLoop.get(task);
                    exceptionsFromCurrentLoop.put(task, exception);

                    // put it to the queue
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
    }

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

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    protected class TaskUncaughtExceptionHandler
            implements Thread.UncaughtExceptionHandler

    {
        private final ConcurrentMap<Task, Throwable> thrownExceptions;
        private final Task task;

        public TaskUncaughtExceptionHandler(ConcurrentMap<Task, Throwable> resultingExceptionMap,
                Task task)
        {
            this.thrownExceptions = resultingExceptionMap;
            this.task = task;
        }

        @Override
        public void uncaughtException(Thread t, Throwable e)
        {
            System.err.println(
                    "Task: " + task.getClass().getSimpleName() + " in thread " + t.getId()
                            + " threw an exception: " + e.toString());
            thrownExceptions.put(task, e);
        }
    }
}
