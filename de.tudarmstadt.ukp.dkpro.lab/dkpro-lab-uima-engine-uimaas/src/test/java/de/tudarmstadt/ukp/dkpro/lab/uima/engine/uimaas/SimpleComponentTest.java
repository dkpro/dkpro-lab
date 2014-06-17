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
package de.tudarmstadt.ukp.dkpro.lab.uima.engine.uimaas;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.junit.Assert.assertEquals;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.lab.uima.engine.uimaas.component.SimpleBroker;
import de.tudarmstadt.ukp.dkpro.lab.uima.engine.uimaas.component.SimpleClient;
import de.tudarmstadt.ukp.dkpro.lab.uima.engine.uimaas.component.SimpleService;

public class SimpleComponentTest
{
	@Test
	public void test() throws Exception
	{
		SimpleBroker broker = new SimpleBroker();
		SimpleService service = new SimpleService("myAnalysisEngine",
		        createEngineDescription(Annotator.class));
		SimpleClient client = new SimpleClient("myAnalysisEngine");

		broker.start();
		service.start();
		client.start();

		CAS cas = CasCreationUtils.createCas(createTypeSystemDescription(), null, null);
		cas.setDocumentText("This is a test.");
		client.process(cas);

		client.stop();
		service.stop();
		broker.stop();

		assertEquals("en", cas.getDocumentLanguage());
	}

	public static final class Annotator
	extends JCasAnnotator_ImplBase
	{
		@Override
		public void process(JCas aJCas)
			throws AnalysisEngineProcessException
		{
			assertEquals("This is a test.", aJCas.getDocumentText());
			aJCas.setDocumentLanguage("en");
		}
	}
}
