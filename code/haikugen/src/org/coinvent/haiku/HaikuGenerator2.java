package org.coinvent.haiku;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.winterwell.maths.stats.distributions.IDistributionBase;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.log.Log;

import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Vector;
import winterwell.maths.stats.distributions.cond.Cntxt;
import winterwell.maths.stats.distributions.cond.ICondDistribution;
import winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import winterwell.nlp.docmodels.IDocModel;
import winterwell.nlp.io.Tkn;
import winterwell.utils.FailureException;
import winterwell.utils.Utils;

/**
 * What is and isn’t haiku: http://www.litkicks.com/EssentialElementsofHaiku
Lists of season marker words: https://en.wikipedia.org/wiki/Saijiki http://www.2hweb.net/haikai/renku/500ESWd.html

 * @author daniel
 *
 */
public class HaikuGenerator2 {

	private static final String LOGTAG = "haiku";
	private LanguageModel languageModel;
	private List<Haiku> haikus;
	private int syllableConstraint[];
//	private int backedoff;
	private Vector topicVector;
	private String topic2;
	private String topic1;
	/**
	 * Constructor
	 * @param languageModel : set corpus for words and relational information
	 * @param haikus : set haikus for generating the grammatical skeleton
	 * @param syllables : syllables constraint, ex: [5,7,5]. Set 0 for no constraint
	 */
	public HaikuGenerator2(LanguageModel languageModel, List<Haiku> haikus, int syllableConstraint[]){
		Utils.check4null(languageModel, haikus, syllableConstraint);
		this.languageModel = languageModel;
		this.haikus = haikus;
		this.syllableConstraint = syllableConstraint;
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
			
			int[] syllables = randomizeSyllable(line);
			if(syllables == null) {
				Log.d(LOGTAG, "failed to fill in syllable template for "+StrUtils.str(line));
				return null;
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
				String picked = generateWord(word, line);
			}
		}
	}
	
	/**
	 * TODO score the text consistency (eg markov chain log-prob)
	 */
	IDocModel docModel;
	
	ICondDistribution<Tkn, Cntxt> wordGen;
	
	String generateWord(WordInfo wordInfo, Line line) {
		String posTag = wordInfo.pos;
		assert posTag != null;		
		assert wordInfo.syllables > 0 : wordInfo;
		
		Cntxt context = new Cntxt(signature, bits);
		Tkn sampled = wordGen.sample(context);
		IFiniteDistribution<Tkn> marginal = (IFiniteDistribution<Tkn>) wordGen.getMarginal(context);
		Tkn mle = marginal.getMostLikely();
						
		String word = sampled.getText();
		wordInfo.setWord(word);
		return word;
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
			String prev = "/s";
			if (i > 0 && lastWord.length() > 0) {
				double logLikelihoodEnd = Math.log(languageModel.getMarkovProbability(lastWord, "/s"));
				double logLikelihoodContinue = Math.log(languageModel.getMarkovProbability(lastWord, result[i][0]));
				ct++;
				score = score + Math.max(logLikelihoodEnd,logLikelihoodContinue);
				assert MathUtils.isFinite(score);
				if (logLikelihoodContinue < logLikelihoodEnd) //is -Infinity
					result[i-1][result[i-1].length - 1] = result[i-1][result[i-1].length - 1] + separator[new Random().nextInt(5)];
					
			}
			for (int j=0;j<result[i].length;j++){
				if (tag[i][j].length() < 2){
					prev = "/s";
					lastWord = "";
					continue;
				}
				lastWord = result[i][j];
				//ct += 1.0;
				double logLikelihood = Math.log(languageModel.getMarkovProbability(prev, result[i][j]));
				//logLikelihood = -Math.log10(8);
				prev = result[i][j];
				if (!(prev.equals("/s") || languageModel.stopWords.contains(prev)) || !languageModel.stopWords.contains(result[i][j])){
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
	 * Uniformly randomize the syllable distribution with dynamic programming
	 * @param N = target syllable
	 * @param tags = array of tag 
	 * @param isKeep = array of integer. If isKeep[i] equal to 1, we keep the original words, thus we force their syllable is equal to the origin.
	 * @return array of integer : the syllable count for each tag
	 */
	public int[] randomizeSyllable(Line line) {
		int numWords = line.words.size();
		assert numWords > 0;
		int res[] = new int[numWords];
		int syllablesLeft = line.syllables;
		assert syllablesLeft > 0;
		
		ArrayList<Integer> options[] = new ArrayList[numWords];
		for (int i=0; i<numWords; i++) {
			WordInfo word = line.words.get(i);
			if (word.fixed) {
				options[i] = languageModel.getPossibleSyllables(tags[i]);
			} else {
				options[i] = new ArrayList<Integer>();
				options[i].add(0);
			}
		}
		//dp[i][j] = 1, if we can make first j words subsequence with total of i syllables
		if (N < 0)
			return null;
		int dp[][] = new int[N+1][M+1];
		dp[0][0] = 1;
		
		//bottom-up dynamic programming
		for (int i=0;i<=N;i++){
			for (int j=0;j<M;j++){
				if (dp[i][j] == 0) 
					continue;
				for (int k=0;k<options[j].size() && i + options[j].get(k) <= N;k++){
					dp[i + options[j].get(k)][j+1] += dp[i][j];
				}
			}
		}
		
		if (dp[N][M] == 0)
			return null;
		
		//uniformly chooses the valid syllables distribution
		Random rand = new Random();
		int pos = N;
		for (int i=M;i>0;i--) {
			int chance = rand.nextInt(dp[pos][i]) + 1;
			for (int j=0;j<options[i-1].size();j++){
				if (pos - options[i-1].get(j) >= 0 && chance <= dp[pos - options[i-1].get(j)][i-1]) {
					res[i-1] = options[i-1].get(j);
					break;
				}
				if (pos - options[i-1].get(j) >= 0)
					chance -= dp[pos - options[i-1].get(j)][i-1];
			}
			pos = pos - res[i-1];
		}
		// set the answers
		for(int i=0; i<line.words.size(); i++) {
			line.words.get(i).syllables = res[i];
		}
		return res;
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
	
	
	
}

