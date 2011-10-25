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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import de.tudarmstadt.ukp.dkpro.lab.Util;
import de.tudarmstadt.ukp.dkpro.lab.storage.StreamWriter;

public class CopyStreamWriter
	implements StreamWriter
{
	private final URL urlSource;
	private final File fileSource;

	public CopyStreamWriter(final URL aSource)
	{
		urlSource = aSource;
		fileSource = null;
	}

	public CopyStreamWriter(final File aSource)
	{
		urlSource = null;
		fileSource = aSource;
	}

	@Override
	public void write(OutputStream aStream)
		throws IOException
	{
		InputStream is = null;
		try {
			if (urlSource != null) {
				is = urlSource.openStream();
			}
			else if (fileSource != null) {
				is = new FileInputStream(fileSource);
			}
			else {
				throw new IllegalStateException("Neither URL nor file source has been set");
			}

			Util.shove(is, aStream);
		}
		finally {
			Util.close(is);
		}
	}
}
