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
package de.tudarmstadt.ukp.dkpro.lab.task.impl;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import de.tudarmstadt.ukp.dkpro.lab.task.BatchTask;
import de.tudarmstadt.ukp.dkpro.lab.task.Dimension;
import de.tudarmstadt.ukp.dkpro.lab.task.ParameterSpace;
import de.tudarmstadt.ukp.dkpro.lab.task.Task;

public class DefaultBatchTask
    extends TaskBase
    implements BatchTask
{
    protected Set<Task> tasks = new LinkedHashSet<Task>();
    private ParameterSpace parameterSpace;
    private ExecutionPolicy executionPolicy = ExecutionPolicy.RUN_AGAIN;
    private Map<String, Object> inheritedConfig;
    protected Set<String> inheritedScope;

    {
        // Just to make sure there is one run if no parameter space is set.
        parameterSpace = new ParameterSpace(Dimension.create("__DUMMY__", 1));
    }

    public void setParameterSpace(ParameterSpace aParameterSpace)
    {
        parameterSpace = aParameterSpace;
    }
    
    @Override
    public ParameterSpace getParameterSpace()
    {
        return parameterSpace;
    }

    public void setExecutionPolicy(ExecutionPolicy aPolicy)
    {
        executionPolicy = aPolicy;
    }
    
    @Override
    public ExecutionPolicy getExecutionPolicy()
    {
        return executionPolicy;
    }

    /**
     * Add a subtask to the batch. Unless otherwise mandated by data dependencies (imports) between
     * the subtasks, the added tasks are executed in the order they are added. This effect can be
     * used e.g. to execute a specific task before all other tasks even though the other tasks have
     * no data dependencies on the first task. Whenever a task needs to access data produced by
     * another tasks, you <b>must</b> import that data. The batch task will still try to execute
     * the tasks in the order they were added, but in case a data dependency for a task is not yet
     * available, the task is moved to the end of the queue.
     * 
     * @param aTask the task to be added.
     */
    public void addTask(Task aTask)
    {
        tasks.add(aTask);
    }

    @Override
    public Set<Task> getTasks()
    {
        return Collections.unmodifiableSet(tasks);
    }

    /**
     * Set the subtasks of this batch. If you depend on a specific exeuction order, you have to 
     * provide a set with a fixed iteration order here, e.g. an {@code LinkedHashSet}.
     */
    public void setTasks(Set<Task> aTasks)
    {
        tasks = new LinkedHashSet<Task>(aTasks);
    }

    @Override
    public void setConfiguration(Map<String, Object> aConfig)
    {
        parameterSpace.reset();
        inheritedConfig = aConfig;
    }
    
    @Override
    public Map<String, Object> getConfiguration()
    {
        return inheritedConfig;
    }

    @Override
    public void setScope(Set<String> aScope)
    {
        inheritedScope = aScope;
    }
    
    @Override
    public Set<String> getScope()
    {
        return inheritedScope;
    }
}
