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

import de.tudarmstadt.ukp.dkpro.lab.Util;
import de.tudarmstadt.ukp.dkpro.lab.storage.StreamReader;

public class CopyStreamReader
	implements StreamReader
{
	private final OutputStream target;

	public CopyStreamReader(final OutputStream aTarget)
	{
		target = aTarget;
	}

	@Override
	public void read(InputStream aInputStream)
		throws IOException
	{
		try {
			Util.shove(aInputStream, target);
		}
		finally {
			Util.close(aInputStream);
		}
	}
}
