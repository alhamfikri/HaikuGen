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
import java.util.HashMap;
import java.util.HashSet;

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
	
	//global list of words, divided by its POS tag
	private HashMap<String,HashSet<String>> allWordlist;
	
	//markov chain model
	public MarkovModel markov;
	
	@SuppressWarnings("unchecked")
	public LanguageModel() {
		markov = new MarkovModel();
		wordDatabase = new HashMap<String,WordInfo>();
		allWordlist = new HashMap<String,HashSet<String>>();
		wordlist = new HashMap[10];
		for (int i=0;i<10;i++)
			wordlist[i] = new HashMap<String,HashSet<String>>();
	}
	
	/**
	 * Open CMUDictionary dictionary for initial word base
	 * @param filepath location of CMUDict
	 */
	public void loadDictionary(String filepath) {
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
				WordInfo w = new WordInfo(word,syllables,null);
				wordDatabase.put(word, w);
			}
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
				markov.add(prev,data.get(i).get(j));
				prev = data.get(i).get(j);
			}
			
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
            }
			
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
	 * @param tag : Penn Treebank Tag
	 * @param syllables : number or syllables
	 * @return Array of String, a list of all possible words with given tag 
	 */
	public String[] getWordlist(String tag) {
		HashSet<String> wordSet = allWordlist.get(tag);
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
		for (int i=1;i<10;i++){
			if (wordlist[i].get(tag) != null) 
				res.add(i);
		}
		
		return res;
	}

	public int getMarkovCount(String string, String word2) {
		return markov.getCount(string,word2);
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
}
