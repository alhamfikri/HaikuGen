package org.coinvent.haiku;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.io.FileUtils;

import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Vector;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import winterwell.nlp.analysis.SyllableCounter;
import winterwell.nlp.io.ITokenStream;
import winterwell.nlp.io.Tkn;
import winterwell.nlp.io.WordAndPunctuationTokeniser;
import winterwell.nlp.io.pos.PosTagByOpenNLP;
import winterwell.nlp.vectornlp.GloveWordVectors;
import winterwell.utils.Utils;
import winterwell.utils.reporting.Log;

/**
 * 
 * @author Alham Fikri Aji
 * corpus used for generating Haiku
 * it does not store the original text, but rather some informations in predefined structure
 * Unique Wordlist : captures all unique words corpus
 * Words relation  : 
 * Word list based on Tag : Given a PoS tag/label, all possible words with that label are provided, sorted by their probability.
 */
public class LanguageModel {
	
	ITokenStream brownTokeniser;
	ITokenStream tweetTokeniser = new WordAndPunctuationTokeniser().setLowerCase(true);
	
	//set of unique words
	private HashMap<String,WordInfo> wordDatabase;

	
	static final GloveWordVectors glove = new GloveWordVectors();

	private static LanguageModel dflt;
	
	/**
	 * The POS tags where we want topic-aligned choices
	 */
	private HashSet<String> topicTagSet = new HashSet(Arrays.asList("JJ", "JJR", "JJS", "NN", "NNS" , "NNP" , "NNPS" , "RB", "RBS", "RBR", 
			"VB", "VBD" , "VBG", "VBN" , "VBP" , "VBZ"));
	
	/** global list of removed words */
	private HashSet<String> unusedWords;
	
	//global list of used words
	private HashSet<String> dictionary;
	HashSet<String> stopWords;
	
	
	//markov chain model
	MarkovModel markov;
	
	private LanguageModel() {
		markov = new MarkovModel();
		dictionary = new HashSet<String>();
		unusedWords = new HashSet<String>();
		stopWords = new HashSet<String>();
		
		wordDatabase = new HashMap<String,WordInfo>();		
		wordDatabase.put(Tkn.START_TOKEN.getText(), new WordInfo(Tkn.START_TOKEN.getText(), 0) );

		
		
		addSpecialSeparator("...",":");
		addSpecialSeparator("..",":");
		addSpecialSeparator(",",":");
		addSpecialSeparator("--",":");
		addSpecialSeparator(";",":");
		addSpecialSeparator(",",",");
		addSpecialSeparator(".",".");
		addSpecialSeparator("",".");
		addSpecialSeparator("?",".");
		addSpecialSeparator("!",".");
		addSpecialSeparator("..",".");
	}
	
	private void addSpecialSeparator(String separator, String tag) {
		allVocab.addWord(new WordInfo(separator, 0).setPOS(tag));
	}

	/**
	 * Open forbidden words dictionary
	 */
	public void loadForbiddenDictionary(String filepath) {
		String currentDirectory = System.getProperty("user.dir");
		String line = null;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(currentDirectory + "/res/name/" + filepath));
			String input;
			while ((input = br.readLine()) != null) {
				unusedWords.add(input);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
	}
	
