package org.coinvent.haiku;


public class WordInfo {
	
	public static final WordInfo UNKNOWN = new WordInfo("?", -1, null);
	public String word;
	public int syllables;
	public int[] tone;
	boolean fixed;
	boolean punctuation;
	public String pos;
	
	public WordInfo(String word, int syllables, int[] tone) {		
		this.tone = tone;
		setWord(word);
		this.syllables = syllables;
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
}
