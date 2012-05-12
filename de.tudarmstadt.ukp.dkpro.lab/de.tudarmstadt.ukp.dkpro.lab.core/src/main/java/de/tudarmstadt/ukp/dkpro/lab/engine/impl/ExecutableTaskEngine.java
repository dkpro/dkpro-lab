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

import de.tudarmstadt.ukp.dkpro.lab.engine.ExecutionException;
import de.tudarmstadt.ukp.dkpro.lab.engine.LifeCycleException;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContextFactory;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskExecutionEngine;
import de.tudarmstadt.ukp.dkpro.lab.task.ExecutableTask;
import de.tudarmstadt.ukp.dkpro.lab.task.Task;

public class ExecutableTaskEngine
	implements TaskExecutionEngine
{
	private TaskContextFactory contextFactory;

	@Override
	public String run(Task aConfiguration)
		throws ExecutionException, LifeCycleException
	{
		if (!(aConfiguration instanceof ExecutableTask)) {
			throw new ExecutionException("This engine can only execute ["
					+ ExecutableTask.class.getName() + "]");
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
				((ExecutableTask) aConfiguration).execute(ctx);
			}
			catch (Exception e) {
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
}
