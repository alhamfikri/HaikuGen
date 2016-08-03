package org.coinvent.haiku;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.sun.org.apache.bcel.internal.generic.FNEG;
import com.winterwell.maths.stats.distributions.IDistributionBase;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;

import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Vector;
import winterwell.maths.stats.distributions.cond.Cntxt;
import winterwell.maths.stats.distributions.cond.ICondDistribution;
import winterwell.maths.stats.distributions.cond.Sitn;
import winterwell.maths.stats.distributions.cond.WWModel;
import winterwell.maths.stats.distributions.d1.MeanVar1D;
import winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import winterwell.maths.timeseries.DataUtils;
import winterwell.maths.vector.VectorUtilsTest;
import winterwell.nlp.NLPWorkshop;
import winterwell.nlp.dict.CMUDict;
import winterwell.nlp.docmodels.IDocModel;
import winterwell.nlp.io.ApplyFnToTokenStream;
import winterwell.nlp.io.FilteredTokenStream;
import winterwell.nlp.io.ITokenStream;
import winterwell.nlp.io.SitnStream;
import winterwell.nlp.io.Tkn;
import winterwell.utils.FailureException;
import winterwell.utils.IFn;
import winterwell.utils.Utils;
import winterwell.utils.web.XStreamUtils;

/**
 * What is and isn’t haiku: http://www.litkicks.com/EssentialElementsofHaiku
Lists of season marker words: https://en.wikipedia.org/wiki/Saijiki http://www.2hweb.net/haikai/renku/500ESWd.html

 * @author daniel
 *
 */
public class PoemGenerator {

	public void setDocModel(IDocModel docModel) {
		this.docModel = docModel;
	}
	
	
	public ICondDistribution<Tkn, Cntxt> getWordGen() {
		return wordGen;
	}
	
	private static final String LOGTAG = "haiku";
	private LanguageModel languageModel;
	private List<Haiku> haikus;
	private int syllableConstraint[];
//	private int backedoff;
	private Vector topicVector;
	private String topic2;
	private String topic1;
	private PoemVocab vocab;
	private List<int[]> rhymeConstraint;
	/**
	 * Constructor
	 * @param languageModel : set corpus for words and relational information
	 * @param haikus : set haikus for generating the grammatical skeleton
	 * @param syllables : syllables constraint, ex: [5,7,5]. Set 0 for no constraint
	 */
	public PoemGenerator(LanguageModel languageModel, List<Haiku> haikus, int syllableConstraint[]){
		Utils.check4null(languageModel, haikus, syllableConstraint);
		this.languageModel = languageModel;
		this.vocab = languageModel.allVocab;
		this.haikus = haikus;
		this.syllableConstraint = syllableConstraint;
		setWordGen(languageModel.getAllWordModel());
	}
	
	/**
	 * The lines (zero-indexed) which should rhyme.
	 * @param rhymes
	 * @return
	 */
	public PoemGenerator setRhymeConstraint(List<int[]> rhymes) {
		rhymeConstraint = rhymes;
		return this;
	}
	
	public void setVocab(PoemVocab vocab) {
		this.vocab = vocab;
	}
	
	Poem generate2() {
		Poem poem = new Poem(syllableConstraint);
		Random random = new Random();
				
		// Make a POS skeleton
		for (int i=0; i<poem.lines.length; i++) {
			final Line line = poem.lines[i];			
			// pick a haiku to provide the template for this line
			int id = random.nextInt(haikus.size());
			Haiku haikuTemplate = haikus.get(id);
			String[] postagsForLine = haikuTemplate.getTag()[i];
			line.words = new ArrayList();
			for(String pos : postagsForLine) {
				WordInfo wi = new WordInfo();
				wi.pos = pos;
				line.words.add(wi);
				assert wi.syllables() == -1 : wi;
			}
			
			// fix the words that will not be removed			
			String[] linei = haikuTemplate.getWord()[i];
			for(int j=0; j<linei.length; j++){
				String word = linei[j];
				if (keepTemplateWord(word)) {
					WordInfo wi = line.words.get(j);
					wi.setWord(word);
					wi.fixed = true;
				}
			}			
			
			SyllableAssignment sa = new SyllableAssignment(line, vocab);
			int[] syllables = sa.randomizeSyllable();
			if(syllables == null) {
				Log.d(LOGTAG, "failed to fill in syllable template for "+StrUtils.str(line));
				return null;
			}			
			
			// sanity check: syllables set
			for(WordInfo wi : line.words) {
				assert wi.syllables() >= 0;
			}
		}
		
		// Fill in words
		generate3_fillInWords(poem, 1);
		generate3_fillInWords(poem, 0);
		
//		// sanity check: words filled in -- But rhymes can fail :(
//		for(Line line : poem.lines) {
//			for(WordInfo wi : line.words) {
//				assert ! Utils.isBlank(wi.word) : line;
//			}
//		}
		
		//post-processing the Haiku, fixes some inconsistencies
		postProcessing(poem);
		return poem;
	}
	
