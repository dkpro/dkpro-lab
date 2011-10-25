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
package de.tudarmstadt.ukp.dkpro.lab.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Simple persistence of object data by reading it to a stream. The implementing class is expected
 * to provide a serialization mechanism. This is usually preferred over simply serializing an
 * object using the Java Object Serialization mechanism, since it is supposed to be resilient to
 * implementation changes. Data would usually be stored in a {@link Properties}, CSV, XML or simple
 * text format.
 *
 * @author Richard Eckart de Castilho
 * @see StreamWriter
 */
public interface StreamReader
{
	/**
	 * Read all data from the given stream an close it.
	 *
	 * @param aInputStream
	 * @throws IOException
	 */
	void read(InputStream aInputStream) throws IOException;
}