	/**
	 * Open and load stop-words list
	 */
	public void loadStopWords(String filepath) {
		String currentDirectory = System.getProperty("user.dir");
		String line = null;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(currentDirectory + "/res/stop-words/" + filepath));
			String input;
			while ((input = br.readLine()) != null) {
				stopWords.add(input);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	public void loadDictionary(String filepath) {
		String currentDirectory = System.getProperty("user.dir");
		String line = null;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(currentDirectory + "/res/model/" + filepath));
			String input;
			while ((input = br.readLine()) != null) {
				dictionary.add(input);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
	}
	
	
	/**
	 * Open CMUDictionary dictionary for initial word base
	 * @param filepath location of CMUDict
	 * @throws IOException 
	 */
	public void loadSyllableDictionary(String filepath) throws IOException {
		String currentDirectory = System.getProperty("user.dir");
		String line = null;
		File file = new File(currentDirectory + "/res/model/" + filepath);
		assert file.isFile() : "No "+filepath+" -> "+file;
		BufferedReader br = FileUtils.getReader(file);
		String input;
		while ((input = br.readLine()) != null) {
			String parsed[] = input.toLowerCase().split(" ");
			String word = parsed[0];
			int syllables = 1;
			for (int i=1;i<parsed.length;i++){
				if (parsed[i].length() > 0 && parsed[i].charAt(0) == '-'){
					syllables++;
				}
			}
			
			if ( ! word.matches("[a-zA-Z]+")) {
				continue;
			}
			
			//if unused
			if (unusedWords.contains(word))
				continue;
			
			if ( ! dictionary.contains(word))
				continue;
			
			WordInfo w = new WordInfo(word, syllables);
			wordDatabase.put(word, w);
			
			List<String> tags = PosTagByOpenNLP.getPossibleTags(word);
			
			//updates wordlist by syllables
            for (String posTag  : tags) {
            	WordInfo wi = new WordInfo(word, syllables);
            	wi.setPOS(posTag);
            	allVocab.addWord(wi);
            }
		}
		
		br.close();
	}
	
	
	public void trainMarkov(ArrayList<ArrayList<String>> data) {
		int N = data.size();
		for (int i=0;i<N;i++){
			int M = data.get(i).size();
			String prev = Tkn.START_TOKEN.getText();
			for (int j=0;j<M;j++) {
				if (data.get(i).get(j).equals(Tkn.START_TOKEN.getText()))
					data.get(i).set(j, Tkn.START_TOKEN.getText());
				if (wordDatabase.get(data.get(i).get(j)) != null && wordDatabase.get(prev) != null)
					markov.add(prev,data.get(i).get(j));
				prev = data.get(i).get(j);
				
			}
			if (wordDatabase.get(prev) != null)
				markov.add(prev,Tkn.START_TOKEN.getText());
			/*
			//updates list of possible words
			String tags[] = HaikuPOSTagger.tag(data.get(i));
			for (int j=0;j<tags.length;j++){
            	String word = data.get(i).get(j);
            	WordInfo wordInfo = wordDatabase.get(word);
            	if (wordInfo == null) 
            		continue;
            	
            	//updates wordlist by syllables
            	HashSet<String> wordset = wordlist[wordInfo.syllables].get(tags[j]);
            	if (wordset == null) {
            		wordset = new HashSet<String>();
            		wordlist[wordInfo.syllables].put(tags[j], wordset);
            	}
            	wordset.add(word);
            	
            	//updates global wordlist 
            	HashSet<String> wordset2 = allWordlist.get(tags[j]);
            	if (wordset2 == null) {
            		wordset2 = new HashSet<String>();
            		allWordlist.put(tags[j], wordset2);
            	}
            	wordset2.add(word);
            }*/
			
		}
		
	}
	
	
	
	final PoemVocab allVocab = new PoemVocab();


	public static String[] sig = new String[]{"w-2","w-1","w+1"};
		
	/**
	 * 
	 * @param word : string of word
	 * @return WordInfo of given word
	 */
	public WordInfo getWordInfo(String word) {
		return wordDatabase.get(word);
	}

	/**
	 * get a syllable for count a given word
	 * @param word
	 * @return int: the number of syllables or 0
	 */
	public int getSyllable(String word){
		WordInfo w = wordDatabase.get(word);
		if (w != null && w.syllables > 0) return w.syllables;
		return SyllableCounter.syllableCount(word);
	}
	
	public int getMarkovCount(String string, String word2) {
		return markov.getCount(string,word2);
	}
	
	
	public void loadMarkovModel(String filepath) {
		
	}
	
	/**
	 * Save current trained model, so you don't have to re-train the whole markov again
	 * @param filepath
	 */
	public void saveMarkovModel(String filename) {
		String currentDirectory = System.getProperty("user.dir");
		System.err.println("Saving model... ");
		File file = new File(currentDirectory + "/res/model/" + filename);
		BufferedWriter bw = null;
		// if file doesnt exists, then create it
	
		try {
			if (!file.exists())
				file.createNewFile();
			
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			bw = new BufferedWriter(fw);
			
			for (String key : wordDatabase.keySet()) {

			    ArrayList<String> nexts = markov.getAllPossiblePairs(key);
			    if (nexts == null)
			    	continue;
			    
			    for (int i=0;i<nexts.size();i++)
			    	bw.write(key+" "+nexts.get(i)+" "+markov.getCount(key,nexts.get(i)) + "\n");
			    
			}
			
			//bw.close();
			System.err.println("Done ");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	
			System.err.println("FAILED TO SAVE THE MODEL :(");
		}
	}

	public double getMarkovProbability(String prev, String next) {
		double p = markov.getProbability(prev,next) + 0.0000001;		
		assert MathUtils.isProb(p) && p!=0 : prev+" -> "+next;
		return p;
	}

	public int getUnigramCount(String string) {
		
		return markov.getCount(string);
	}
	
	public double getDistance(String s1, String s2) {		
		Vector v1 = glove.getVector(s1);
		Vector v2 = glove.getVector(s2); 
		
		if (v1 == null || v2 == null)
			return 9999;
		return -WordVector.cosineSimilarity(v1,v2);
		
		//return WordVector.dotProduct(v1,v2);
		//return WordVector.euclidian(v1,v2);
	}


	
	public double getDistance(Vector v1, String s2) {
		Vector v2 = glove.getVector(s2); 
		
		if (v1 == null || v2 == null)
			return 9999;
		
		return -WordVector.cosineSimilarity(v1,v2);
		//return WordVector.dotProduct(v1,v2);
		//return WordVector.euclidian(v1,v2);
	}
	
	public String[] getClosestWords(String word, int K) {
		String[] res = new String[K];
		ArrayList<StringDouble> candidates = new ArrayList<StringDouble>();
		
		//construct vector representation of the keyword
		Vector topicVector = new DenseVector(300);
		
		String[] words = word.split(" ");
		for (int i=0;i<words.length;i++) {
			Vector v = getVector(words[i]);
			if (v != null)
				topicVector = topicVector.add(v);	
		}
		
		for (String key : wordDatabase.keySet()) {
			double dist = getDistance(topicVector, key);
			candidates.add(new StringDouble(key,dist));
		}
		Collections.sort(candidates);
		for (int i=0;i<K;i++)
			res[i] = candidates.get(i).s;
		
		return res;
	}
	
	public String[] getClosestWords(Vector v, int K) {
		String[] res = new String[K];
		ArrayList<StringDouble> candidates = new ArrayList<StringDouble>();
		for (String key : wordDatabase.keySet()) {
			double dist = getDistance(v, key);
			candidates.add(new StringDouble(key,dist));
		}
		Collections.sort(candidates);
		for (int i=0;i<K;i++)
			res[i] = candidates.get(i).s;
		
		return res;
	}

	public String[] getClosestPattern(String x, String y, String z, int K) {
		//pattern: y - x = result - z
		//ex: men to women is like king to [queen]
		Vector vx = getVector(x);
		Vector vy = getVector(y); 
		Vector vz = getVector(z); 
		
		if (vx == null || vy == null || vz == null)
			return null;
		
		Vector vyx = vy.copy().add(-1, vx);
//		ArrayList<Double> distanceVector = WordVector.subtract(vy, vx);
		Vector distanceVector = vyx.add(vz); // WordVector.add(distanceVector,vz);
		return getClosestWords(distanceVector,K);
	}

	public boolean isTopicTag(String topic) {
		return topicTagSet.contains(topic);
	}
	
	public Vector getVector(String word){
		return glove.getVector(word);
	}

	public String getRandomTopic(){
		return Utils.getRandomMember(glove.getWords());
//		List<String> keysAsArray = new ArrayList<String>(wordVector.keySet());
//		Random r = new Random();
//
//		return keysAsArray.get(r.nextInt(keysAsArray.size()));
	}
	
	public String[] getClosestTopic(String text){
		String words[] = text.split(" ");
		String res[] = new String[20];
		ArrayList<StringDouble> candidates = new ArrayList<StringDouble>();
		
		
		for (String key : wordDatabase.keySet()) {
			double dist = 0.0;
			double n = 0.0;
			for (int i=0;i<words.length;i++){
				double tmp = getDistance(key,words[i]);
				if (tmp < 9999){
					dist += tmp*tmp;
					n++;
					}
			}
			dist /= n;
			if (n > 0)
				candidates.add(new StringDouble(key, -dist));
		}
		Collections.sort(candidates);
		for (int i=0;i<20;i++)
			res[i] = candidates.get(i).s;
		
		return res;
	}

	public String getClosestExcludedTopic(String text) {
		String words[] = text.split(" ");
		String res = "";
		double best = 0;
		for (String key : wordDatabase.keySet()) {
			double dist = 0.0;
			double n = 0.0;
			for (int i=0;i<words.length;i++){
				double tmp = getDistance(key,words[i]);
				if (tmp < 9999){
					dist += tmp*tmp;
					n++;
					}
				//excluded topic penalty
				if (key.equalsIgnoreCase(words[i]))
					dist -= 99999999;
			}
			dist /= n;
			if (best < dist) {
				best = dist;
				res = key;
			}
		}
		return res;
	}

	public synchronized static LanguageModel get() {
		if (dflt!=null) return dflt;
		Log.d("haiku", "Preparing LanguageModel... It may take a minute");
		LanguageModel languageModel = new LanguageModel();
		languageModel.loadDictionary("en");
		languageModel.loadForbiddenDictionary("names__f.csv");
		languageModel.loadForbiddenDictionary("names__m.csv");
		//languageModel.loadStopWords("names__f.csv");
		//languageModel.loadStopWords("names__m.csv");
		languageModel.loadStopWords("stop-words_english_1_en.txt");
		languageModel.loadStopWords("stop-words_english_2_en.txt");
		languageModel.loadStopWords("stop-words_english_3_en.txt");
		languageModel.loadStopWords("stop-words_english_4_google_en.txt");
		languageModel.loadStopWords("stop-words_english_5_en.txt");
		languageModel.loadStopWords("stop-words_english_6_en.txt");
				
		//loading word dictionary
		try {
			languageModel.loadSyllableDictionary("cmudict");
		} catch (IOException e) {
			throw Utils.runtime(e);
		}

		//languageModel.trainMarkov(CorpusReader.readWikipedia("englishText_10000_20000"));
		brownOpen(languageModel,44,"ca");
		brownOpen(languageModel,75,"cg");
		brownOpen(languageModel,80,"cj");
		brownOpen(languageModel,24,"ch");
		brownOpen(languageModel,20,"ck");
		brownOpen(languageModel,9,"cr");
				
		dflt = languageModel;
		Log.d("haiku", "...prepared LanguageModel");
		return dflt;
	}
	

	private static void brownOpen(LanguageModel languageModel,int N,String code) {
		ArrayList<ArrayList<String>> data;
		
		//adding corpus 
		for (int i=1;i<N;i++) {
			//System.out.println("Loading corpus: );
			if (i < 10)
				data = CorpusReader.readBrown(code+"0"+i);
			else
				data = CorpusReader.readBrown(code+i);
		
			languageModel.trainMarkov(data);
		}		
	}
}
