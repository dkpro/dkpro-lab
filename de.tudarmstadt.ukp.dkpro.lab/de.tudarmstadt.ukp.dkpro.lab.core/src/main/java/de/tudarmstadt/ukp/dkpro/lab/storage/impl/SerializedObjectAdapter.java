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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import de.tudarmstadt.ukp.dkpro.lab.storage.StreamReader;
import de.tudarmstadt.ukp.dkpro.lab.storage.StreamWriter;

/**
 * Adapter to persist/load a serializable Java object. It is convenient but not recommended to use
 * this adapter. The serialized format is not human-readable and is sensitive to changes in the 
 * persisted classes.
 * 
 * @param <T> the type of object.
 */
public class SerializedObjectAdapter<T>
	implements StreamReader, StreamWriter
{
	private T object;

	public SerializedObjectAdapter()
	{
		// This is generally used for reader usage.
	}

	public SerializedObjectAdapter(T aObject)
	{
		setObject(aObject);
	}
	
	@SuppressWarnings("unchecked")
    @Override
	public void read(InputStream aInputStream)
		throws IOException
	{
	    try {
            object = (T) new ObjectInputStream(aInputStream).readObject();
        }
        catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
	}

	@Override
	public void write(OutputStream aStream)
		throws Exception
	{
	    new ObjectOutputStream(aStream).writeObject(object);
	}

	public void setObject(T aProperties)
	{
		object = aProperties;
	}

	public T getObject()
	{
		return object;
	}
	
	public static <V> SerializedObjectAdapter<V> wrap(V aObject)
	{
	    return new SerializedObjectAdapter<V>(aObject);
	}
}
