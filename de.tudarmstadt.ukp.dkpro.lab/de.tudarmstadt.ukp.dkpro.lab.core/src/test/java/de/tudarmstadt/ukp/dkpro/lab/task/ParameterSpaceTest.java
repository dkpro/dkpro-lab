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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Test;

public class ParameterSpaceTest
{
	@Test
	public void test()
	{
		Dimension<String> letters = Dimension.create("letters", "a", "b", "c");
		Dimension<Integer> numbers = Dimension.create("numbers", 1, 2);
		Dimension<Character> symbols = Dimension.create("symbols", '!', '&');

		int n = 0;
		ParameterSpace pSpace = new ParameterSpace(letters, numbers, symbols);
		for (@SuppressWarnings("unused") Map<String, Object> config : pSpace) {
			n++;
		}
		assertEquals(3 * 2 * 2, n);
	}

	@Test
	public void testWithCondition()
	{
		Dimension<String> letters = Dimension.create("letters", "a", "b", "c");
		Dimension<Integer> numbers = Dimension.create("numbers", 1, 2);
		Dimension<Character> symbols = Dimension.create("symbols", '!', '&');

		int n = 0;
		ParameterSpace pSpace = new ParameterSpace(letters, numbers, symbols);
		pSpace.addConstraint(new Constraint()
		{
			@Override
			public boolean isValid(Map<String, Object> aConfiguration)
			{
				// a implies 1
				return !aConfiguration.get("letters").equals("a")
						|| aConfiguration.get("symbols").equals('!');
			}
		});
		for (Map<String, Object> config : pSpace) {
			System.out.println(config);
			if ("a".equals(config.get("letters")) && !config.get("symbols").equals('!')) {
				fail();
			}
			n++;
		}
		assertEquals((3 * 2 * 2) - 2, n);
	}
}
