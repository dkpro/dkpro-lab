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
package de.tudarmstadt.ukp.dkpro.lab.task;

import java.util.Map;
import java.util.Set;

public interface BatchTask
    extends Task, ConfigurationAware
{
    /**
     * The subtask context IDs produced by this batch task in the order of their production.
     */
    public static final String SUBTASKS_KEY = "Subtasks";
    
    public static enum ExecutionPolicy
    {
        USE_EXISTING, ASK_EXISTING, RUN_AGAIN
    }

    ParameterSpace getParameterSpace();

    Set<Task> getTasks();
    
    Map<String, Object> getConfiguration();
    
    void setScope(Set<String> aScope);
    
    Set<String> getScope();
    
    ExecutionPolicy getExecutionPolicy();
}