	void generate3_fillInWords(Poem poem, double randomness) {
		for(Line line : poem.lines) {
			for(WordInfo word : line.words) {
				if (word.fixed) {
					assert ! word.word.isEmpty();
					assert keepTemplateWord(word.word) : word;
					assert ! Utils.isBlank(word.word) : line;
					continue;
				}
				if (word.syllables()==0) {
					// punctuation -- should have been filled in from the punctuation
					assert ! Utils.isBlank(word.word) : line;
					continue;
				}
				Tkn picked = generateWord(word, line, poem, randomness);
				if (picked==null) {
					// Fail
					Log.e(LOGTAG, "Failed to pick a word for "+word+" in "+line);
					continue;
				}
				word.setWord(picked.getText());
				assert ! Utils.isBlank(picked.getText());
			}			
		} // end for-each-line
	}
	
	/**
	 * TODO score the text consistency (eg markov chain log-prob)
	 */
	IDocModel docModel;
	
	ICondDistribution<Tkn, Cntxt> wordGen;

	double topicScoreWeight = Config.get().weight_topic;
	double senseScoreWeight = Config.get().weight_sense;
	double focusScoreWeight = 1;
	
	public void setWordGen(ICondDistribution<Tkn, Cntxt> wordGen) {
		this.wordGen = wordGen;
	}
	
