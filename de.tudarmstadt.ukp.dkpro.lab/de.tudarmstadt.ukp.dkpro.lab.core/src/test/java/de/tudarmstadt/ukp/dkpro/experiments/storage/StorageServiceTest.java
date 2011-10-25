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

import javax.annotation.Resource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.tudarmstadt.ukp.dkpro.lab.storage.StorageService;


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
	public void makeJunitHappy()
	{
		// Do nothing
	}

//	@Test
//	public void testAppend() throws Exception
//	{
//		JSONObject child = new JSONObject();
//		child.put("alive", true);
//
//		JSONObject payload = new JSONObject();
//		payload.put("ok", true);
//		payload.put("nested", child);
//
//		JSONArray ref = new JSONArray();
//		ref.put(payload);
//		ref.put(payload);
//
//		storageService.appendObject("dummy", "arrayTest", payload);
//		storageService.appendObject("dummy", "arrayTest", payload);
//
//		JSONArray aArray = storageService.retrieveArray("dummy", "arrayTest");
//
//		assertEquals(ref.toString(), aArray.toString());
//	}
}
