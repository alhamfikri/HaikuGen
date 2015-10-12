package org.coinvent.haiku;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import winterwell.utils.Utils;


public class HaikuGenerator {

	private LanguageModel languageModel;
	private Haiku haikus[];
	private int syllableConstraint[];
	private int haikuSize;
	private int backedoff;
	private ArrayList<Double> topicVector;
	/**
	 * Constructor
	 * @param languageModel : set corpus for words and relational information
	 * @param haikus : set haikus for generating the grammatical skeleton
	 * @param syllables : syllables constraint, ex: [5,7,5]. Set 0 for no constraint
	 */
	public HaikuGenerator(LanguageModel languageModel, Haiku haikus[], int syllableConstraint[]){
		this.languageModel = languageModel;
		this.haikus = haikus;
		this.syllableConstraint = syllableConstraint;
		this.haikuSize = haikus.length;
	}
	
	public Haiku generate() {
		Random random = new Random();
		
		String[][] result = new String[3][];
		String[][] tag = new String[3][];
		int syllable[][] = new int[3][];
		
		for (int i=0;i<3;i++){
			int id = random.nextInt(haikuSize);
			tag[i] = haikus[id].getTag()[i];

			result[i] = new String[tag[i].length];
			
			boolean keepList[] = new boolean[tag[i].length];
			int penalty = 0;
			
			//filter the words that will not be removed
			if (Config.isKeep) {
				for (int j=0;j<tag[i].length;j++){
					if (languageModel.stopWords.contains(haikus[id].getWord()[i][j]) ){
						penalty += languageModel.getSyllable(haikus[id].getWord()[i][j]);
						result[i][j] = haikus[id].getWord()[i][j];
						keepList[j] = true;
					}
				}
			}
			
			syllable[i] = randomizeSyllable(syllableConstraint[i] - penalty,tag[i], keepList);
			if(syllable[i] == null)
				return null;
		}
		
		String topic = "";
		ArrayList<Double> focusedTopicVector = topicVector;
		double topicScore = 0.0;
		int topicSplit = 1 + random.nextInt(2);
		int topicCount = 0;
		for (int i=0;i<tag.length;i++){
			if (i == topicSplit) {
				topic = "";
				focusedTopicVector = topicVector;
			}
				
			
			String words[] = null;
			
			for (int j=0;j<tag[i].length;j++){
				int divider = 50;
				if (topic.length() == 0)
					divider = 20;
				if (languageModel.isTopicTag(tag[i][j])==false)
					divider = 1;
				
				words = languageModel.getWordlist(tag[i][j],syllable[i][j]);
				if (result[i][j] != null)
					continue;
				String prev = "/s";
				if (j > 0)
					prev = result[i][j-1];
				String word = words[random.nextInt(words.length)];
				ArrayList<StringDouble> wordPool = new ArrayList<StringDouble>();
				ArrayList<StringDouble> uniWordPool = new ArrayList<StringDouble>();
				
				//put all next possible word into word pool
				for (int k=0;k<words.length;k++){
				
					double dist = languageModel.getDistance(focusedTopicVector, words[k]);
					
					
					int count = languageModel.getMarkovCount(prev,words[k]);
					
					if (count > 3)
						wordPool.add(new StringDouble(words[k],dist));
					else
						uniWordPool.add(new StringDouble(words[k],dist));
				}
				
				//if exsists some words that has bigram occurence with the previous word, pick one randomly,
				//from top K of the list, sorted by its closeness with the topics
				if (wordPool.size() > 0) {
					Collections.sort(wordPool);
					int bound = (divider - 1 + wordPool.size())/divider;
					
					word = wordPool.get(random.nextInt(bound)).s;
					}
				//else, pick any word with unigram occurence (backoff)
				else {
					Collections.sort(uniWordPool);
					int bound = (divider - 1 + uniWordPool.size())/divider;
					
					word = uniWordPool.get(random.nextInt(bound)).s;
					
				}
				
				if (topic.length() == 0 && languageModel.isTopicTag(tag[i][j]) && languageModel.getVector(word) != null){
					//System.err.println("Focusing on topic: "+word);
					topic = word;
					//focusedTopicVector = WordVector.add(topicVector, languageModel.getVector(word));
							
				}
				result[i][j] = word;
				//updates the score
				double dist = languageModel.getDistance(topicVector, word);
				if (dist < 1000)
					topicScore += dist*dist;
					topicCount++;	
				
				
			}
		}
		
		//post-processing the Haiku, fixes some inconsistencies
		postProcessing(result);
		double sc = getModelScore(result,tag);
		Haiku haiku = new Haiku(result, tag, topicScore / topicCount, sc , 0);
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
					if (logLikelihood < -10000)
						result[i][j] = result[i][j];
					ct++;
				}
			}
		}
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

	public Haiku generate(String keywords) {
		assert ! Utils.isBlank(keywords);
		//backedoff = 0;
		//System.err.println("Creating haiku, main idea "+keywords);
		topicVector = new ArrayList<Double>();
		for (int i=0;i<300;i++)
			topicVector.add(.0);
		
		String[] words = keywords.split(" ");
		for (int i=0;i<words.length;i++) {
			ArrayList<Double> v = languageModel.getVector(words[i]);
			if (v != null)
				topicVector = WordVector.add(topicVector, v);	
		}
		//System.out.println("before "+topicVector.get(0));
		Haiku res = generate();
		//System.out.println("after "+topicVector.get(0));
		return res;
	}
	
	
	
}

