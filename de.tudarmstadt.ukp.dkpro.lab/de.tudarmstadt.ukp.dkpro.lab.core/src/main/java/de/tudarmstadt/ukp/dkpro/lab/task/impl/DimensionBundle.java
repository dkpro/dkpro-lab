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

import java.util.HashMap;
import java.util.Map;

import de.tudarmstadt.ukp.dkpro.lab.Util;
import de.tudarmstadt.ukp.dkpro.lab.task.Dimension;

public class DimensionBundle<T>
	extends Dimension<Map<String, T>>
{
	public static final String KEY_BUNDLE_ID = "__bundleId";

	private Map<String, T>[] values;
	private int current;

	public DimensionBundle(String aName, Map<String, T>... aValues)
	{
		super(aName);
		values = aValues;
		current = -1;
	}


	public DimensionBundle(String aName, Object[]... aValues)
	{
		super(aName);
		values = new HashMap[aValues.length];
		for (int n = 0; n < aValues.length; n++) {
			Object[] vals = aValues[n];
			values[n] = new HashMap<String, T>();
			for (int i = 0; i < vals.length; i += 2) {
				values[n].put((String) vals[i], (T) vals[i+1]);
			}
		}
		current = -1;
	}

	@Override
	public boolean hasNext()
	{
		return current+1 < values.length;
	}

	@Override
	public Map<String, T> next()
	{
		current++;
		return current();
	}

	@Override
	public Map<String, T> current()
	{
		Map<String, T> mapCopy = new HashMap<String, T>(values[current]);
		mapCopy.remove(KEY_BUNDLE_ID);
		return mapCopy;
//		return values[current];
	}

	@Override
	public void rewind()
	{
		current = -1;
	}

	public Map<String, T>[] values()
	{
		return values;
	}

	public String getBundleId()
	{
		if (current >= 0 && current < values.length) {
			Object id = values[current].get(KEY_BUNDLE_ID);
			if (id != null) {
				return Util.toString(id);
			}
		}
		return null;
	}

	@Override
	public String toString()
	{
		if (current >= 0 && current < values.length) {
			String bundleId = getBundleId();
			if (bundleId != null) {
				return bundleId;
			}
			else {
				return "[" + getName() + ": " + values[current] + "]";
			}
		}
		else {
			return "[" + getName() + ": ?]";
		}
	}
}
