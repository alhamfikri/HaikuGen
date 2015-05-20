import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
public class Corpus {
	
	//set of unique words
	private HashMap<String,WordInfo> wordDatabase;

	//list of words, divided by its syllables and POS tag
	private HashMap<String,HashSet<String>> wordlist[];
	
	//global list of words, divided by its POS tag
	private HashMap<String,HashSet<String>> allWordlist;
	
	//markov chain model
	private MarkovModel markov;
	
	@SuppressWarnings("unchecked")
	public Corpus() {
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
				System.out.println(word+" "+syllables);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Open, add and process the given new data
	 * Automatically updates the corpus
	 * 
	 * @param filepath
	 */
	public void add(String filepath) {
		
		String currentDirectory = System.getProperty("user.dir");
		String line = null;
		try {
		  BufferedReader br = new BufferedReader(new FileReader(currentDirectory + "/res/brown/" + filepath));
	         while ((line = br.readLine()) != null) {
	            String tokens[] = line.trim().toLowerCase().split(" ");
	            if (tokens.length == 0) 
	            	continue;
	            
	            
	            //remove the tagset, create cleaned corpus and update the wordlist
	            for (int i=0;i<tokens.length;i++) {
	            	String word = tokens[i].split("/")[0];
	            	
	            	//if new word found, updates wordlist
	            	if ( wordDatabase.get(word) == null ) {
	            		//database.put(word, WORD.);
	            	}
	            	tokens[i] = word;
	            	
	            	//update the markov model
	            	if (i > 0 && word.length() > 0 && tokens[i-1].length() > 0){
	            		markov.add(word, tokens[i-1]);
	            	}
	            }

	            //re-tagging with Penn Treebank Tagset
	            String tags[] = HaikuPOSTagger.tag(tokens);
	            for (int i=0;i<tokens.length;i++){
	            	if (tokens[i].length() == 0)
	            		continue;
	            	
	            	WordInfo wordInfo = wordDatabase.get(tokens[i]);
	            	if (wordInfo == null) 
	            		continue;
	            	
	            	//updates wordlist by syllables
	            	HashSet<String> wordset = wordlist[wordInfo.syllables].get(tags[i]);
	            	if (wordset == null) {
	            		wordset = new HashSet<String>();
	            		wordlist[wordInfo.syllables].put(tags[i], wordset);
	            	}
	            	wordset.add(tokens[i]);
	            	
	            	//updates global wordlist 
	            	HashSet<String> wordset2 = allWordlist.get(tags[i]);
	            	if (wordset2 == null) {
	            		wordset2 = new HashSet<String>();
	            		allWordlist.put(tags[i], wordset2);
	            	}
	            	wordset2.add(tokens[i]);
	            }
	         }       
		}
		catch (IOException e) {
		  // Model loading failed, handle the error
		  e.printStackTrace();
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
}
