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
package de.tudarmstadt.ukp.dkpro.experiments.storage;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.tudarmstadt.ukp.dkpro.lab.storage.StorageService;
import de.tudarmstadt.ukp.dkpro.lab.storage.impl.PropertiesAdapter;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/test-context.xml" })
public class StorageServiceTest
{
	@Resource(name = "StorageService")
	private StorageService storageService;

	@Before
	public void setup()
	{
		storageService.delete("dummy");
	}

	@After
	public void teardown()
	{
		storageService.delete("dummy");
	}

	@Test
	public void storeReadProperties()
	{
		Map<String, String> data = new HashMap<String, String>();
		data.put("key1", "value1");
		data.put("key2", "value2");
	
		storageService.storeBinary("dummy", "data", new PropertiesAdapter(data));

		Map<String, String> data2 = storageService.retrieveBinary(
				"dummy", "data", new PropertiesAdapter(data)).getMap();
		
		assertEquals(data, data2);
	}
}
