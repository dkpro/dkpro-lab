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

import groovy.lang.Closure;

import java.util.Map;
import java.util.Map.Entry;

import org.dkpro.lab.task.Dimension;
import org.dkpro.lab.task.impl.DynamicDimension;

/**
 * A dimension made from Groovy {@link Closure}s.
 * 
 * @author Richard Eckart de Castilho
 */
public class ClosureDimension<T>
	extends Dimension<T>
	implements DynamicDimension
{
	private Closure[] closures;
	private int current = -1;
	private Map<String, Object> config;

	/**
	 * Create a new dimension using values obtained from closure calls. A closure may accept a
	 * map as argument which then contains the current parameter space configuration with all
	 * values bound except those that need to be obtained from closures. If the dimension is
	 * used as a discriminator, the value returned from the closure call will be the discriminator
	 * value - you need to make sure uniquely reflects the desired closure call result and can be
	 * serialized to a string using its {@link Object#toString} method.
	 */
	public ClosureDimension(String aName, Closure... aClosures)
	{
		super(aName);
		closures = aClosures;
	}
	
	/**
	 * Create a new dimension of closures. These closures directly injected into the parameter
	 * space - they are not called (cf. {@link ClosureDimension#ClosureDimension(String, Closure...)}).
	 * The key associated with each closure is the value used when the closure used as a 
	 * discriminator.
	 */
	public ClosureDimension(String aName, Map<String, Closure> aClosures)
	{
		super(aName);
		closures = new Closure[aClosures.size()];
		int i = 0;
		for (Entry<String, Closure> e : aClosures.entrySet()) {
			closures[i] = new DiscriminableClosure(e.getValue(),e.getKey());
			i++;
		}
	}
	
	@Override
	public void setConfiguration(Map<String, Object> aConfig)
	{
		config = aConfig;
	}

	@Override
	public boolean hasNext()
	{
		return current+1 < closures.length;
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
		// When calling next() after rewind() to position current() at the first
		// dimension value, no config has been set yet. At this point just
		// do nothing.
		if (config == null) {
			return null;
		}
		
		if (closures[current] instanceof DiscriminableClosure) {
			return closures[current];
		}
		
		try {
			return closures[current].call(config);
		}
		catch (Exception e) {
			return closures[current].call();
		}
	}

	@Override
	public void rewind()
	{
		current = -1;
	}

	@Override
	public String toString()
	{
		return "[" + getName() + ": " + (current >= 0 && current < closures.length ? current() : "?") + "]";
	}
}
