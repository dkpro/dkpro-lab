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

import groovy.lang.Closure;
import de.tudarmstadt.ukp.dkpro.lab.task.Discriminable;

public class DiscriminableClosure
	extends AbstractClosureProxy
	implements Discriminable
{
	private static final long serialVersionUID = -4012729586437644140L;
	
	private String name;

	public DiscriminableClosure(Closure aClosure)
	{
		super(aClosure);
	}

	public DiscriminableClosure(Closure aClosure, String aName)
	{
		super(aClosure);
		name = aName;
	}

	@Override
	protected void doBeforeCall(Object[] aArgs)
	{
		// Do nothing
	}

	@Override
	protected void doAfterCall(Object[] aArgs)
	{
		// Do nothing
	}

	@Override
	protected Closure createWrapper(Closure aC)
	{
		DiscriminableClosure c = new DiscriminableClosure(aC);
		c.name = name;
		return c;
	}

	@Override
	public String getDiscriminatorValue()
	{
		return name;
	}

	@Override
	public Object getActualValue()
	{
		return target;
	}
}
