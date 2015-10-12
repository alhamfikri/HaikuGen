package org.coinvent.haiku;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.util.Sequence;

/**
 * Static POS Tagger class
 */
public class HaikuPOSTagger {
	
	private static POSTaggerME tagger = null;
	
	/*
	 * One time call only. Automatically called when using POSTagger for the first time.
	 */
	private static void init() {
		InputStream modelIn = null;
		String currentDirectory = System.getProperty("user.dir");
		
		try {
		  modelIn = new FileInputStream(currentDirectory + "/res/model/en-pos-maxent.bin");
		  POSModel model = new POSModel(modelIn);
		  tagger = new POSTaggerME(model);

		}
		catch (IOException e) {
		  // Model loading failed, handle the error
		  e.printStackTrace();
		}
		finally {
		  if (modelIn != null) {
		    try {
		      modelIn.close();
		    }
		    catch (IOException e) {
		    }
		  }
		}
	}
	
	/**
	 * Tags given string
	 * @param sentence : String of English that wants to be tagged
	 * @return array of string, the tag label of the input for each word
	 */
	public static String[] tag(String sentence) {
		
		//first time initialization
		if (tagger == null) {
			init();
		}
		
		String sent[] = sentence.split(" ");		  
		String tags[] = tagger.tag(sent);
		
		return tags;
	}
	
	/**
	 * Tags given string
	 * @param sentence : Array of string of English words that wants to be tagged
	 * @return array of string, the tag label of the input for each word
	 */
	public static String[] tag(String[] sentence) {
		
		//first time initialization
		if (tagger == null) {
			init();
		}
		
		String tags[] = tagger.tag(sentence);
		
		return tags;
	}

	/**
	 * Tags given string
	 * @param sentence : ArrayList of string of English words that wants to be tagged
	 * @return array of string, the tag label of the input for each word
	 */
	public static String[] tag(ArrayList<String> sentence) {

		//first time initialization
		if (tagger == null) {
			init();
		}
		
		String[] sentenceArr = new String[sentence.size()];
		sentenceArr = sentence.toArray(sentenceArr);
		
		String tags[] = tagger.tag(sentenceArr);
		
		return tags;
	}
	
	/**
	 * Return all possible tags of a given word. A given tag T[i] is possible, if its probability P(T[i]) is not less than R * P(T[i-1])
	 * @param words
	 * @param R
	 * @return
	 */
	public static ArrayList<String> possibleTags(String w, double R) {
		ArrayList<String> word = new ArrayList<String>();
		word.add(w);
		
		Sequence topSequences[] = tagger.topKSequences(word);
		ArrayList<String> res = new ArrayList<String>();
		
		int N = topSequences.length;
		List<String> tags = topSequences[0].getOutcomes();
		res.add(tags.get(0));
		double prob = topSequences[0].getProbs()[0];
		for (int i=1;i<N;i++){
			double probNew = topSequences[i].getProbs()[0];
			if (probNew < prob * R)
				return res;
			prob = probNew;
			res.add(topSequences[i].getOutcomes().get(0));
			
		}
		
		return res;
	}
}