	/**
	 * 
	 * @param wordInfo Will not be modified
	 * @param line
	 * @return
	 */
	Tkn generateWord(WordInfo wordInfo, Line line, Poem poem, double randomness) {
		assert ! wordInfo.fixed && wordInfo.syllables() > 0 : wordInfo;		
		assert wordInfo!=null;
		// context
		String posTag = wordInfo.pos;
		assert posTag != null;		
		SitnStream ss = languageModel.getSitnStream(line);
		List<Sitn<Tkn>> list = Containers.getList(ss);
		assert list.size() <= line.words.size() : list+" vs "+line.words;
		// work out which element of list we want (sorry - this is ugly due to dropped punctuation)
		int inst=0;
		for(WordInfo wi : line.words) {
			if (Utils.equals(wordInfo.word, wi.word)) {
				inst++;
			}
			if (wordInfo == wi) {
				break;
			}			
		}
		int matchInst = 0;
		Cntxt context = null;
		for(Sitn<Tkn> si : list) {
			if (Utils.equals(wordInfo.word, si.outcome.getText()) 
					|| (wordInfo.word==null && si.outcome.getText().equals(Tkn.UNKNOWN))) {				
				matchInst++;
				if (matchInst==inst) {
					context = si.context;
					break;
				}
			}
		}
		assert context != null : wordInfo+" "+inst+" not in "+list;
		
		// filter by rhyme? (allow occasional rhyme-breaking)
		String rhyme = null;
		if (isLastWord(wordInfo, line) && Utils.getRandomChoice(1 - Config.P_IGNORE_RHYME)) {
			if (rhymeConstraint!=null) {
				for(int[] rhymes : rhymeConstraint) {
					for (int i = 0; i < rhymes.length; i++) {
						if (rhymes[i] != line.num) continue;
						int ri = rhymes[line.num==0? 1 : 0];
						Line lineri = poem.lines[ri];
						WordInfo endWord = getLastWord(line);
						if (endWord==null || Utils.isBlank(endWord.word)) {
							continue;
						}					
						rhyme = endWord.word;
					}
				}
			}
		}

		
		// sample
		// ...score each option
		ObjectDistribution<Tkn> wordChoices = new ObjectDistribution<>();
		Set<String> wordList = vocab.getWordlist(wordInfo.pos, wordInfo.syllables());		
		for(String w : wordList) {
			if (LanguageModel.get().avoidWord(w)) {
				continue;
			}
			if (keepTemplateWord(w)) {
				continue; // stopwords like this are retained from the template
			}
			if (vocab==LanguageModel.get().getAllVocab()) {
				if ( ! LanguageModel.get().isWord(w)) {
					continue; // only real words from the wider vocab
				}
			}
			Tkn outcome = new Tkn(w);
			double s = scoreWord(outcome, context, wordInfo, line);
//			assert MathUtils.isProb(s) : s+" "+outcome;
			wordChoices.setProb(outcome, s);
		}
		// include the wider vocab?
		if (wordChoices.size() < 5 || rhyme!=null) {
			Set<String> wordList2 = LanguageModel.get().allVocab.getWordlist(wordInfo.pos, wordInfo.syllables());
			for(String w : wordList2) {
				if (LanguageModel.get().avoidWord(w)) {
					continue;
				}
				if (keepTemplateWord(w)) {
					continue; // stopwords like this are retained from the template
				}
				if ( ! LanguageModel.get().isWord(w)) {
					continue; // only real words from the wider vocab
				}
				Tkn outcome = new Tkn(w);
				WWModel<Tkn> wm = LanguageModel.get().getAllWordModel();
				double p = wm.prob(outcome, context);
				assert p > 0 : p+" "+outcome+" "+context;
				wordChoices.setProb(outcome, p*Config.DOWNVOTE_USEALLVOCAB);
			}	
		}
		if (wordChoices.isEmpty()) {
			Log.w("poem", "No word for "+wordInfo.pos+" "+wordInfo.syllables());
			return null;
		}
		// filter by rhyme? (allow occasional rhyme-breaking)
		if (rhyme!=null) {
			List<String> rhymeWords = new CMUDict().getRhymes(rhyme);
			ArrayList<Tkn> remove = new ArrayList();
			for(Tkn tkn : wordChoices) {
				if ( ! rhymeWords.contains(tkn.getText())) {
					remove.add(tkn);
				}
			}
			wordChoices.removeAll(remove);
			if (wordChoices.isEmpty()) {
				Log.w("poem", "No word for "+wordInfo.pos+" "+wordInfo.syllables()+" that rhymes with "+rhyme);
				return null;
			}
		}
		
		Tkn sampled = wordChoices.sample(); //wordGen.sample(context);
		Tkn best = wordChoices.getMostLikely();
		if (Utils.getRandomChoice(randomness)) {
			return sampled;
		}						
		return best;
	}

	private WordInfo getLastWord(Line line) {
		for(int i=line.words.size()-1; i>=0; i--) {
			WordInfo wi = line.words.get(i);
			if (wi.syllables()!=0 || StrUtils.isWord(wi.word)) {
				return wi;
			}
		}
		return null;
	}

	private boolean isLastWord(WordInfo wordInfo, Line line) {
		return wordInfo == getLastWord(line);
	}

	/**
	 * Weighted score: word-model flow and topic-match
	 * @param outcome
	 * @param context
	 * @param wordInfo
	 * @param line
	 * @return
	 */
	private double scoreWord(Tkn outcome, Cntxt context, WordInfo wordInfo, Line line) {
		assert wordGen != null : this;
		double p = wordGen.prob(outcome, context);
		if (topicVector==null) {
			return p;
		}
		Vector vec = LanguageModel.glove.getVector(outcome.getText());
		double ts = vec==null? 0.0001 : Math.abs(topicVector.dot(vec));
		double s = p*Config.get().weight_sense + ts*Config.get().weight_topic;
		return s;
	}

