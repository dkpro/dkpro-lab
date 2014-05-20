/*******************************************************************************
 * Copyright 2014
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

import org.apache.commons.io.IOUtils;

import de.tudarmstadt.ukp.dkpro.lab.storage.StreamReader;
import de.tudarmstadt.ukp.dkpro.lab.storage.StreamWriter;

/**
 * Adapter to persist/load a simple string.
 */
public class StringAdapter
	implements StreamReader, StreamWriter
{
	private String object;
	private String encoding;

    public StringAdapter()
    {
        this("UTF-8");
    }
    
	public StringAdapter(String aString)
	{
        setString(aString);
	}

	public StringAdapter(String aObject, String aEncoding)
	{
		setString(aObject);
		setEncoding(aEncoding);
	}
	
    @Override
	public void read(InputStream aInputStream)
		throws IOException
	{
	    object = IOUtils.toString(aInputStream, encoding);
	}

	@Override
	public void write(OutputStream aStream)
		throws Exception
	{
	    IOUtils.write(object, aStream, encoding);
	}

	public void setString(String aString)
	{
		object = aString;
	}

	public String getString()
	{
		return object;
	}
	
	public void setEncoding(String aEncoding)
    {
        encoding = aEncoding;
    }
	
	public String getEncoding()
    {
        return encoding;
    }
}
