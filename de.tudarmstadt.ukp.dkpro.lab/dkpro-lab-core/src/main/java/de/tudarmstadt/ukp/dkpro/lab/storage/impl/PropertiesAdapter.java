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
package de.tudarmstadt.ukp.dkpro.lab.storage.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import de.tudarmstadt.ukp.dkpro.lab.storage.StreamReader;
import de.tudarmstadt.ukp.dkpro.lab.storage.StreamWriter;

public class PropertiesAdapter
	implements StreamReader, StreamWriter
{
	private Properties properties;
	private String comment;

	public PropertiesAdapter()
	{
		// This is generally used for reader usage.
	}

	public PropertiesAdapter(Properties aProperties)
	{
		this(aProperties, null);
	}

	public PropertiesAdapter(Properties aProperties, String aComment)
	{
		setProperties(aProperties);
		setComment(aComment);
	}

	public PropertiesAdapter(Map<String, String> aMap)
	{
		this(aMap, null);
	}

	public PropertiesAdapter(Map<String, String> aMap, String aComment)
	{
		Properties props = new Properties();
		for (Entry<String, String> e : aMap.entrySet()) {
			props.setProperty(e.getKey(), e.getValue());
		}
		setProperties(props);
		setComment(aComment);
	}

	@Override
	public void read(InputStream aInputStream)
		throws IOException
	{
		if (properties == null) {
			properties = new Properties();
		}
		properties.load(aInputStream);
	}

	@Override
	public void write(OutputStream aStream)
		throws Exception
	{
		properties.store(aStream, comment);
	}

	public void setProperties(Properties aProperties)
	{
		properties = aProperties;
	}

	public Properties getProperties()
	{
		return properties;
	}

	public Map<String, String> getMap()
	{
		Map<String, String> map = new HashMap<String, String>();
		for (String key : properties.stringPropertyNames()) {
			map.put(key, properties.getProperty(key));
		}
		return map;
	}

	public void setComment(String aComment)
	{
		comment = aComment;
	}

	public String getComment()
	{
		return comment;
	}
}
