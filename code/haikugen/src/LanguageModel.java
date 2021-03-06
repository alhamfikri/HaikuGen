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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

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
	
	//set of unique words
	private HashMap<String,WordInfo> wordDatabase;

	//list of words, divided by its syllables and POS tag
	private HashMap<String,HashSet<String>> wordlist[];
	//word vector
	private HashMap<String,ArrayList<Double>> wordVector;
	
	final String[] topicTags = {"JJ", "JJR", "JJS", "NN", "NNS" , "NNP" , "NNPS" , "RB", "RBS", "RBR", 
								"VB", "VBD" , "VBG", "VBN" , "VBP" , "VBZ" };
	private HashSet<String> topicTagSet;
	
	//global list of removed words
	private HashSet<String> unusedWords;
	
	//global list of used words
	private HashSet<String> dictionary;
	HashSet<String> stopWords;
	
	
	//markov chain model
	MarkovModel markov;
	
	@SuppressWarnings("unchecked")
	public LanguageModel() {
		markov = new MarkovModel();
		dictionary = new HashSet<String>();
		unusedWords = new HashSet<String>();
		stopWords = new HashSet<String>();
		
		wordDatabase = new HashMap<String,WordInfo>();
		wordVector = new HashMap<String,ArrayList<Double>>();
		wordDatabase.put("/s", new WordInfo("/s", 0, null) );
		
		wordlist = new HashMap[10];
		for (int i=0;i<10;i++)
			wordlist[i] = new HashMap<String,HashSet<String>>();
		
		topicTagSet = new HashSet<String>();
		for (int i=0;i<topicTags.length;i++)
			topicTagSet.add(topicTags[i]);
		
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
		HashSet<String> wordset = wordlist[0].get(tag);
    	if (wordset == null) {
    		wordset = new HashSet<String>();
    		wordlist[0].put(tag, wordset);
    	}
    	wordset.add(separator);
		
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
	
	public void loadVectorModel(String filepath) {
		String currentDirectory = System.getProperty("user.dir");
		String line = null;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(currentDirectory + "/res/wordvector/" + filepath));
			String input;
			while ((input = br.readLine()) != null) {
				String[] data = input.split(" ");
				String word = data[0];
				
				if (wordDatabase.get(word) == null || stopWords.contains(word))
					continue;
				
				ArrayList<Double> vector = new ArrayList<Double>();
				for (int i=1;i<data.length;i++) {
					vector.add(Double.valueOf(data[i]));
				}
				
				wordVector.put(word,vector);
				
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
	}
	/**
	 * Open CMUDictionary dictionary for initial word base
	 * @param filepath location of CMUDict
	 */
	public void loadSyllableDictionary(String filepath) {
		String currentDirectory = System.getProperty("user.dir");
		String line = null;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(currentDirectory + "/res/model/" + filepath));
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
				
				if (word.matches("[a-zA-Z]+") == false)
					continue;
				
				//if unused
				if (unusedWords.contains(word))
					continue;
				
				if (!dictionary.contains(word))
					continue;
				
				WordInfo w = new WordInfo(word,syllables,null);
				wordDatabase.put(word, w);
				
				ArrayList<String> tags = HaikuPOSTagger.possibleTags(word, 1);
				
				//updates wordlist by syllables
	            for (int i=0;i<tags.size();i++) {
	            	HashSet<String> wordset = wordlist[w.syllables].get(tags.get(i));
	            	if (wordset == null) {
	            		wordset = new HashSet<String>();
	            		wordlist[w.syllables].put(tags.get(i), wordset);
	            	}
	            	wordset.add(word);

	            }
			}
			
			br.close();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void trainMarkov(ArrayList<ArrayList<String>> data) {
		int N = data.size();
		for (int i=0;i<N;i++){
			int M = data.get(i).size();
			String prev = "/s";
			for (int j=0;j<M;j++) {
				if (data.get(i).get(j).equals("/s"))
					data.get(i).set(j, "/s");
				if (wordDatabase.get(data.get(i).get(j)) != null && wordDatabase.get(prev) != null)
					markov.add(prev,data.get(i).get(j));
				prev = data.get(i).get(j);
				
			}
			if (wordDatabase.get(prev) != null)
				markov.add(prev,"/s");
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
	
	/**
	 * 
	 * @param tag : Penn Treebank Tag
	 * @param syllables : number or syllables
	 * @return Array of String, a list of all possible words with given tag and syllables
	 */
	public String[] getWordlist(String tag, int syllables) {
		HashSet<String> wordSet = wordlist[syllables].get(tag);
		if (wordSet == null) 
			return null;
		
		return wordSet.toArray(new String[wordSet.size()]);	
	}
		
	/**
	 * 
	 * @param word : string of word
	 * @return WordInfo of given word
	 */
	public WordInfo getWordInfo(String word) {
		return wordDatabase.get(word);
	}

	/**
	 * Given a tag, return all possible number of syllables available within the tag
	 * @param tag
	 * @return sequence of int
	 */
	public ArrayList<Integer> getPossibleSyllables(String tag) {
		ArrayList<Integer> res = new ArrayList<Integer>();
		if (tag.length() < 2){
			res.add(0);
			return res;
		}			
		for (int i=0;i<10;i++){
			if (wordlist[i].get(tag) != null) 
				res.add(i);
		}
		
		return res;
	}

	/**
	 * get a syllable for count a given word
	 * @param word
	 * @return int: the number of sylable
	 */
	public int getSyllable(String word){
		WordInfo w = wordDatabase.get(word);
		if (w == null) return 0;
		return w.syllables;
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
		return markov.getProbability(prev,next);
	}

	public int getUnigramCount(String string) {
		
		return markov.getCount(string);
	}
	
	public double getDistance(String s1, String s2) {
		ArrayList<Double> v1 = wordVector.get(s1);
		ArrayList<Double> v2 = wordVector.get(s2); 
		
		if (v1 == null || v2 == null)
			return 9999;
		return -WordVector.cosineSimilarity(v1,v2);
		
		//return WordVector.dotProduct(v1,v2);
		//return WordVector.euclidian(v1,v2);
	}


	
	public double getDistance(ArrayList<Double> v1, String s2) {
		ArrayList<Double> v2 = wordVector.get(s2); 
		
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
		ArrayList<Double> topicVector = new ArrayList<Double>();
		for (int i=0;i<300;i++)
			topicVector.add(.0);
		
		String[] words = word.split(" ");
		for (int i=0;i<words.length;i++) {
			ArrayList<Double> v = getVector(words[i]);
			if (v != null)
				topicVector = WordVector.add(topicVector, v);	
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
	
	public String[] getClosestWords(ArrayList<Double> v, int K) {
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
		ArrayList<Double> vx = wordVector.get(x);
		ArrayList<Double> vy = wordVector.get(y); 
		ArrayList<Double> vz = wordVector.get(z); 
		
		if (vx == null || vy == null || vz == null)
			return null;
		
		ArrayList<Double> distanceVector = WordVector.subtract(vy, vx);
		distanceVector = WordVector.add(distanceVector,vz);
		return getClosestWords(distanceVector,K);
	}

	public boolean isTopicTag(String topic) {
		return topicTagSet.contains(topic);
	}
	
	public ArrayList<Double> getVector(String word){
		return wordVector.get(word);
	}

	public String getRandomTopic(){
		List<String> keysAsArray = new ArrayList<String>(wordVector.keySet());
		Random r = new Random();

		return keysAsArray.get(r.nextInt(keysAsArray.size()));
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
}
