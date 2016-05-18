package org.coinvent.haiku;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.winterwell.utils.MathUtils;

import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Vector;
import winterwell.utils.Utils;


public class HaikuGenerator2 {

	private LanguageModel languageModel;
	private List<Haiku> haikus;
	private int syllableConstraint[];
	private int backedoff;
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
	
	Haiku generate2() {
		Random random = new Random();
		
		String[][] result = new String[3][];
		// [line][word] -> POS tag
		String[][] postagFromLineWord = new String[3][];
		int syllable[][] = new int[3][];
		
		// Make a POS skeleton
		for (int i=0;i<3;i++){
			int id = random.nextInt(haikus.size());
			Haiku haikuTemplate = haikus.get(id);
			postagFromLineWord[i] = haikuTemplate.getTag()[i];

			result[i] = new String[postagFromLineWord[i].length];
			
			boolean keepList[] = new boolean[postagFromLineWord[i].length];
			int penalty = 0;
			
			//filter the words that will not be removed
			if (Config.isKeep) {
				for (int j=0;j<postagFromLineWord[i].length;j++){
					if (languageModel.stopWords.contains(haikuTemplate.getWord()[i][j]) ){
						penalty += languageModel.getSyllable(haikuTemplate.getWord()[i][j]);
						result[i][j] = haikuTemplate.getWord()[i][j];
						keepList[j] = true;
					}
				}
			}
			
			syllable[i] = randomizeSyllable(syllableConstraint[i] - penalty,postagFromLineWord[i], keepList);
			if(syllable[i] == null)
				return null;
		}
		
		// Fill in words
		double topicScore = 0.0;
		int topicCount=0;
		for (int i=0;i<postagFromLineWord.length;i++){						
			for (int j=0;j<postagFromLineWord[i].length;j++){								
				if (result[i][j] != null) continue;
				String posTag = postagFromLineWord[i][j];
				String words[] = languageModel.getWordlist(posTag,syllable[i][j]);
				String prev = j==0? "/s" : result[i][j-1];
				ArrayList<StringDouble> wordPool = new ArrayList<StringDouble>();
				ArrayList<StringDouble> uniWordPool = new ArrayList<StringDouble>();
				
				//put all next possible word into word pool
				for (String word : words){				
					double dist = languageModel.getDistance(topicVector, word);					
					int count = languageModel.getMarkovCount(prev,word);					
					if (count > 3) {
						wordPool.add(new StringDouble(word,dist));
					} else {
						uniWordPool.add(new StringDouble(word,dist));
					}
				}
				
				//if exsists some words that has bigram occurence with the previous word, pick one randomly,
				//from top K of the list, sorted by its closeness with the topics
				int k = 10;
				String word = null;
				if (wordPool.size() > 0) {
					Collections.sort(wordPool);
					int bound = Math.min(k, wordPool.size());					
					word = wordPool.get(random.nextInt(bound)).s;
				} else { //else, pick any word with unigram occurence (backoff)
					Collections.sort(uniWordPool);
					int bound = Math.min(k, uniWordPool.size());					
					word = uniWordPool.get(random.nextInt(bound)).s;					
				}
				if (word==null) {
					word = words[random.nextInt(words.length)];
				}
								
				result[i][j] = word;
				//updates the score
				double dist = languageModel.getDistance(topicVector, word);
				if (dist < 1000) {
					topicScore += dist*dist;
					topicCount++;	
				}				
			}
		}
		
		//post-processing the Haiku, fixes some inconsistencies
		postProcessing(result);
		double sc = getModelScore(result,postagFromLineWord);
		Haiku haiku = new Haiku(result, postagFromLineWord, topicScore / topicCount, sc , 0);
		if (sc < -999999)
			backedoff++;
		return haiku;
	}

	private void postProcessing(String[][] result) {
		for (int i=0;i<result.length;i++){
			for (int j=1;j<result[i].length;j++){
				//fix a/an inconsistency
				if (result[i][j-1].equals("a") && "aeiou".indexOf(result[i][j].charAt(0)) >= 0){
					result[i][j-1] = "the";
				} else
				if (result[i][j-1].equals("an") && "aeiou".indexOf(result[i][j].charAt(0)) < 0){
					result[i][j-1] = "the";
				}
			}
		}
	}
	
	public double getSimilarityScore(){
		
		return 0.0;
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
	public int[] randomizeSyllable(int N, String[] tags, boolean isKeep[]) {
		int M = tags.length;
		int res[] = new int[M];
		
		ArrayList<Integer> options[] = new ArrayList[M];
		for (int i=0;i<M;i++) {
			if (isKeep == null || !isKeep[i])
				options[i] = languageModel.getPossibleSyllables(tags[i]);
			else 
				{
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
		return res;
	}

	/**
	 * 
	 * @param topic1
	 * @param topic2 Can be null
	 * @return
	 */
	public Haiku generate(String topic1, String topic2) {		
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

		List<Haiku> candidates = new ArrayList();
		for(int i=0; i<5; i++) {
			Haiku res = generate2();
			if (res==null) continue;
			candidates.add(res);
		}
		Collections.sort(candidates);
		Haiku winner = candidates.get(0);
		return winner;
	}
	
	
	
}

