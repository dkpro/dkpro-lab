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

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import de.tudarmstadt.ukp.dkpro.lab.task.impl.DimensionBundle;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.DiscreteDimension;

/**
 * A dimension in a {@link ParameterSpace}.
 *
 * @author Richard Eckart de Castilho
 */
public abstract class Dimension<T>
	implements Iterator<T>
{
	private final String name;

	public Dimension(String aName)
	{
		name = aName;
	}

	public String getName()
	{
		return name;
	}

	/**
	 * Reset dimension to before the first value. A call to {@link #next} will return the first
	 * value.
	 */
	public abstract void rewind();

	/**
	 * Get the next value.
	 * 
	 * @throws NoSuchElementException if the dimension is empty.
	 */
	@Override
	public abstract T next();
	
	/**
	 * Get the current value.
	 * 
	 * @throws NoSuchElementException if the dimension is empty.
	 */
	public abstract T current();

	/**
	 * Removing values from a dimension is not supported.
	 *
	 * @throws UnsupportedOperationException operation is not supported.
	 */
	@Override
	public void remove()
	{
		throw new UnsupportedOperationException("No no");
	}

	/**
	 * Create a new dimension. If only one value is given an that is a subclass of {@link Enum},
	 * then this method forwards to {@link #create(String, Class)}.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> Dimension<T> create(String aName, T... aValues)
	{
		if (aValues.length == 1 && (aValues[0] instanceof Class)) {
			// Dispatch to enum factory method
			Class<?> clazz = (Class<?>) aValues[0];
			if (Enum.class.isAssignableFrom(clazz)) {
				return create(aName, (Class<Enum>) clazz);
			}
		}
		return new DiscreteDimension<T>(aName, aValues);
	}

	public static <T> Dimension<Map<String, T>> createBundle(String aName, Map<String, T>... aValues)
	{
		return new DimensionBundle<T>(aName, aValues);
	}

	public static <T> Dimension<Map<String, T>> createBundle(String aName, Object[]... aValues)
	{
		return new DimensionBundle<T>(aName, aValues);
	}

	/**
	 * Create a new dimension from the values of an {@link Enum}.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T extends Enum<T>> Dimension<T> create(String aName, Class<T> aEnum)
	{
		if (!Enum.class.isAssignableFrom(aEnum)) {
			// Dispatch to non-enum factory method - this is necessary because Java dispatches to
			// this method when create(name, class) is called. Obviously the generic type
			// restriction is ignored at runtime.
			return (Dimension) create(aName, new Object[] { aEnum } );
		}

		try {
			T[] values = (T[]) aEnum.getMethod("values").invoke(null, (Object[]) null);
			return new DiscreteDimension<T>(aName, values);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Fetches the value associated with this dimension from the given configuration.
	 *
	 * @param aMap the configuration.
	 * @return the value for this dimension.
	 */
	@SuppressWarnings("unchecked")
	public T get(Map<String, Object> aMap)
	{
		return (T) aMap.get(getName());
	}
}
