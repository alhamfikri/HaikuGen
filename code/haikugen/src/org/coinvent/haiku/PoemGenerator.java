package org.coinvent.haiku;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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
import winterwell.nlp.docmodels.IDocModel;
import winterwell.nlp.io.SitnStream;
import winterwell.nlp.io.Tkn;
import winterwell.utils.FailureException;
import winterwell.utils.Utils;

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
			Line line = poem.lines[i];
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
			// syllable counts
			
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
			// sanity check on line
			for(WordInfo wi : line.words) {
				assert wi.fixed || wi.syllables>0 : wi;
			}
		}
		
		// Fill in words
		generate3_fillInWords(poem);
		
		//post-processing the Haiku, fixes some inconsistencies
		postProcessing(poem);
		return poem;
	}
	
	void generate3_fillInWords(Poem poem) {
		double topicScore = 0.0;
		int topicCount=0;
		for(Line line : poem.lines) {
			for(WordInfo word : line.words) {
				if (word.fixed) continue;
				if (word.syllables==0) continue;
				Tkn picked = generateWord(word, line);
				word.setWord(picked.getText());
			}
		}
	}
	
	/**
	 * TODO score the text consistency (eg markov chain log-prob)
	 */
	IDocModel docModel;
	
	ICondDistribution<Tkn, Cntxt> wordGen;
	
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
		assert wordInfo.syllables > 0 : wordInfo;
		int wi = line.words.indexOf(wordInfo);
		assert wi != -1 : wordInfo+" not in "+line;
		String[] sig = new String[]{Tkn.POS.name,"w-2","w-1","w+1"};
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
		return languageModel.stopWords.contains(word);
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
	

	private double getModelScore(String[][] result, String[][] tag) {
		double score = 0.0;
		String lastWord = "";
		double ct = 0;
		String[] separator = {"","","...",",","--"};
		for (int i=0;i<result.length;i++){
			String prev = Tkn.START_TOKEN.getText();
			if (i > 0 && lastWord.length() > 0) {
				double logLikelihoodEnd = Math.log(languageModel.getMarkovProbability(lastWord, Tkn.END_TOKEN.getText()));
				double logLikelihoodContinue = Math.log(languageModel.getMarkovProbability(lastWord, result[i][0]));
				ct++;
				score = score + Math.max(logLikelihoodEnd,logLikelihoodContinue);
				assert MathUtils.isFinite(score);
				if (logLikelihoodContinue < logLikelihoodEnd) //is -Infinity
					result[i-1][result[i-1].length - 1] = result[i-1][result[i-1].length - 1] + separator[new Random().nextInt(5)];
					
			}
			for (int j=0;j<result[i].length;j++){
				if (tag[i][j].length() < 2){
					prev = Tkn.START_TOKEN.getText();
					lastWord = "";
					continue;
				}
				lastWord = result[i][j];
				//ct += 1.0;
				double logLikelihood = Math.log(languageModel.getMarkovProbability(prev, result[i][j]));
				//logLikelihood = -Math.log10(8);
				prev = result[i][j];
				if ( ! (prev.equals(Tkn.START_TOKEN.getText()) || languageModel.stopWords.contains(prev)) || !languageModel.stopWords.contains(result[i][j])){
					score = score + logLikelihood;
					assert MathUtils.isFinite(score);
					if (logLikelihood < -10000)
						result[i][j] = result[i][j];
					ct++;
				}
			}
		}
		assert MathUtils.isFinite(score);
		assert ct > 0;
		return (score*0.01)/ct;
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
		SitnStream ss;
		return 0;
	}

	public Poem generate(String topic) {
		return generate(topic, null);
	}
	
	
	
}

