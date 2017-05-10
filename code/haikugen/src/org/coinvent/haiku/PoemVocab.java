package org.coinvent.haiku;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.winterwell.maths.stats.distributions.discrete.IDiscreteDistribution;
import com.winterwell.nlp.corpus.IDocument;

/**
 * The vocab this poem can use, with info on the words. 
 * Does not hold any statistical info.
 * 
 * @author daniel
 *
 */
public class PoemVocab {

	int MAX_SYLLABLES = 0;
	
	/** list of words, divided by its syllables and POS tag TODO a clearer form */
	private Map<Integer, Map<String, Set<String>>> wordlistFromPOSFromSyllables = new HashMap();

	private Set<WordInfo> allwords = new HashSet();	
	
	/**
	 * 
	 * @param tag : Penn Treebank Tag
	 * @param syllables : number or syllables
	 * @return a list of all possible words with given tag and syllables.
	 * Can be empty, never null.
	 */
	public Set<String> getWordlist(String tag, int syllables) {
		assert syllables > 0;
		Map<String, Set<String>> rightLength = wordlistFromPOSFromSyllables.get(syllables);
		if (rightLength==null) return Collections.EMPTY_SET;
		if (tag==null) {
			// bummer -- join
			HashSet join = new HashSet();
			for(Set<String> words : rightLength.values()) {
				join.addAll(words);
			}
			return join;
		}
		Set<String> list = rightLength.get(tag);
		if (list==null) return Collections.EMPTY_SET;
		return list;
	}
	

	public synchronized void addWord(WordInfo wi) {				
		assert wi.syllables() >= 0 : wi;
		assert wi.pos != null : wi;
		assert wi.word != null : wi;
		allwords.add(wi);
		Map<String, Set<String>> rightLength = wordlistFromPOSFromSyllables.get(wi.syllables());
		if (rightLength==null) {
			rightLength = new HashMap();
			wordlistFromPOSFromSyllables.put(wi.syllables(), rightLength);
		}
		Set<String> list = rightLength.get(wi.pos);
		if (list==null) {
			list = new HashSet();
			rightLength.put(wi.pos, list);			
		}
		list.add(wi.word);
		MAX_SYLLABLES = Math.max(wi.syllables(), MAX_SYLLABLES);
	}


	public Set<WordInfo> getAllWords() {
		return allwords;
	}

}
