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
package de.tudarmstadt.ukp.dkpro.lab;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.dao.DataAccessResourceFailureException;

import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContextFactory;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskExecutionService;
import de.tudarmstadt.ukp.dkpro.lab.logging.LoggingService;
import de.tudarmstadt.ukp.dkpro.lab.storage.StorageService;
import de.tudarmstadt.ukp.dkpro.lab.task.Task;
import de.tudarmstadt.ukp.dkpro.lab.task.TaskContextMetadata;

public class Lab
{
	public static final String DEFAULT_CONTEXT = "/META-INF/spring/context.xml";

//	{
//		java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");
//		Handler[] handlers = rootLogger.getHandlers();
//		for (int i = 0; i < handlers.length; i++) {
//			rootLogger.removeHandler(handlers[i]);
//		}
//		SLF4JBridgeHandler.install();
//	}

	private static Lab instance;

	private ApplicationContext context;

	public static synchronized Lab getInstance()
	{
		if (instance == null) {
			instance = newInstance(DEFAULT_CONTEXT);
		}
		return instance;
	}

	public static Lab newInstance(String aContext)
	{
		Lab lab = new Lab();
		lab.context = new ClassPathXmlApplicationContext(aContext, lab.getClass());
		return lab;
	}

	public TaskExecutionService getTaskExecutionService()
	{
		return (TaskExecutionService) context.getBean("TaskExecutionService");
	}

	public StorageService getStorageService()
	{
		return (StorageService) context.getBean("StorageService");
	}

	public LoggingService getLoggingService()
	{
		return (LoggingService) context.getBean("LoggingService");
	}

	public TaskContextFactory getTaskContextFactory()
	{
		return (TaskContextFactory) context.getBean("TaskContextFactory");
	}

	public void runAll(Task... aConfigurations)
		throws Exception
	{
		for (Task task : aConfigurations) {
			run(task);
		}
	}

	public String run(Task aConfiguration)
		throws Exception
	{
		return getTaskExecutionService().run(aConfiguration);
	}

	public String runAsking(Task aConfiguration)
		throws Exception
	{
		boolean found;
		TaskContextMetadata meta = null;
		try {
			meta = getStorageService().getLatestContext(aConfiguration.getType(),
					aConfiguration.getDescriminators());
			found = true;
		}
		catch (DataAccessResourceFailureException e) {
			found = false;
		}

		boolean execute = true;
		if (found) {
			InputStreamReader converter = new InputStreamReader(System.in);
			BufferedReader in = new BufferedReader(converter);
			String line = "";
			while (line != null) {
				System.out.println("[" + aConfiguration.getType() + "] has already been executed in" +
				" this configuration. Do you wish to execute it again? (y/n)");
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
		}

		if (execute) {
			return getTaskExecutionService().run(aConfiguration);
		}
		else {
			return meta.getId();
		}
	}
}
