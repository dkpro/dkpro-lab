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

import static org.uimafit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.uimafit.factory.CollectionReaderFactory.createDescription;
import static org.uimafit.util.JCasUtil.select;

import java.io.File;
import java.io.IOException;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.classifier.jar.GenericJarClassifierFactory;
import org.cleartk.classifier.jar.JarClassifierBuilder;
import org.cleartk.classifier.mallet.DefaultMalletCRFDataWriterFactory;
import org.junit.Test;
import org.uimafit.component.JCasAnnotator_ImplBase;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.io.imscwb.ImsCwbWriter;
import de.tudarmstadt.ukp.dkpro.core.io.negra.NegraExportReader;
import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiReader;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiWriter;
import de.tudarmstadt.ukp.dkpro.core.snowball.SnowballStemmer;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.lab.Lab;
import de.tudarmstadt.ukp.dkpro.lab.Util;
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

public class PosExampleCrf
{
	private static final String CORPUS_PATH = "src/test/resources/trainingdata/tuebadz-5.0-first10.export.bz2";

	@Test
	public void run()
		throws Exception
	{
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
						NegraExportReader.PARAM_INPUT_FILE, corpusPath,
						NegraExportReader.PARAM_LANGUAGE, "de");
			}

			@Override
			public AnalysisEngineDescription getAnalysisEngineDescription(TaskContext aContext)
				throws ResourceInitializationException, IOException
			{
				File xmiDir = aContext.getStorageLocation("XMI", AccessMode.READWRITE);
				return createEngine(
						createEngine(SnowballStemmer.class),
						createEngine(XmiWriter.class,
								XmiWriter.PARAM_PATH, xmiDir.getAbsolutePath(),
								XmiWriter.PARAM_COMPRESS, true));
			}
		};

		Task featureExtractionTask = new UimaTaskBase() {
			{
				setType("FeatureExtraction");
			}

			@Override
			public CollectionReaderDescription getCollectionReaderDescription(TaskContext aContext)
				throws ResourceInitializationException, IOException
			{
				File xmiDir = aContext.getStorageLocation("XMI", AccessMode.READONLY);
				xmiDir.mkdirs();
				return createReader(XmiReader.class,
						XmiReader.PARAM_PATH, xmiDir.getAbsolutePath(),
						XmiReader.PARAM_PATTERNS, new String[] { "[+]**/*.xmi.gz" });
			}

			@Override
			public AnalysisEngineDescription getAnalysisEngineDescription(TaskContext aContext)
				throws ResourceInitializationException, IOException
			{
				File modelDir = aContext.getStorageLocation("MODEL", AccessMode.READWRITE);
				modelDir.mkdirs();
				return createEngine(
						createPrimitiveDescription(
								ExamplePosAnnotator.class,
						        ExamplePosAnnotator.PARAM_DATA_WRITER_FACTORY_CLASS_NAME, DefaultMalletCRFDataWriterFactory.class.getName(),
								DefaultMalletCRFDataWriterFactory.PARAM_OUTPUT_DIRECTORY, modelDir.getAbsolutePath()));
			}
		};

		Task trainingTask = new ExecutableTaskBase() {
			{
				setType("TrainingTask");
			}

			@Override
			public void execute(TaskContext aContext)
				throws Exception
			{
				File dir = aContext.getStorageLocation("MODEL", AccessMode.READWRITE);
			    JarClassifierBuilder<?> classifierBuilder = JarClassifierBuilder.fromTrainingDirectory(dir);
			    classifierBuilder.trainClassifier(dir, new String[0]);
			    classifierBuilder.packageClassifier(dir);
			}
		};

		Task analysisTask = new UimaTaskBase() {
			{
				setType("AnalysisTask");
			}

			@Override
			public CollectionReaderDescription getCollectionReaderDescription(TaskContext aContext)
				throws ResourceInitializationException, IOException
			{
				return createDescription(TextReader.class,
						TextReader.PARAM_PATH, "src/test/resources/text",
						TextReader.PARAM_PATTERNS, new String[] { "[+]**/*.txt" },
						TextReader.PARAM_LANGUAGE, "de");
			}

			@Override
			public AnalysisEngineDescription getAnalysisEngineDescription(TaskContext aContext)
				throws ResourceInitializationException, IOException
			{
				File model = new File(aContext.getStorageLocation("MODEL", AccessMode.READONLY), "model.jar");
				File tsv = new File(aContext.getStorageLocation("TSV", AccessMode.READWRITE), "output.tsv");
				return createEngine(
						createPrimitiveDescription(BreakIteratorSegmenter.class),
				        createPrimitiveDescription(SnowballStemmer.class),
				        createPrimitiveDescription(ExamplePosAnnotator.class,
				        		GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH, model.getAbsolutePath()),
		        		createPrimitiveDescription(ImsCwbWriter.class,
		        				ImsCwbWriter.PARAM_OUTPUT_FILE, tsv.getAbsolutePath()));
			}
		};

		ParameterSpace pSpace = new ParameterSpace(Dimension.create("corpusPath", CORPUS_PATH));

		featureExtractionTask.addImportLatest("XMI", "XMI", preprocessingTask.getType());
		trainingTask.addImportLatest("MODEL", "MODEL", featureExtractionTask.getType());
		analysisTask.addImportLatest("MODEL", "MODEL", trainingTask.getType());

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
		File repo = new File("target/repository");
		Util.delete(repo);
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
