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
package de.tudarmstadt.ukp.dkpro.lab.ml.example;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.apache.uima.fit.util.JCasUtil.select;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.classifier.jar.GenericJarClassifierFactory;
import org.cleartk.classifier.jar.JarClassifierBuilder;
import org.cleartk.classifier.opennlp.DefaultMaxentDataWriterFactory;
import org.cleartk.classifier.viterbi.ViterbiDataWriterFactory;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionMethod;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.io.imscwb.ImsCwbWriter;
import de.tudarmstadt.ukp.dkpro.core.io.negra.NegraExportReader;
import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiReader;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiWriter;
import de.tudarmstadt.ukp.dkpro.core.snowball.SnowballStemmer;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.lab.Lab;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.storage.StorageService.AccessMode;
import de.tudarmstadt.ukp.dkpro.lab.storage.filesystem.FileSystemStorageService;
import de.tudarmstadt.ukp.dkpro.lab.task.Dimension;
import de.tudarmstadt.ukp.dkpro.lab.task.Discriminator;
import de.tudarmstadt.ukp.dkpro.lab.task.ParameterSpace;
import de.tudarmstadt.ukp.dkpro.lab.task.Task;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask.ExecutionPolicy;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.ExecutableTaskBase;
import de.tudarmstadt.ukp.dkpro.lab.uima.task.impl.UimaTaskBase;

public class PosExampleMaxEnt
{
	private static final String CORPUS_PATH = "src/test/resources/trainingdata/tuebadz-5.0-first10.export.bz2";

