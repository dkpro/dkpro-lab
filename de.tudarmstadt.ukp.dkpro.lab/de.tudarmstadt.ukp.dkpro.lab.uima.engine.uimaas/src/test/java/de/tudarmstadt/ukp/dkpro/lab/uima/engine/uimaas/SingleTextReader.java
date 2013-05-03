/*
 Copyright 2009 Regents of the University of Colorado.
 All rights reserved.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package de.tudarmstadt.ukp.dkpro.lab.uima.engine.uimaas;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

/**
 * <br>
 * O
 * This collection reader allows one to read in a single test file. Always good
 * to have one of these for testing purposes.
 *
 * @author Richard Eckart de Castilho
 */
public class SingleTextReader
	extends CollectionReader_ImplBase
{

	/**
	 * "FileName" is a single, required, string parameter that takes either the
	 * name of a single file or the root directory containing all the files to
	 * be processed.
	 */
	public static final String PARAM_FILE_NAME = "org.uimafit.t.util.SingleTextReader.PARAM_FILE_NAME";

	private boolean hasNext = true;

	private File file;

	@Override
	public void initialize()
		throws ResourceInitializationException
	{
		super.initialize();

		// get the input directory
		String fileName = (String) this.getUimaContext()
				.getConfigParameterValue(PARAM_FILE_NAME);

		if (fileName == null) {
			throw new ResourceInitializationException(
					ResourceInitializationException.CONFIG_SETTING_ABSENT,
					new Object[] { PARAM_FILE_NAME });
		}
		file = new File(fileName);
	}

	@Override
	public void getNext(CAS cas)
		throws IOException, CollectionException
	{
		ByteBuffer buffer = ByteBuffer.allocate(16000);
		StringBuilder sb = new StringBuilder();
		FileInputStream is = new FileInputStream(file);
		int read;
		while ((read = is.getChannel().read(buffer)) != -1) {
			sb.append(new String(buffer.array(), 0, read, "UTF-8"));
		}
		is.close();

		cas.setDocumentText(sb.toString());

		hasNext = false;
	}

	@Override
	public void close()
		throws IOException
	{
		// Do nothing
	}

	@Override
	public Progress[] getProgress()
	{
		if (hasNext) {
			return new Progress[] { new ProgressImpl(0, 1,
					Progress.ENTITIES) };
		}
		return new Progress[] { new ProgressImpl(1, 1, Progress.ENTITIES) };
	}

	@Override
	public boolean hasNext()
		throws IOException, CollectionException
	{
		return hasNext;
	}

}
