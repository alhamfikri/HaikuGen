package org.coinvent.haiku;

import winterwell.nlp.io.Tkn;


public class WordInfo {
	
	public static final WordInfo UNKNOWN = new WordInfo("?", -1);
	public String word;
	public int syllables = -1;
	boolean fixed;
	boolean punctuation;
	public String pos;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((pos == null) ? 0 : pos.hashCode());
		result = prime * result + ((word == null) ? 0 : word.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WordInfo other = (WordInfo) obj;
		if (pos == null) {
			if (other.pos != null)
				return false;
		} else if (!pos.equals(other.pos))
			return false;
		if (word == null) {
			if (other.word != null)
				return false;
		} else if (!word.equals(other.word))
			return false;
		return true;
	}

	public WordInfo(String word, int syllables) {		
		this.word = word;
		this.syllables = syllables;		
	}
	
	public WordInfo setFixed(boolean fixed) {
		this.fixed = fixed;
		return this;
	}
	
	public WordInfo() {
	}

	@Override
	public String toString() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;		
		this.syllables = word==null? -1 : LanguageModel.get().getSyllable(word);
	}

	public WordInfo setPOS(String posTag) {
		this.pos = posTag;
		return this;
	}

	public Tkn getTkn() {		
		Tkn tkn = new Tkn(word==null? Tkn.UNKNOWN : word);
		tkn.put(tkn.POS, pos);
		return tkn;
	}
}
