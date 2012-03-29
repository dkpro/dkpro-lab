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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import de.tudarmstadt.ukp.dkpro.lab.task.impl.DimensionBundle;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.DynamicDimension;

public class ParameterSpace implements Iterable<Map<String, Object>>
{
	private Dimension<?>[] dimensions;
	private Set<Constraint> constraints;
	private int stepCount = 0;

	public ParameterSpace(Dimension<?>... aDimensions)
	{
		dimensions = aDimensions;
		constraints = new HashSet<Constraint>();
	}

	public void reset()
	{
		stepCount = 0;
		for (Dimension<?> d : dimensions) {
			d.rewind();
		}
	}
	
	public int getStepCount()
	{
		return stepCount;
	}

	public void addConstraint(Constraint aConstraint)
	{
		constraints.add(aConstraint);
	}

	public void removeCondition(Constraint aConstraint)
	{
		constraints.remove(aConstraint);
	}

	@Override
	public Iterator<Map<String, Object>> iterator()
	{
		return new ParameterSpaceIterator();
	}

	private class ParameterSpaceIterator
		implements Iterator<Map<String, Object>>
	{
		private int incDim = -3;

		public ParameterSpaceIterator()
		{
			step();
		}

		private void step()
		{
			do {
				// Initialize
				if (incDim == -3) {
					for (Dimension<?> d : dimensions) {
						d.rewind();
						try {
							d.next();
						}
						catch (NoSuchElementException e) {
							// No need to move the cursor to the first element in empty dimensions.
						}
					}
					incDim = dimensions.length - 1;
					if (conditionsMet()) {
						return;
					}
				}

				if (incDim < 0) {
					return; // Nothing more to iterate over
				}

				if (dimensions[incDim].hasNext()) {
					dimensions[incDim].next();
				}
				else {
					while (!dimensions[incDim].hasNext()) {
						dimensions[incDim].rewind();
						try {
							dimensions[incDim].next();
						}
						catch (NoSuchElementException e) {
							// No need to move the cursor to the first element in empty dimensions.
						}
						incDim--;
						if (incDim < 0) {
							return; // Nothing more to iterate over
						}
					}
					dimensions[incDim].next();
					incDim = dimensions.length - 1;
				}
			}
			while (!conditionsMet());
		}

		private boolean conditionsMet()
		{
			Map<String, Object> config = current();

			stepCount++;

			for (Constraint c : constraints) {
				if (!c.isValid(config)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean hasNext()
		{
			return incDim > -1;
		}

		public Map<String, Object> current()
		{
			Map<String, Object> config = new LinkedHashMap<String, Object>();
			// Pass 1: no dynamic dimensions
			for (Dimension<?> d : dimensions) {
				try {
					if (d instanceof DimensionBundle<?>) {
						DimensionBundle<?> bundle = ((DimensionBundle<?>) d);
						String bundleId = bundle.getBundleId();
						if (bundleId != null) {
							config.put(bundle.getName(), bundle.getBundleId());
						}
						config.putAll(bundle.current());
					}
					else if (d instanceof DynamicDimension) {
						// defer
					}
					else {
						config.put(d.getName(), d.current());
					}
				}
				catch (NoSuchElementException e) {
					// Empty dimensions contribute nothing
				}
			}

			// Pass 2: dynamic dimensions
			for (Dimension<?> d : dimensions) {
				if (d instanceof DynamicDimension) {
					try {
						((DynamicDimension) d).setConfiguration(config);
						config.put(d.getName(), d.current());
					}
					catch (NoSuchElementException e) {
						// Empty dimensions contribute nothing
					}
				}
			}
			return config;
		}

		@Override
		public Map<String, Object> next()
		{
			try {
				return current();
			}
			finally {
				step();
			}
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException("No no");
		}

		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			for (Dimension<?> d : dimensions) {
				sb.append(d);
			}
			sb.append("]");
			return sb.toString();
		}
	}
}