	private boolean keepTemplateWord(String word) {
		// punctuation?
		if (StrUtils.isPunctuation(word)) return true;
		if ( ! Config.isKeep) {
			return false;
		}
		if (languageModel.stopWords.contains(word)) return true;
		return false;
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
			// fix i im ive id
			for (int j=0; j<line.words.size(); j++) {
				WordInfo word = line.words.get(j);
				if ("i".equals(word.word)) {
					word.setWord("I");
				} else if ("ive".equals(word.word)) {
					word.setWord("I've");
				} else if ("im".equals(word.word)) {
					word.setWord("I'm");
				} else if ("id".equals(word.word)) {
					word.setWord("I'd");
				}
			}
			// not a word? hashtag it
			for (int j=1; j<line.words.size(); j++) {
				WordInfo word = line.words.get(j);
				if ( ! LanguageModel.get().isWord(word.word) && StrUtils.isWord(word.word)) {
					word.setWord("#"+word.word);
				}
			}
		}
	}
	

	

	/**
	 * 
	 * @param topic1
	 * @param topic2 Can be null
	 * @return
	 */
	public Poem generate(String topic1, String topic2) {
		setTopic(topic1, topic2);

		List candidates = new ArrayList();
		for(int i=0; i<Config.batchSize; i++) {
			try {
				Poem res = generate2();
				if (res==null) {
					Log.d(LOGTAG, "generate "+i+" failed :(");
					continue;
				}
				res.score = score(res);			
				assert res.score != -1;
				candidates.add(res);
			} catch(Exception ex) {
				Log.e(LOGTAG, ex);
				continue;
			}
		}
		if (candidates.isEmpty()) {
			throw new FailureException("no poem found :(");
		}
		Collections.sort(candidates);
		Poem winner = (Poem) candidates.get(0);
		return winner;
	}

	private void setTopic(String topic1, String topic2) {
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
	}

	private double score(Poem res) {
		assert res.lines != null;
		// TODO
		// topic: overall topic-vector score
		double topicScore = 0;
		Vector poemTopic = null;
		for(Line line : res.lines) {
			assert line!=null;
			for(WordInfo wi : line.words) {
				Vector vector = LanguageModel.get().getVector(wi.word);
				if (vector == null) continue;
				if (poemTopic==null) poemTopic = vector;
				else poemTopic = poemTopic.add(vector);
			}
		}
		DataUtils.normalise(poemTopic);
		if (poemTopic!=null && topicVector!=null) {
			DataUtils.normalise(topicVector);
			topicScore = Math.abs(poemTopic.dot(topicVector));
		}
		
		// aesthetics: similar-words without repetition
		int repeats=0;
		HashSet<String> seen = new HashSet();
		Set<String> stopwords = NLPWorkshop.get().getStopwords();
		double focusScore = 0;
		int focaln = 0;
		for(Line line : res.lines) {
			for(WordInfo wi : line.words) {
				if (StrUtils.isWord(wi.word) && ! stopwords.contains(wi.word)) {
					if (seen.contains(wi.word)) {
						repeats ++;
					}
					seen.add(wi.word);
				}
				Vector vector = LanguageModel.glove.getVector(wi.word);
				if (vector == null) continue;
				DataUtils.normalise(vector);
				focusScore += Math.abs(poemTopic.dot(vector));
				focaln++;
			}
		}
		focusScore = focusScore / focaln;
		if (repeats!=0) {
			focusScore = focusScore / (20*repeats);
		}
		
		// TODO rhyme!
		
		// sense: multiply per-word scores
		double senseScore = 1;
		MeanVar1D mv = new MeanVar1D();
		for(Line line : res.lines) {			
			WWModel<Tkn> wm = languageModel.getAllWordModel();
			SitnStream ss = languageModel.getSitnStream(line);			
			for (Sitn<Tkn> sitn : ss) {
				double p = wm.prob(sitn.outcome, sitn.context);
				mv.train1(p);
			}			
		}	
		senseScore = mv.getMean()*1000; // these numbers tend to be very low
		// weight them (add a bit in to avoid 0s)
		double ts = 0.0000000001 + topicScore*topicScoreWeight;
		double ss = 0.0000000001 + senseScore*senseScoreWeight;
		double fs = 0.0000000001 + focusScore*focusScoreWeight;
		// balance them, F-Score style
		double score = 3*ts*ss*fs / (ts+ss+fs);
		// store all 
		res.scoreInfo = new ArrayMap("score", score, 
									"topicScore", ts,
									 "senseScore", ss,
									 "focusScore", fs);
		return score;
	}

	public Poem generate(String topic) {
		return generate(topic, null);
	}
	
	
	
}

