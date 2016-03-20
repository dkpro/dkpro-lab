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
package org.dkpro.lab.groovy.task;

import static org.junit.Assert.assertEquals
import static org.junit.Assert.fail

import org.junit.Test

import org.dkpro.lab.Lab
import org.dkpro.lab.engine.TaskContext
import org.dkpro.lab.task.Dimension
import org.dkpro.lab.task.Discriminator
import org.dkpro.lab.task.ParameterSpace
import org.dkpro.lab.task.impl.DefaultBatchTask;
import org.dkpro.lab.task.impl.ExecutableTaskBase

public class ParameterSpaceTest
{
    @Test
    public void simple()
    {   
        System.setProperty("DKPRO_HOME", "target");
          
        def subtask = new ExecutableTaskBase() {
            @Discriminator int x;
            @Discriminator int y;
        
            void execute(TaskContext context) {
                println "Parameter space coordinates x: ${x}  y: ${y}";
            }
        }
        
        def batchTask = [ 
            parameterSpace: [
              dimensions:  [
                Dimension.create("x", 1, 2, 3, 4, 5, /*6, 7, 8, 9, 10, 11, 12, 13, 14, 15*/),
                Dimension.create("y", 1, 2, 3, 4, 5, /*6, 7, 8, 9, 10, 11, 12, 13, 14, 15*/)],
            ] as ParameterSpace,
            tasks: [subtask]
        ] as DefaultBatchTask;

        Lab.instance.run(batchTask);
    }

	@Test
	public void testWithConstraint()
	{
		Dimension<String> letters = Dimension.create("letters", "a", "b", "c");
		Dimension<Integer> numbers = Dimension.create("numbers", 1, 2);
		Dimension<Character> symbols = Dimension.create("symbols", '!', '&');

		int n = 0;
		def pSpace = [
            constraints: [new Constraint({ it["letters"] != "a" || it["symbols"] == '!' })],
            dimensions:  [letters, numbers, symbols]
        ] as ParameterSpace;
		for (Map<String, Object> config : pSpace) {
			println config;
			if ("a" == config["letters"] && config["symbols"] != '!') {
				fail();
			}
			n++;
		}
		assertEquals((3 * 2 * 2) - 2, n);
		assertEquals((3 * 2 * 2), pSpace.getStepCount());
	}

    @Test
    public void testWithConstraint2()
    {
        Dimension<String> docs = Dimension.create("docs", "a", "b", "c");
        Dimension<String> lang = Dimension.create("lang", "de", "en");
        Dimension<String> res =  Dimension.create("res", "res-de.txt", "res-en.txt");

        int n = 0;
        def pSpace = [
            dimensions:  [docs, lang, res],
            constraints: [new Constraint({ it["res"].endsWith(it["lang"] + ".txt") })],
        ] as ParameterSpace;
        for (Map<String, Object> config : pSpace) {
            println config;
            n++;
        }
        assertEquals(6, n);
        assertEquals(12, pSpace.getStepCount());
    }
}