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
package de.tudarmstadt.ukp.dkpro.lab.groovy.task;

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse

import org.junit.Test

public class ClosureDimensionTest
{
	@Test
	public void testSimpleClosures()
	{
		def dimTest = new ClosureDimension("test", { "value" })
		dimTest.setConfiguration [:];
		
		assertEquals "value", dimTest.next()
		assertFalse dimTest.hasNext()
		
		dimTest.rewind()
		
		assertEquals "value", dimTest.next()
		assertFalse dimTest.hasNext()
	}

	@Test
	public void testDiscriminableClosures()
	{
		def dimTest = new ClosureDimension("test", [first: { "one" }, second: { "two" }])
		dimTest.setConfiguration [:];
		
		def first = dimTest.next() as DiscriminableClosure
		assertEquals "first", first.getDiscriminatorValue()
		assertEquals "one", first()
		
		def second = dimTest.next() as DiscriminableClosure
		assertEquals "second", second.getDiscriminatorValue()
		assertEquals "two", second()
		
		assertFalse dimTest.hasNext()
		
		dimTest.rewind()
		
		assertEquals "one", dimTest.next()()
		assertEquals "two", dimTest.next()()
		assertFalse dimTest.hasNext()
	}
}
