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
package de.tudarmstadt.ukp.dkpro.lab.task.impl;

import de.tudarmstadt.ukp.dkpro.lab.task.Dimension;

public class DiscreteDimension<T>
	extends Dimension<T>
{
	private T[] values;
	private int current;

	public DiscreteDimension(String aName, T... aValues)
	{
		super(aName);
		values = aValues;
		current = -1;
	}

	@Override
	public boolean hasNext()
	{
		return current+1 < values.length;
	}

	@Override
	public T next()
	{
		current++;
		return current();
	}

	@Override
	public T current()
	{
		return values[current];
	}

	@Override
	public void rewind()
	{
		current = -1;
	}

	public T[] values()
	{
		return values;
	}

	@Override
	public String toString()
	{
		return "[" + getName() + ": "
				+ (current >= 0 && current < values.length ? values[current] : "?") + "]";
	}
}
