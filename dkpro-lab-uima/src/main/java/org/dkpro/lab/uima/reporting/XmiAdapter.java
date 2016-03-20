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
package org.dkpro.lab.uima.reporting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.util.XMLSerializer;
import org.dkpro.lab.storage.StreamReader;
import org.dkpro.lab.storage.StreamWriter;
import org.xml.sax.SAXException;

public class XmiAdapter
	implements StreamReader, StreamWriter
{
	private CAS cas;

	public XmiAdapter(CAS aCas)
	{
		setCas(aCas);
	}

	@Override
	public void read(InputStream aInputStream)
		throws IOException
	{
		try {
			XmiCasDeserializer.deserialize(aInputStream, cas);
		}
		catch (SAXException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void write(OutputStream aStream)
		throws Exception
	{
		XmiCasSerializer ser = new XmiCasSerializer(cas.getTypeSystem());
		XMLSerializer xmlSer = new XMLSerializer(aStream, false);
		ser.serialize(cas, xmlSer.getContentHandler());
	}

	public void setCas(CAS aCas)
	{
		cas = aCas;
	}

	public CAS getCas()
	{
		return cas;
	}
}
