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
package org.dkpro.lab.storage;

import java.io.OutputStream;
import java.util.Properties;

/**
 * Simple persistence of object data by writing it to a stream. The implementing class is expected
 * to provide a serialization mechanism. This is usually preferred over simply serializing an
 * object using the Java Object Serialization mechanism, since it is supposed to be resilient to
 * implementation changes. Data would usually be stored in a {@link Properties}, CSV, XML or simple
 * text format.
 * 
 * @see StreamReader
 */
public interface StreamWriter
{
	void write(OutputStream aStream) throws Exception;
}
