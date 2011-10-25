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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import de.tudarmstadt.ukp.dkpro.lab.storage.StreamReader;
import de.tudarmstadt.ukp.dkpro.lab.storage.StreamWriter;

/**
 * Task context meta data is a subset of essential information from the {@link Task} as well as some
 * essential execution information. It is persisted for every execution.
 *
 * @author Richard Eckart de Castilho
 */
public class TaskContextMetadata
	implements StreamReader, StreamWriter
{
	public static final String METADATA_KEY = "METADATA.txt";

	private final static String IMPORT = "import.";

	private String uuid;
	private String label;
	private String type;
	private long start;
	private long end;
	private Map<String, String> imports;

	{
		imports = new HashMap<String, String>();
	}

	public String getId()
	{
		return uuid;
	}

	public void setId(String aUuid)
	{
		uuid = aUuid;
	}

	public String getLabel()
	{
		return label;
	}

	public void setLabel(String aLabel)
	{
		label = aLabel;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String aName)
	{
		type = aName;
	}

	public long getStart()
	{
		return start;
	}

	public void setStart(long aStart)
	{
		start = aStart;
	}

	public long getEnd()
	{
		return end;
	}

	public void setEnd(long aEnd)
	{
		end = aEnd;
	}

	public void setImports(Map<String, String> aImports)
	{
		imports.clear();
		if (aImports != null) {
			imports.putAll(aImports);
		}
	}

	public Map<String, String> getImports()
	{
		return imports;
	}

	@Override
	public String toString()
	{
		return "ExperimentEntry [uuid=" + uuid + ", name=" + type + ", start=" + start + ", end="
				+ end + "]";
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(InputStream aInputStream)
		throws IOException
	{
		Properties props = new Properties();
		props.load(aInputStream);
		setStart(Long.valueOf(props.getProperty("begin")));
		setEnd(Long.valueOf(props.getProperty("end")));
		setType(props.getProperty("type"));
		setId(props.getProperty("uuid"));
		setLabel(props.getProperty("label"));

		for (String key : (Set<String>) (Set) props.keySet()) {
			if (!key.startsWith(IMPORT)) {
				continue;
			}
			imports.put(key.substring(IMPORT.length()), props.getProperty(key));
		}
	}

	@Override
	public void write(OutputStream aStream)
		throws Exception
	{
		Properties props = new Properties();
		props.setProperty("begin", String.valueOf(getStart()));
		props.setProperty("end", String.valueOf(getEnd()));
		props.setProperty("type", getType());
		props.setProperty("uuid", getId());
		if (getLabel() != null) {
			props.setProperty("label", getLabel());
		}
		props.setProperty("duration", ((getEnd() - getStart()) / 1000) + "s");

		for (Entry<String, String> e : imports.entrySet()) {
			props.put(IMPORT + e.getKey(), e.getValue());
		}

		props.store(aStream, null);
	}
}
