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
package de.tudarmstadt.ukp.dkpro.lab.reporting;

import static de.tudarmstadt.ukp.dkpro.lab.reporting.LabelFunction.PROP_TASK_CONTEXT_ID;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.storage.StreamReader;
import de.tudarmstadt.ukp.dkpro.lab.storage.StreamWriter;
import de.tudarmstadt.ukp.dkpro.lab.storage.impl.PropertiesAdapter;
import de.tudarmstadt.ukp.dkpro.lab.task.Task;

/**
 * Base class for reports.
 *
 * @author Richard Eckart de Castilho
 */
public abstract class ReportBase
	implements Report
{
	private TaskContext context;
	private Map<String, String> properties;
	private Map<String, String> discriminators;

	@Override
	public void setContext(final TaskContext aContext)
	{
		context = aContext;
	}

	public TaskContext getContext()
	{
		return context;
	}

	/**
	 * Convenience method to fetch the properties stored in the tast context.
	 */
	public Map<String, String> getProperties()
	{
		if (properties == null) {
			properties = retrieveBinary(Task.PROPERTIES_KEY, new PropertiesAdapter()).getMap();
		}
		return properties;
	}

	/**
	 * Convenience method to fetch the disciminators stored in the tast context.
	 */
	public Map<String, String> getDiscriminators()
	{
		if (discriminators == null) {
			discriminators = retrieveBinary(Task.DISCRIMINATORS_KEY, new PropertiesAdapter()).getMap();
		}
		return discriminators;
	}

	/**
	 * Convenience method to store data in the task context.
	 */
	public void storeBinary(String aKey, InputStream aStream)
	{
		context.storeBinary(aKey, aStream);
	}

	/**
	 * Convenience method to store data in the task context.
	 */
	public void storeBinary(String aKey, StreamWriter aStream)
	{
		context.storeBinary(aKey, aStream);
	}

	/**
	 * Convenience method to load data from the task context.
	 */
	public <T extends StreamReader> T retrieveBinary(String aPath, T aConsumer)
	{
		return context.retrieveBinary(aPath, aConsumer);
	}

	/**
	 * Convenience method to load data from the task context.
	 */
	public static Map<String, String> asStringMap(Map<? extends Object, ? extends Object> aMap)
	{
		Map<String, String> result = new LinkedHashMap<String, String>();
		for (Entry<?, ?> e : aMap.entrySet()) {
			result.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
		}
		return result;
	}

	public static <V> Map<String, V> translate(Map<String, ? extends V> aMap,
			Map<String, String> aTranslation)
	{
		LinkedHashMap<String, V> out = new LinkedHashMap<String, V>();
		for (Entry<String, ? extends V> e : aMap.entrySet()) {
			String translation = aTranslation.get(e.getKey());
			out.put(translation != null ? translation : e.getKey(), e.getValue());
		}
		return out;
	}

	/**
	 * Get the context label for the given context. Normally this is the context ID, but if a
	 * label function is set using the {@link LabelFunction#PROP_TASK_CONTEXT_ID} property,
	 * this function is instead called and used to create the context label. 
	 * 
	 * @param aContextId a context ID.
	 * @return the label.
	 */
	protected String getContextLabel(String aContextId)
	{
		String func = getProperties().get(TASK_LABEL_FUNC_PROP);
		String result = null;
		if (func != null) {
			LabelFunction lf;
			try {
				lf = (LabelFunction) Class.forName(func).newInstance();
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
			Map<String, String> discs = getContext().getStorageService()
					.retrieveBinary(aContextId, Task.DISCRIMINATORS_KEY, new PropertiesAdapter())
					.getMap();
			discs.put(PROP_TASK_CONTEXT_ID, aContextId);
			result = lf.makeLabel(discs);
		}

		if (result == null) {
			result = aContextId;
		}
		return result;
	}

	protected String getContextLabel()
	{
		return getContextLabel(getContext().getId());
	}

	@Deprecated
	protected PrintWriter getWriter(final String queryId, final String suffix)
		throws IOException
	{
		OutputStream os = new ByteArrayOutputStream()
		{
			@Override
			public void close()
				throws IOException
			{
				super.close();
				storeBinary(queryId + "_" + suffix, new ByteArrayInputStream(toByteArray()));
			}
		};
		return new PrintWriter(new OutputStreamWriter(os, "UTF-8"));
	}
}
