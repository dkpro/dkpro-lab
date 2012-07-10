package de.tudarmstadt.ukp.dkpro.lab.task;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import de.tudarmstadt.ukp.dkpro.lab.Lab;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.storage.filesystem.FileSystemStorageService;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.ExecutableTaskBase;

public class DiscriminatorTest
{
	@Test
	public void testMap() throws Exception
	{
		File repo = new File("target/repository/"+getClass().getSimpleName()+"/"+name.getMethodName());
		FileUtils.deleteDirectory(repo);
		repo.mkdirs();
		((FileSystemStorageService) Lab.getInstance().getStorageService()).setStorageRoot(repo);

		Map<String, String> map1 = new HashMap<String, String>();
		map1.put("A", "89zsöoibca");
		map1.put("243jh1g45", "09z2#3ROj 	2r!]9832");

		Map<String, String> map2 = new HashMap<String, String>();
		map2.put("!&(B§V  §= ", "ü 8z9^2g3f9	xy");
		map2.put("!&§$ Ü!§$%ø⁄ª¨", "!$§ 240324 #");

		Dimension<Map<String, String>> dimMap = Dimension.create("map", map1, map2);
		
		ParameterSpace pSpace = new ParameterSpace(dimMap);
		
		BatchTask batch = new BatchTask();
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
