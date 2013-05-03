package de.tudarmstadt.ukp.dkpro.lab.uima.engine.uimaas;

import static org.junit.Assert.assertEquals;
import static org.uimafit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.uimafit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Test;
import org.uimafit.component.JCasAnnotator_ImplBase;

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
				createPrimitiveDescription(Annotator.class));
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
