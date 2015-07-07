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
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import de.tudarmstadt.ukp.dkpro.lab.Lab;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.DefaultBatchTask;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.ExecutableTaskBase;

public class DiscriminatorTest
{
	@Before
	public void setup()
	{
		String path = "target/repository/"+getClass().getSimpleName()+"/"+name.getMethodName();
		System.setProperty("DKPRO_HOME", new File(path).getAbsolutePath());
	}
	
	@Test
	public void testMap() throws Exception
	{
		Map<String, String> map1 = new HashMap<String, String>();
		map1.put("A", "89zsöoibca");
		map1.put("243jh1g45", "09z2#3ROj 	2r!]9832");

		Map<String, String> map2 = new HashMap<String, String>();
		map2.put("!&(B§V  §= ", "ü 8z9^2g3f9	xy");
		map2.put("!&§$ Ü!§$%ø⁄ª¨", "!$§ 240324 #");

		Dimension<Map<String, String>> dimMap = Dimension.create("map", map1, map2);
		
		ParameterSpace pSpace = new ParameterSpace(dimMap);
		
		DefaultBatchTask batch = new DefaultBatchTask();
		batch.setParameterSpace(pSpace);
		batch.addTask(new MapDiscriminatorTask());
		
		Lab.getInstance().run(batch);
	}
	
	public static class MapDiscriminatorTask extends ExecutableTaskBase
	{
		@Discriminator
		private Map<String, String> map;
		
		@Override
		public void execute(TaskContext aContext)
			throws Exception
		{
			// Do nothing
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
