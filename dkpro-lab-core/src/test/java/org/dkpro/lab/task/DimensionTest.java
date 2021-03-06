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
package org.dkpro.lab.task;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.dkpro.lab.task.Dimension;
import org.dkpro.lab.task.impl.DiscreteDimension;
import org.junit.Test;

public class DimensionTest
{
	public static enum TestEnum { ONE, TWO }

	@Test
	public void testEnum()
	{
		Dimension<TestEnum> dim = Dimension.create("test", TestEnum.class);
		assertArrayEquals(TestEnum.values(), ((DiscreteDimension<TestEnum>) dim).values());

		assertTrue(dim.hasNext());
		assertEquals(TestEnum.ONE, dim.next());
		assertTrue(dim.hasNext());
		assertEquals(TestEnum.TWO, dim.next());
		assertFalse(dim.hasNext());

		dim.rewind();

		assertTrue(dim.hasNext());
		assertEquals(TestEnum.ONE, dim.next());
		assertTrue(dim.hasNext());
		assertEquals(TestEnum.TWO, dim.next());
		assertFalse(dim.hasNext());
	}
}
