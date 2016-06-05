package org.coinvent.haiku;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.winterwell.maths.stats.distributions.IDistributionBase;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;

import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Vector;
import winterwell.maths.stats.distributions.cond.Cntxt;
import winterwell.maths.stats.distributions.cond.ICondDistribution;
import winterwell.maths.stats.distributions.cond.Sitn;
import winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import winterwell.maths.timeseries.DataUtils;
import winterwell.maths.vector.VectorUtilsTest;
import winterwell.nlp.NLPWorkshop;
import winterwell.nlp.docmodels.IDocModel;
import winterwell.nlp.io.SitnStream;
import winterwell.nlp.io.Tkn;
import winterwell.utils.FailureException;
import winterwell.utils.Utils;
import winterwell.utils.web.XStreamUtils;

/**
 * What is and isn’t haiku: http://www.litkicks.com/EssentialElementsofHaiku
Lists of season marker words: https://en.wikipedia.org/wiki/Saijiki http://www.2hweb.net/haikai/renku/500ESWd.html

 * @author daniel
 *
 */
public class PoemGenerator {

	public void setDocModel(IDocModel docModel) {
		this.docModel = docModel;
	}
	
	private static final String LOGTAG = "haiku";
	private LanguageModel languageModel;
	private List<Haiku> haikus;
	private int syllableConstraint[];
//	private int backedoff;
	private Vector topicVector;
	private String topic2;
	private String topic1;
	private PoemVocab vocab;
	/**
	 * Constructor
	 * @param languageModel : set corpus for words and relational information
	 * @param haikus : set haikus for generating the grammatical skeleton
	 * @param syllables : syllables constraint, ex: [5,7,5]. Set 0 for no constraint
	 */
	public PoemGenerator(LanguageModel languageModel, List<Haiku> haikus, int syllableConstraint[]){
		Utils.check4null(languageModel, haikus, syllableConstraint);
		this.languageModel = languageModel;
		this.vocab = languageModel.allVocab;
		this.haikus = haikus;
		this.syllableConstraint = syllableConstraint;
	}
	
	public void setVocab(PoemVocab vocab) {
		this.vocab = vocab;
	}
	
	Poem generate2() {
		Poem poem = new Poem(syllableConstraint);
		Random random = new Random();
				
		// Make a POS skeleton
		for (int i=0; i<poem.lines.length; i++) {
			final Line line = poem.lines[i];
			// pick a haiku to provide the template for this line
			int id = random.nextInt(haikus.size());
			Haiku haikuTemplate = haikus.get(id);
			String[] postagsForLine = haikuTemplate.getTag()[i];
			line.words = new ArrayList();
			for(String pos : postagsForLine) {
				WordInfo wi = new WordInfo();
				wi.pos = pos;
				line.words.add(wi);
			}
			
			// fix the words that will not be removed
			if (Config.isKeep) {
				String[] linei = haikuTemplate.getWord()[i];
				for(int j=0; j<linei.length; j++){
					String word = linei[j];
					if (keepTemplateWord(word)) {
						WordInfo wi = line.words.get(j);
						wi.setWord(word);
						wi.fixed = true;
					}
				}
			}
			
			SyllableAssignment sa = new SyllableAssignment(line, vocab);
			int[] syllables = sa.randomizeSyllable();
			if(syllables == null) {
				Log.d(LOGTAG, "failed to fill in syllable template for "+StrUtils.str(line));
				return null;
			}			
			
			// sanity check: syllables set
			for(WordInfo wi : line.words) {
				assert wi.syllables() >= 0;
			}
		}
		
		// Fill in words
		generate3_fillInWords(poem);
		
		// sanity check: words filled in
		for(Line line : poem.lines) {
			for(WordInfo wi : line.words) {
				assert ! Utils.isBlank(wi.word) : line;
			}
		}
		
		//post-processing the Haiku, fixes some inconsistencies
		postProcessing(poem);
		return poem;
	}
	
	void generate3_fillInWords(Poem poem) {
		double topicScore = 0.0;
		int topicCount=0;
		for(Line line : poem.lines) {
			for(WordInfo word : line.words) {
				if (word.fixed) {
					assert ! word.word.isEmpty();
					assert keepTemplateWord(word.word) : word;
					assert ! Utils.isBlank(word.word) : line;
					continue;
				}
				if (word.syllables()==0) {
					// punctuation -- should have been filled in from the punctuation
					assert ! Utils.isBlank(word.word) : line;
					continue;
				}
				Tkn picked = generateWord(word, line);
				word.setWord(picked.getText());
				assert ! Utils.isBlank(picked.getText());
			}
			for(WordInfo wi : line.words) {
				assert ! Utils.isBlank(wi.word) : line;
			}
		} // end for-each-line
	}
	
	/**
	 * TODO score the text consistency (eg markov chain log-prob)
	 */
	IDocModel docModel;
	
	ICondDistribution<Tkn, Cntxt> wordGen;

	double topicScoreWeight = 1;
	double senseScoreWeight = 1;
	double focusScoreWeight = 1;
	
	public void setWordGen(ICondDistribution<Tkn, Cntxt> wordGen) {
		this.wordGen = wordGen;
	}
	