	@Test
	public void run()
		throws Exception
	{
        // Route logging through log4j
        System.setProperty("org.apache.uima.logger.class", "org.apache.uima.util.impl.Log4jLogger_impl");        
	    
		clean();

		Task preprocessingTask = new UimaTaskBase() {
			@Discriminator
			String corpusPath;

			{ setType("Preprocessing"); }

			@Override
			public CollectionReaderDescription getCollectionReaderDescription(TaskContext aContext)
				throws ResourceInitializationException, IOException
			{
				return createReader(NegraExportReader.class,
						NegraExportReader.PARAM_SOURCE_LOCATION, corpusPath,
						NegraExportReader.PARAM_LANGUAGE, "de");
			}

			@Override
			public AnalysisEngineDescription getAnalysisEngineDescription(TaskContext aContext)
				throws ResourceInitializationException, IOException
			{
				File xmiDir = aContext.getFolder("XMI", AccessMode.READWRITE);
				return createEngine(
						createEngine(SnowballStemmer.class),
						createEngine(XmiWriter.class,
								XmiWriter.PARAM_TARGET_LOCATION, xmiDir.getAbsolutePath(),
								XmiWriter.PARAM_COMPRESSION, CompressionMethod.GZIP));
			}
		};

		Task featureExtractionTask = new UimaTaskBase() {

			{ setType("FeatureExtraction"); }

			@Override
			public CollectionReaderDescription getCollectionReaderDescription(TaskContext aContext)
				throws ResourceInitializationException, IOException
			{
				File xmiDir = aContext.getFolder("XMI", AccessMode.READONLY);
				return createReader(XmiReader.class,
						XmiReader.PARAM_SOURCE_LOCATION, xmiDir.getAbsolutePath(),
						XmiReader.PARAM_PATTERNS, new String[] { "[+]**/*.xmi.gz" });
			}

			@Override
			public AnalysisEngineDescription getAnalysisEngineDescription(TaskContext aContext)
				throws ResourceInitializationException, IOException
			{
				File modelDir = aContext.getFolder("MODEL", AccessMode.READWRITE);
				return createEngine(
						createEngineDescription(
								ExamplePosAnnotator.class,
						        ExamplePosAnnotator.PARAM_DATA_WRITER_FACTORY_CLASS_NAME, ViterbiDataWriterFactory.class.getName(),
						        ViterbiDataWriterFactory.PARAM_OUTPUT_DIRECTORY, modelDir.getAbsolutePath(),
						        ViterbiDataWriterFactory.PARAM_DELEGATED_DATA_WRITER_FACTORY_CLASS, DefaultMaxentDataWriterFactory.class.getName()));
			}
		};
		Task trainingTask = new ExecutableTaskBase() {
			@Discriminator
			private int iterations;

			@Discriminator
			private int cutoff;

			{ setType("TrainingTask"); }

			@Override
			public void execute(TaskContext aContext)
				throws Exception
			{
				File dir = aContext.getFolder("MODEL", AccessMode.READWRITE);
			    JarClassifierBuilder<?> classifierBuilder = JarClassifierBuilder.fromTrainingDirectory(dir);
			    classifierBuilder.trainClassifier(dir, new String[] { String.valueOf(iterations),
			    		String.valueOf(cutoff)});
			    classifierBuilder.packageClassifier(dir);
			}
		};

		Task analysisTask = new UimaTaskBase() {

			{ setType("AnalysisTask"); }

			@Override
			public CollectionReaderDescription getCollectionReaderDescription(TaskContext aContext)
				throws ResourceInitializationException, IOException
			{
				return createReaderDescription(TextReader.class,
						TextReader.PARAM_SOURCE_LOCATION, "src/test/resources/text/**/*.txt",
						TextReader.PARAM_LANGUAGE, "de");
			}

			@Override
			public AnalysisEngineDescription getAnalysisEngineDescription(TaskContext aContext)
				throws ResourceInitializationException, IOException
			{
				File model = new File(aContext.getFolder("MODEL", AccessMode.READONLY), "model.jar");
				File tsv = new File(aContext.getFolder("TSV", AccessMode.READWRITE), "output.tsv");
				return createEngine(
						createEngineDescription(BreakIteratorSegmenter.class),
						createEngineDescription(SnowballStemmer.class),
						createEngineDescription(ExamplePosAnnotator.class,
				        		GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH, model.getAbsolutePath()),
				        		createEngineDescription(ImsCwbWriter.class,
		        				ImsCwbWriter.PARAM_TARGET_LOCATION, tsv));
			}
		};

		ParameterSpace pSpace = new ParameterSpace(
				Dimension.create("corpusPath", CORPUS_PATH),
				Dimension.create("iterations", 20, 50, 100),
				Dimension.create("cutoff", 5));

        featureExtractionTask.addImport(preprocessingTask, "XMI");
        trainingTask.addImport(featureExtractionTask, "MODEL");
        analysisTask.addImport(trainingTask, "MODEL");

		BatchTask batch = new BatchTask();
		batch.setParameterSpace(pSpace);
		batch.setExecutionPolicy(ExecutionPolicy.USE_EXISTING);
		batch.addTask(preprocessingTask);
		batch.addTask(featureExtractionTask);
		batch.addTask(trainingTask);
		batch.addTask(analysisTask);

		Lab.getInstance().run(batch);
	}

	/**
	 * This is a hack to change the repository to a path inside this project.Normally you should
	 * set the environment variable DKPRO_HOME before using the Lab.
	 */
	public void clean()
		throws Exception
	{
        System.setProperty("DKPRO_HOME", new File("target").getAbsolutePath());
		File repo = new File("target/repository");
		FileUtils.deleteDirectory(repo);
		repo.mkdirs();
		((FileSystemStorageService) Lab.getInstance().getStorageService()).setStorageRoot(repo);
	}

	public static class TokenCounter extends JCasAnnotator_ImplBase
	{
		long start;
		long tcount = 0;

		@Override
		public void initialize(UimaContext aContext)
			throws ResourceInitializationException
		{
			start = System.currentTimeMillis();
		}

		@Override
		public void process(JCas aJCas)
			throws AnalysisEngineProcessException
		{
			tcount += select(aJCas, Token.class).size();

			long now = System.currentTimeMillis();
			System.out.println("Time: "+(now - start));
			System.out.println("Tokens: "+tcount);
			System.out.println("Tokens per minute: "+((double) tcount/ ((now - start)/60000)));
		}

		@Override
		public void collectionProcessComplete()
			throws AnalysisEngineProcessException
		{
			long now = System.currentTimeMillis();
			System.out.println("Time: "+(now - start));
			System.out.println("Tokens: "+tcount);
			System.out.println("Tokens per minute: "+((double) tcount/ ((now - start)/60000)));
		}
	}
}
