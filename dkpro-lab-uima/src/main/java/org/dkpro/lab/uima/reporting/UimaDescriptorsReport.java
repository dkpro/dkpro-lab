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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.dkpro.lab.Lab;
import org.dkpro.lab.engine.TaskContext;
import org.dkpro.lab.reporting.Report;
import org.dkpro.lab.storage.StreamReader;
import org.dkpro.lab.uima.task.UimaTask;

public class UimaDescriptorsReport
	implements Report
{
	private TaskContext context;

	@Override
	public void execute()
	{
		XSLTStreamReader r = new XSLTStreamReader();
		context.retrieveBinary(UimaTask.ANALYSIS_ENGINE_DESC_KEY, r);

		context.storeBinary(UimaTask.ANALYSIS_ENGINE_DESC_KEY + ".html", r.openStream());

		context.retrieveBinary(UimaTask.COLLECTION_READER_DESC_KEY, r);

		context.storeBinary(UimaTask.COLLECTION_READER_DESC_KEY + ".html", r.openStream());
	}

	@Override
	public void setContext(TaskContext aContext)
	{
		context = aContext;
	}

	public static void main(String[] args) throws Exception
	{
		String uuid = args[0];

		Lab framework = Lab.getInstance();

		UimaDescriptorsReport report = new UimaDescriptorsReport();
		report.setContext(framework.getTaskContextFactory().getContext(uuid));
		report.execute();
	}

	private class XSLTStreamReader implements StreamReader
	{
		private ByteArrayOutputStream bos;

		@Override
		public void read(InputStream aInputStream)
			throws IOException
		{
			try {
				bos = new ByteArrayOutputStream();
				Transformer trans = TransformerFactory.newInstance().newTransformer(
						new StreamSource(getClass().getResource(
								"/xslt/resourceSpecifier2html.xslt").openStream()));
				// apply transformation on XML file and pipe to stdout
				trans.transform(
						new StreamSource(aInputStream),
						new StreamResult(bos));
			}
			catch (Exception e) {
				throw new IOException(e);
			}
		}

		public InputStream openStream()
		{
			return new ByteArrayInputStream(bos.toByteArray());
		}
	}
}
