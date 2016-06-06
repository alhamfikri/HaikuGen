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
import winterwell.maths.stats.distributions.cond.WWModel;
import winterwell.maths.stats.distributions.cond.WWModelFactory;
import winterwell.nlp.analysis.SyllableCounter;
import winterwell.nlp.corpus.IDocument;
import winterwell.nlp.corpus.brown.BrownCorpus;
import winterwell.nlp.corpus.brown.BrownDocument;
import winterwell.nlp.io.ApplyFnToTokenStream;
import winterwell.nlp.io.ITokenStream;
import winterwell.nlp.io.SitnStream;
import winterwell.nlp.io.Tkn;
import winterwell.nlp.io.WordAndPunctuationTokeniser;
import winterwell.nlp.io.pos.PosTagByOpenNLP;
import winterwell.nlp.vectornlp.GloveWordVectors;
import winterwell.utils.IFn;
import winterwell.utils.Mutable.Ref;
import winterwell.utils.Utils;
import winterwell.utils.reporting.Log;
import winterwell.utils.reporting.Log.KErrorPolicy;

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
		
	ITokenStream tweetTokeniser = new WordAndPunctuationTokeniser()
										.setLowerCase(true)
										.setNormaliseToAscii(KErrorPolicy.ACCEPT)
										.setUrlsAsWords(true)
										.setSwallowPunctuation(true);
	
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
				unusedWords.add(input);
			}
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
	
		
	final PoemVocab allVocab = new PoemVocab();
	private WWModel<Tkn> allWordModel;

	
	public synchronized WWModel<Tkn> getAllWordModel() {
		if (allWordModel!=null) return allWordModel;
		Desc<Ref> desc = new Desc<>("allWordModel", Ref.class).setTag("poem");
		desc.put("sig", sig);
		desc.put("training", "brown");
		desc.put("punc", false); // no punctuation
		final VocabFromTwitterProfile vftp = new VocabFromTwitterProfile(null, null);
		// also stash the internal model settings, e.g. TPW
		Desc<WWModel> wmdesc = vftp.getWordModel().getDesc();
		desc.addDependency("model", wmdesc);
		Ref<WWModel> modelref = Depot.getDefault().get(desc);
		if (modelref!=null && modelref.value!=null) {
			allWordModel = modelref.value;
			return allWordModel;
		}
		Log.d("lang", "WordModel: Processing Brown Corpus...");
		BrownCorpus bc = new BrownCorpus();		
		final AtomicInteger cnt = new AtomicInteger();
//		SafeExecutor exec = new SafeExecutor(Executors.newFixedThreadPool(8));		
		for (final IDocument doc : bc) {
			BrownDocument bdoc = (BrownDocument) doc;
			bdoc.setSwallowPunctuation(true);
//			exec.submit(new Callable<Object>(){
//				@Override
//				public Object call() throws Exception {
					vftp.train(bdoc);
					int c = cnt.incrementAndGet();
					if (c % 10 == 0) Log.d("lang", "WordModel: Processing Brown Corpus: docs:"+cnt+" "+(c/5)+"%\twords:"+vftp.getWordModel().getTrainingCnt() +"...");
//					return null;
//				}				
//			});			
		}
//		exec.shutdown();
//		exec.awaitTermination();
		
		allWordModel = vftp.getWordModel();
		Depot.getDefault().put(desc, new Ref(allWordModel));
		Log.d("lang", "WordModel: ...Processed Brown Corpus.");
		return allWordModel;
	}

	public static String[] sig = new String[]{"w-3", "w-2", "w-1"};
		
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
		return Utils.getRandomMember(glove.getWords());
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

		dflt = languageModel;
		Log.d("haiku", "...prepared LanguageModel");
		return dflt;
	}

	static WWModel<Tkn> newWordModel() {
		List<String> sig = Arrays.asList(LanguageModel.sig);
		WWModelFactory wwmf = new WWModelFactory();
		IFn<List<String>, int[]> trackedFormula = wwmf.trackedFormula(1000, 2, 100, 2);
		WWModel<Tkn> wordModel = wwmf.fullFromSig(sig, null, 
				trackedFormula, 
				1, 100, new HashMap()
				);
		return wordModel;
	}

	public PoemVocab getAllVocab() {
		return allVocab;
	}

	public SitnStream getSitnStream(Line line) {
		ITokenStream zeroPunctuation = new ApplyFnToTokenStream(line, new IFn<Tkn, Tkn>() {			
			@Override
			public Tkn apply(Tkn value) {
				if (Tkn.UNKNOWN.equals(value.getText())) return value;
				if (StrUtils.AZ.matcher(value.getText()).find()) {
					return value;
				}
				return null;
			}
		});
		SitnStream ss = new SitnStream(null, zeroPunctuation, sig);
		return ss;
	}

}