	/**
	 * 
	 * @param wordInfo Will not be modified
	 * @param line
	 * @return
	 */
	Tkn generateWord(WordInfo wordInfo, Line line) {
		assert wordInfo!=null;
		// context
		String posTag = wordInfo.pos;
		assert posTag != null;		
		assert wordInfo.syllables() > 0 : wordInfo;
		int wi = line.words.indexOf(wordInfo);
		assert wi != -1 : wordInfo+" not in "+line;
		String[] sig = LanguageModel.get().sig;
		SitnStream ss = new SitnStream(null, line, sig);
		List<Sitn<Tkn>> list = Containers.list(ss);
		Cntxt context = list.get(wi).context;
		
		// sample
		Tkn sampled = wordGen.sample(context);
		
		IFiniteDistribution<Tkn> marginal = (IFiniteDistribution<Tkn>) wordGen.getMarginal(context);
		Tkn mle = marginal.getMostLikely();
						
		return sampled;
	}

	private boolean keepTemplateWord(String word) {
		if (languageModel.stopWords.contains(word)) return true;
		// punctuation?
		if (StrUtils.isPunctuation(word)) return true;
		return false;
	}

	private void postProcessing(Poem poem) {
		for (Line line : poem.lines) {
			//fix a/an inconsistency
			for (int j=1; j<line.words.size(); j++) {
				WordInfo word = line.words.get(j);
				WordInfo prev = line.words.get(j-1);
				if (prev.word.equals("a") && "aeiou".indexOf(word.word.charAt(0)) != -1) {
					prev.setWord("the");
				} else if (prev.word.equals("an") && "aeiou".indexOf(word.word.charAt(0)) == -1) {
					prev.setWord("the");
				}
			}
		}
	}
	

	

	/**
	 * 
	 * @param topic1
	 * @param topic2 Can be null
	 * @return
	 */
	public Poem generate(String topic1, String topic2) {		
		assert ! Utils.isBlank(topic1);
		String keywords = topic1;
		if (topic2!=null) keywords += " "+topic2;
		this.topic1 = topic1;
		this.topic2 = topic2;
		topicVector = null;
		
		String[] words = keywords.split(" ");
		for (int i=0;i<words.length;i++) {
			Vector v = languageModel.getVector(words[i]);
			if (v == null) continue;
			if (topicVector==null) topicVector = v.copy();
			else topicVector = topicVector.add(v);	
		}

		List candidates = new ArrayList();
		for(int i=0; i<Config.batchSize; i++) {
			Poem res = generate2();
			res.score = score(res);
			if (res==null) continue;
			assert res.score != -1;
			candidates.add(res);
		}
		if (candidates.isEmpty()) {
			throw new FailureException("no poem found :(");
		}
		Collections.sort(candidates);
		Poem winner = (Poem) candidates.get(0);
		return winner;
	}

	private double score(Poem res) {
		// TODO
		// topic: overall topic-vector score
		double topicScore = 0;
		Vector poemTopic = null;
		for(Line line : res.lines) {
			for(WordInfo wi : line.words) {
				Vector vector = LanguageModel.get().getVector(wi.word);
				if (vector == null) continue;
				if (poemTopic==null) poemTopic = vector;
				else poemTopic = poemTopic.add(vector);
			}
		}
		DataUtils.normalise(poemTopic);
		if (poemTopic!=null && topicVector!=null) {
			DataUtils.normalise(topicVector);
			topicScore = Math.abs(poemTopic.dot(topicVector));
		}
		
		// aesthetics: similar-words without repetition
		int repeats=0;
		HashSet<String> seen = new HashSet();
		Set<String> stopwords = NLPWorkshop.get().getStopwords();
		double focusScore = 0;
		for(Line line : res.lines) {
			for(WordInfo wi : line.words) {
				if (StrUtils.isWord(wi.word) && ! stopwords.contains(wi.word)) {
					if (seen.contains(wi.word)) {
						repeats ++;
					}
					seen.add(wi.word);
				}
				Vector vector = LanguageModel.glove.getVector(wi.word);
				if (vector == null) continue;
				DataUtils.normalise(vector);
				focusScore += Math.abs(poemTopic.dot(vector));
			}
		}
		if (repeats!=0) {
			focusScore = focusScore / (100*repeats);
		}
		
		// sense: multiply per-word scores
		double senseScore = 1;
		for(Line line : res.lines) {
			double logProb = 0;
			SitnStream ss = new SitnStream(null, line, LanguageModel.sig);
//			for (Sitn<Tkn> sitn : ss) {
//				double lp = languageModel.allWordModel.logProb(sitn.outcome, sitn.context);
//				logProb += lp;
//			}
		}		
		// weight them (add a bit in to avoid 0s)
		double ts = 0.0000000001 + topicScore*topicScoreWeight;
		double ss = 0.0000000001 + senseScore*senseScoreWeight;
		double fs = 0.0000000001 + focusScore*focusScoreWeight;
		// balance them, F-Score style
		return 3*ts*ss*fs / (ts+ss+fs);
	}

	public Poem generate(String topic) {
		return generate(topic, null);
	}
	
	
	
}

