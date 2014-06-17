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
package de.tudarmstadt.ukp.dkpro.lab.engine;

import de.tudarmstadt.ukp.dkpro.lab.task.Task;

/**
 * Exceptions thrown during the main execution step of a {@link Task} that is being executed in a
 * {@link TaskExecutionEngine}.
 *
 * @author Richard Eckart de Castilho
 */
public class ExecutionException
	extends Exception
{
	private static final long serialVersionUID = -6251644756208619215L;

	public ExecutionException(String aMessage)
	{
		super(aMessage);
	}

	public ExecutionException(Throwable aCause)
	{
		super(aCause);
	}

	public ExecutionException(String aMessage, Throwable aCause)
	{
		super(aMessage, aCause);
	}
}
