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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.org.apache.bcel.internal.generic.FNEG;
import com.winterwell.depot.Depot;
import com.winterwell.depot.Desc;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.threads.SafeExecutor;

import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Vector;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import com.winterwell.maths.stats.distributions.cond.WWModel;
import com.winterwell.maths.stats.distributions.cond.WWModelFactory;
import com.winterwell.nlp.NLPWorkshop;
import com.winterwell.nlp.PorterStemmer;
import com.winterwell.nlp.analysis.SyllableCounter;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.nlp.corpus.brown.BrownCorpus;
import com.winterwell.nlp.corpus.brown.BrownCorpusTags;
import com.winterwell.nlp.corpus.brown.BrownDocument;
import com.winterwell.nlp.dict.CMUDict;
import com.winterwell.nlp.io.ApplyFnToTokenStream;
import com.winterwell.nlp.io.ITokenStream;
import com.winterwell.nlp.io.ListTokenStream;
import com.winterwell.nlp.io.SitnStream;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.nlp.io.WordAndPunctuationTokeniser;
import com.winterwell.nlp.io.pos.PosTagByOpenNLP;
import com.winterwell.nlp.vectornlp.GloveWordVectors;
import com.winterwell.utils.IFn;
import com.winterwell.utils.Mutable.Ref;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.log.KErrorPolicy;

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
		
	ITokenStream tokeniser = makeTokeniser();
	
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
	
	
	private LanguageModel() {
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
	
	private static ITokenStream makeTokeniser() {
		WordAndPunctuationTokeniser words = new WordAndPunctuationTokeniser()
										.setLowerCase(true)
										.setNormaliseToAscii(KErrorPolicy.ACCEPT)
										.setUrlsAsWords(true)
										.setSwallowPunctuation(true);
		ITokenStream noUrls = new ApplyFnToTokenStream(words, new IFn<Tkn, Tkn>() {			
			@Override
			public Tkn apply(Tkn value) {
				if (value.getPOS() == WordAndPunctuationTokeniser.POS_URL) return null;
				return value;
			}
		});
		ITokenStream zeroPunctuation = new ApplyFnToTokenStream(noUrls, new IFn<Tkn, Tkn>() {			
			@Override
			public Tkn apply(Tkn value) {
				if (Tkn.UNKNOWN.equals(value.getText())) return value;
				if (StrUtils.AZ.matcher(value.getText()).find()) {
					return value;
				}
				return null;
			}
		});		
		return zeroPunctuation;
	}

	private void addSpecialSeparator(String separator, String tag) {
		allVocab.addWord(new WordInfo(separator, 0).setPOS(tag));
	}

	/**
	 * Open forbidden words dictionary
	 */
	public void loadForbiddenDictionary(String filepath) {
		String currentDirectory = System.getProperty("user.dir");		
		try {
			BufferedReader br = new BufferedReader(new FileReader(currentDirectory + "/res/name/" + filepath));
			String input;
			while ((input = br.readLine()) != null) {
				unusedWords.add(input.trim().toLowerCase());
			}
			br.close();
		} catch (Exception e) {
			throw Utils.runtime(e);
		}			
	}
	
	/**
	 * Open and load stop-words list
	 */
	public void loadStopWords(String filepath) {
		String currentDirectory = System.getProperty("user.dir");		
		try {
			BufferedReader br = new BufferedReader(new FileReader(currentDirectory + "/res/stop-words/" + filepath));
			String input;
			while ((input = br.readLine()) != null) {
				stopWords.add(input);
			}
		} catch (Exception e) {
			throw Utils.runtime(e);
		}	
	}
	
	public void loadDictionary(String filepath) {
		String currentDirectory = System.getProperty("user.dir");
		PorterStemmer stemmer = new PorterStemmer();		
		try {
			BufferedReader br = new BufferedReader(new FileReader(currentDirectory + "/res/model/" + filepath));
			String input;
			while ((input = br.readLine()) != null) {
				String w = input.trim().toLowerCase();
				dictionary.add(w);
				// also the stemmed form
				dictionary.add(stemmer.stem(w));
			}
			br.close();
		} catch (Exception e) {
			throw Utils.runtime(e);
		}	
		
	}
	
	
	/**
	 * Open CMUDictionary dictionary for initial word base
	 * @param filepath location of CMUDict
	 * @throws IOException 
	 */
	public void loadSyllableDictionary() {
		CMUDict cmud = new CMUDict();
		cmud.load();
		for(String word : cmud.getAllWords()) {
			word = word.trim();
			if ( ! StrUtils.isWord(word)) {
				continue;
			}			
			//if unused
			if (unusedWords.contains(word)) {
				continue;
			}
			// in the dictionary
			if ( ! isWord(word)) {
				continue; // most of these are junk
			}
			
			int syllables = cmud.getSyllableCount(word);
			WordInfo w = new WordInfo(word, syllables);
			wordDatabase.put(word, w);
			
			List<String> tags = getPossiblePOSTags(word);
			
			//updates wordlist by syllables
            for (String posTag  : tags) {
            	WordInfo wi = new WordInfo(word, syllables);
            	wi.setPOS(posTag);
            	allVocab.addWord(wi);
            }
		}
	}
	
		
	final PoemVocab allVocab = new PoemVocab();
	private WWModel<Tkn> allWordModel;

	
	public synchronized WWModel<Tkn> getAllWordModel() {
		if (allWordModel!=null) return allWordModel;
		final VocabFromTwitterProfile vftp = new VocabFromTwitterProfile(null, null);
		vftp.setTokeniser(getTokeniser());
		WWModel<Tkn> model = vftp.getWordModel();
		Desc<WWModel> desc = model.getDesc();
		desc.unset();
		desc.setTag("poem");
		desc.put("sig", sig);
		desc.put("train", "brown");
		desc.put("punc", false); // no punctuation		
		// also stash the internal model settings, e.g. TPW
		ArrayList allModels = new ArrayList();
		allModels.add(model);
		allModels.addAll(model.getAllSimplerModels());
		for(Object simple : allModels) {
			Desc<Object> sd = Desc.desc(simple);
			if (sd==null) continue;			
			sd.unset();
			sd.setTag("poem");					
		}		
		model = model.sync(Depot.getDefault());
		if (model.getTrainingCnt() > 1000) {
			allWordModel = model;
			return allWordModel;
		}
		Log.d("lang", "WordModel: Processing Brown Corpus...");
		BrownCorpus bc = new BrownCorpus();		
		final AtomicInteger cnt = new AtomicInteger();
		for (final IDocument doc : bc) {
			BrownDocument bdoc = (BrownDocument) doc;
			bdoc.setSwallowPunctuation(true);
			bdoc.setLowerCase(true);
			vftp.train(bdoc);
			int c = cnt.incrementAndGet();
			if (c % 10 == 0) Log.d("lang", "WordModel: Processing Brown Corpus: docs:"+cnt+" "+(c/5)+"%\twords:"+model.getTrainingCnt() +"...");
		}
		
		allWordModel = model;
		Depot.getDefault().put(desc, allWordModel);
		Log.d("lang", "WordModel: ...Processed Brown Corpus.");
		return allWordModel;
	}

	public static String[] sig = 
//			new String[]{"w-1"};
			new String[]{"w-3", "w-2", "w-1"};
		
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
		if (w != null && w.syllables() > 0) return w.syllables();
		return SyllableCounter.syllableCount(word);
	}
		
	public double getDistance(String s1, String s2) {		
		Vector v1 = glove.getVector(s1);
		Vector v2 = glove.getVector(s2); 
		
		if (v1 == null || v2 == null)
			return 9999;
		return -WordVector.cosineSimilarity(v1,v2);
	}


	
	public double getDistance(Vector v1, String s2) {
		Vector v2 = glove.getVector(s2); 
		
		if (v1 == null || v2 == null)
			return 9999;
		
		return -WordVector.cosineSimilarity(v1,v2);
		//return WordVector.dotProduct(v1,v2);
		//return WordVector.euclidian(v1,v2);
	}
	
	
	public boolean isTopicTag(String topic) {
		return topicTagSet.contains(topic);
	}
	
	public Vector getVector(String word){
		return glove.getVector(word);
	}

	public String getRandomTopic(){
		return Utils.getRandomMember(Arrays.asList("love","beauty","business","travel","the city","sunshine","sadness","happiness","madness"));
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
		languageModel.loadSyllableDictionary();		

		dflt = languageModel;
		Log.d("haiku", "...prepared LanguageModel");
		return dflt;
	}

	static WWModel<Tkn> newWordModel() {
		List<String> sig = Arrays.asList(LanguageModel.sig);
		WWModelFactory wwmf = new WWModelFactory();
		IFn<List<String>, int[]> trackedFormula = wwmf.trackedFormula(10000, 2, 100, 2);
		// use a low TPW value to encourage phrases
		double t2tpw = 1.0;
		WWModel<Tkn> wordModel = wwmf.fullFromSig(sig, null, 
				trackedFormula, 
				t2tpw, 100, new HashMap()
				);
		return wordModel;
	}

	public PoemVocab getAllVocab() {
		return allVocab;
	}

	public SitnStream getSitnStream(Line line) {		
		ITokenStream tokens = new ListTokenStream(line.toList());
		SitnStream ss = new SitnStream(null, tokens, sig);
		return ss;
	}

	public static List<String> getPossiblePOSTags(String text) {
		if ("rt".equalsIgnoreCase(text)) {
			return Arrays.asList("VB");
		}
		return PosTagByOpenNLP.getPossibleTags(text);
	}

	public boolean isWord(String word) {
		if (StrUtils.isPunctuation(word)) return false;
		word = word.toLowerCase();
		// special case removed 's
		if (Arrays.asList("im","id","ive","cant","wont","wouldnt","shouldnt","couldnt").contains(word)) {
			return true;
		}
		assert ! dictionary.isEmpty();
		boolean ok = dictionary.contains(word);
		if (ok) return true;
		// stemmed?
		String stemmed = new PorterStemmer().stem(word);
		if (dictionary.contains(stemmed)) {
			return true;
		}
		return false;
	}

	public boolean avoidWord(String w) {
		if (unusedWords.contains(w.toLowerCase())) {
			return true;
		}
		return false;
	}

	public static List<Tkn> posTag(ITokenStream linei) {
		PosTagByOpenNLP tagger = new PosTagByOpenNLP(linei);
		return Containers.getList(tagger);
	}

	public ITokenStream getTokeniser() {
		return tokeniser;
	}

}
