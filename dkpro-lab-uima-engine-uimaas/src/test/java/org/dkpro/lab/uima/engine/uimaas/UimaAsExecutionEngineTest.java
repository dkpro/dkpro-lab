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
package org.dkpro.lab.uima.engine.uimaas;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.CasCollectionReader_ImplBase;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.dkpro.lab.Util;
import org.dkpro.lab.engine.TaskContext;
import org.dkpro.lab.engine.TaskContextFactory;
import org.dkpro.lab.engine.TaskExecutionEngine;
import org.dkpro.lab.engine.TaskExecutionService;
import org.dkpro.lab.storage.StorageService;
import org.dkpro.lab.storage.StreamReader;
import org.dkpro.lab.storage.filesystem.FileSystemStorageService;
import org.dkpro.lab.support.slf4j.Logging;
import org.dkpro.lab.task.TaskContextMetadata;
import org.dkpro.lab.uima.engine.uimaas.component.SimpleBroker;
import org.dkpro.lab.uima.task.TaskContextProvider;
import org.dkpro.lab.uima.task.impl.DefaultUimaTask;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/test-context.xml" })
public class UimaAsExecutionEngineTest
{
	@Resource(name = "TaskExecutionService")
	private TaskExecutionService executionService;

	@Resource(name = "TaskContextFactory")
	private TaskContextFactory contextFactory;

	@Resource(name = "StorageService")
	private StorageService storageService;

	private static SimpleBroker broker;

	@BeforeClass
	public static void init() throws ResourceInitializationException
	{
        // Ok, so this is how we tell UIMA-AS not to till the JVM... *sigh*
        System.setProperty("dontKill","");  
	    
        Logging.initialize();
        
		broker = new SimpleBroker();
		broker.start();
	}

	@AfterClass
	public static void teardown() throws ResourceInitializationException
	{
		broker.stop();
	}

	@Test
	public void testInit()
		throws Exception
	{
		File repo = new File("target/repository");
		FileUtils.deleteQuietly(repo);
		((FileSystemStorageService) storageService).setStorageRoot(repo);

		assertNotNull(executionService);
		assertNotNull(contextFactory);

		AnalysisEngineDescription desc = createEngineDescription(DummyAE.class);

		DefaultUimaTask cfg = new DefaultUimaTask();
		cfg.setReaderDescription(createReaderDescription(TestReader.class));
		cfg.setAnalysisEngineDescription(desc);

		TaskExecutionEngine runner = executionService.createEngine(cfg);
		String uuid = runner.run(cfg);

		System.out.println("=== Experiments in repository ===");
		List<TaskContextMetadata> experiments = storageService.getContexts();
		for (TaskContextMetadata e : experiments) {
			System.out.println(e);
		}

		final StringBuilder sb = new StringBuilder();
		storageService.retrieveBinary(uuid, "test", new StreamReader()
		{
			@Override
			public void read(InputStream aInputStream)
				throws IOException
			{
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				Util.shoveAndClose(aInputStream, bos);
				sb.append(new String(bos.toByteArray(), "UTF-8"));
			}
		});

		assertEquals("works", sb.toString());
	}

    public static final class TestReader
        extends CasCollectionReader_ImplBase
    {
        private boolean read = false;

        @Override
        public void getNext(CAS aCAS)
            throws IOException, CollectionException
        {
            read = true;
            aCAS.setDocumentText("This is a text.");
        }

        @Override
        public boolean hasNext()
            throws IOException, CollectionException
        {
            return !read;
        }

        @Override
        public Progress[] getProgress()
        {
            return null;
        }
    }

    public static final class DummyAE
        extends JCasAnnotator_ImplBase
    {
        @ExternalResource(api = TaskContextProvider.class)
        TaskContext ctx;

        @Override
        public void process(JCas aJCas)
            throws AnalysisEngineProcessException
        {
            try {
                ctx.message("Processing");
                ctx.getStorageService().storeBinary(ctx.getId(), "test",
                        new ByteArrayInputStream("works".getBytes("UTF-8")));
            }
            catch (Exception e) {
                throw new AnalysisEngineProcessException(e);
            }
        }
    }
}
