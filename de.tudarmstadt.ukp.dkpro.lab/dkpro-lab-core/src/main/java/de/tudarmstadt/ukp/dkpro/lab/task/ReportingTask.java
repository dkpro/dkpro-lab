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
package de.tudarmstadt.ukp.dkpro.lab.task;

import de.tudarmstadt.ukp.dkpro.lab.engine.LifeCycleManager;
import de.tudarmstadt.ukp.dkpro.lab.reporting.Report;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.TaskBase;

/**
 * Task for executing only reports. Actually this task does nothing and relies on the
 * {@link LifeCycleManager} to execute the attached {@link Report}s.
 *
 * @author Richard Eckart de Castilho
 */
public class ReportingTask
	extends TaskBase
{
	// Nothing needs to be changed here
}
