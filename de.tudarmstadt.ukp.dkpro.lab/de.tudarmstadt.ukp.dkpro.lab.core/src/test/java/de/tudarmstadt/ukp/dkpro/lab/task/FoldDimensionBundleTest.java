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

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.lab.task.impl.FoldDimensionBundle;

public class FoldDimensionBundleTest
{
	@Test
	public void testTrainingDimension()
	{
		Dimension<String> baseData = Dimension.create("base", "1", "2", "3", "4", "5", "6", "7", 
				"8", "9", "10");
		
		FoldDimensionBundle<String> foldBundle = new FoldDimensionBundle<String>("fold", baseData, 3);
		
		String expected = 
				"0 - [1, 4, 7, 10] [2, 5, 8, 3, 6, 9]\n" +
				"1 - [2, 5, 8] [1, 4, 7, 10, 3, 6, 9]\n" +
				"2 - [3, 6, 9] [1, 4, 7, 10, 2, 5, 8]\n";

		StringBuilder actual = new StringBuilder();

		int n = 0;
		ParameterSpace pSpace = new ParameterSpace(foldBundle);
		for (Map<String, Object> config : pSpace) {
			actual.append(String.format("%d - %s %s%n", n, config.get("fold_validation"),
					config.get("fold_training")));
			n++;
		}
		
		assertEquals(3 , n);
		assertEquals(3, pSpace.getStepCount());
		assertEquals(expected, actual.toString());
	}
}
