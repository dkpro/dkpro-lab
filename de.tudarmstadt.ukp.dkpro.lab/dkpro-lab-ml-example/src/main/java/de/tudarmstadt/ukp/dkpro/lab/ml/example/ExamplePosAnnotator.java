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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.classifier.CleartkSequenceAnnotator;
import org.cleartk.classifier.Instance;
import org.cleartk.classifier.feature.extractor.ContextExtractor;
import org.cleartk.classifier.feature.extractor.ContextExtractor.Following;
import org.cleartk.classifier.feature.extractor.ContextExtractor.Preceding;
import org.cleartk.classifier.feature.extractor.simple.SimpleFeatureExtractor;
import org.cleartk.classifier.feature.extractor.simple.SpannedTextExtractor;
import org.cleartk.classifier.feature.extractor.simple.TypePathExtractor;
import org.cleartk.classifier.feature.proliferate.CapitalTypeProliferator;
import org.cleartk.classifier.feature.proliferate.CharacterNGramProliferator;
import org.cleartk.classifier.feature.proliferate.LowerCaseProliferator;
import org.cleartk.classifier.feature.proliferate.NumericTypeProliferator;
import org.cleartk.classifier.feature.proliferate.ProliferatingExtractor;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class ExamplePosAnnotator
	extends CleartkSequenceAnnotator<String>
{
	private List<SimpleFeatureExtractor> tokenFeatureExtractors;

	private List<ContextExtractor<Token>> contextFeatureExtractors;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
	    super.initialize(context);
	    // alias for NGram feature parameters
	    int fromRight = CharacterNGramProliferator.RIGHT_TO_LEFT;

		// a list of feature extractors that require only the token: the stem of the word, the text
		// of the word itself, plus features created from the word text like character ngrams
	    this.tokenFeatureExtractors = asList(
	        new TypePathExtractor(Token.class, "stem/value"),
	        new ProliferatingExtractor(
	            new SpannedTextExtractor(),
	            new LowerCaseProliferator(),
	            new CapitalTypeProliferator(),
	            new NumericTypeProliferator(),
	            new CharacterNGramProliferator(fromRight, 0, 2),
	            new CharacterNGramProliferator(fromRight, 0, 3)));

	    // a list of feature extractors that require the token and the sentence
	    this.contextFeatureExtractors = singletonList(new ContextExtractor<Token>(Token.class,
	        new TypePathExtractor(Token.class, "stem"), new Preceding(2), new Following(2)));

	  }
	@Override
	public void process(JCas jCas)
		throws AnalysisEngineProcessException
	{
		Collection<TOP> addToIndexes = new ArrayList<TOP>();

		// generate a list of training instances for each sentence in the  document
		for (Sentence sentence : select(jCas, Sentence.class)) {
			List<Instance<String>> instances = new ArrayList<Instance<String>>();
			List<Token> tokens = selectCovered(jCas, Token.class, sentence);

			// for each token, extract all feature values and the label
			for (Token token : tokens) {
				Instance<String> instance = new Instance<String>();

				// extract all features that require only the token annotation
				for (SimpleFeatureExtractor extractor : this.tokenFeatureExtractors) {
					instance.addAll(extractor.extract(jCas, token));
				}

				// extract all features that require the token and sentence annotations
				for (ContextExtractor<Token> extractor : this.contextFeatureExtractors) {
					instance.addAll(extractor.extractWithin(jCas, token, sentence));
				}

				// set the instance label from the token's part of speech
				if (this.isTraining()) {
					instance.setOutcome(token.getPos().getPosValue());
				}

				// add the instance to the list
				instances.add(instance);
			}

			if (this.isTraining()) {
				// for training, write instances to the data write
				this.dataWriter.write(instances);
			}
			else {
				// for classification, set the labels as the token POS labels
				Iterator<Token> tokensIter = tokens.iterator();
				List<String> labels = classify(instances);
				for (String label : labels) {
					Token t = tokensIter.next();
					POS pos = t.getPos();
					if (pos == null) {
						pos = new POS(jCas, t.getBegin(), t.getEnd());
						addToIndexes.add(pos);
						t.setPos(pos);
					}
					pos.setPosValue(label);
				}
			}

			for (TOP fs : addToIndexes) {
				fs.addToIndexes();
			}
		}
	}

}
