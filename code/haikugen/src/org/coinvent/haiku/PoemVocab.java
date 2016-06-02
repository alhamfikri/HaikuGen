package org.coinvent.haiku;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
	
	/**
	 * 
	 * @param tag : Penn Treebank Tag
	 * @param syllables : number or syllables
	 * @return Array of String, a list of all possible words with given tag and syllables
	 */
	public Set<String> getWordlist(String tag, int syllables) {
		assert syllables > 0;
		Map<String, Set<String>> rightLength = wordlistFromPOSFromSyllables.get(syllables);
		if (rightLength==null) return null;
		if (tag==null) {
			// bummer -- join
			HashSet join = new HashSet();
			for(Set<String> words : rightLength.values()) {
				join.addAll(words);
			}
			return join;
		}
		Set<String> list = rightLength.get(tag);
		if (list==null) return null;
		return list;
	}
	

	public void addWord(WordInfo wi) {		
		assert wi.syllables >= 0 : wi;
		assert wi.pos != null : wi;
		assert wi.word != null : wi;
		Map<String, Set<String>> rightLength = wordlistFromPOSFromSyllables.get(wi.syllables);
		if (rightLength==null) {
			rightLength = new HashMap();
			wordlistFromPOSFromSyllables.put(wi.syllables, rightLength);
		}
		Set<String> list = rightLength.get(wi.pos);
		if (list==null) {
			list = new HashSet();
			rightLength.put(wi.pos, list);			
		}
		list.add(wi.word);
		MAX_SYLLABLES = Math.max(wi.syllables, MAX_SYLLABLES);
	}

}
