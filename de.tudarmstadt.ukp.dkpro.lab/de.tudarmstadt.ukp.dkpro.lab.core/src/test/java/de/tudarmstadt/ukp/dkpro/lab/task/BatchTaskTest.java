/*******************************************************************************
 * Copyright 2012
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

import java.io.File;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import de.tudarmstadt.ukp.dkpro.lab.Lab;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.storage.filesystem.FileSystemStorageService;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.ExecutableTaskBase;

public class BatchTaskTest
{
	@Ignore("Currently does not run on Jenkins")
	@Test
	public void testNested() throws Exception
	{
		File repo = new File("target/repository/"+getClass().getSimpleName()+"/"+name.getMethodName());
		FileUtils.deleteDirectory(repo);
		repo.mkdirs();
		((FileSystemStorageService) Lab.getInstance().getStorageService()).setStorageRoot(repo);
		
		Dimension innerDim = Dimension.create("inner", "1", "2", "3");
		ParameterSpace innerPSpace = new ParameterSpace(innerDim);
		BatchTask innerTask = new BatchTask() {
			@Override
			public void setConfiguration(Map<String, Object> aConfig)
			{
				super.setConfiguration(aConfig);
				System.out.printf("A %10d %s %s%n", this.hashCode(), getType(), aConfig);
			}
		};
		innerTask.setParameterSpace(innerPSpace);
		innerTask.addTask(new ConfigDumperTask1());
		
		Dimension outerDim = Dimension.create("outer", "1", "2", "3");
		ParameterSpace outerPSpace = new ParameterSpace(outerDim);
		BatchTask outerTask = new BatchTask() {
			@Override
			public void setConfiguration(Map<String, Object> aConfig)
			{
				super.setConfiguration(aConfig);
				System.out.printf("B %10d %s %s%n", this.hashCode(), getType(), aConfig);
			}
		};
		outerTask.setParameterSpace(outerPSpace);
		outerTask.addTask(innerTask);
		outerTask.addTask(new ConfigDumperTask2());
		
		Lab.getInstance().run(outerTask);
	}
	
	public static class ConfigDumperTask1 extends ExecutableTaskBase implements ConfigurationAware
	{
		@Discriminator
		private String inner;
		
		private Map<String, Object> config;
		
		@Override
		public void execute(TaskContext aContext)
			throws Exception
		{
			System.out.printf("C %10d %s %s%n", this.hashCode(), getType(), config);
		}

		@Override
		public void setConfiguration(Map<String, Object> aConfig)
		{
			config = aConfig;
		}
	}

	public static class ConfigDumperTask2 extends ExecutableTaskBase implements ConfigurationAware
	{
		@Discriminator
		private String outer;
		
		private Map<String, Object> config;
		
		@Override
		public void execute(TaskContext aContext)
			throws Exception
		{
			System.out.printf("D %10d %s %s%n", this.hashCode(), getType(), config);
		}

		@Override
		public void setConfiguration(Map<String, Object> aConfig)
		{
			config = aConfig;
		}
	}


	@Rule
	public TestName name = new TestName();

	@Before
	public void printSeparator()
	{
		System.out.println("\n=== " + name.getMethodName() + " =====================");
	}
}
