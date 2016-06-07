package org.coinvent.haiku;


public class Config {
	public final static int WIKI_SIZE = 10;
	public final static String[] keepWord = {"through","into","in","at","is","or","was","were","to","for","are","of","a","the","on","an"};
	/**
	 * Keep stopwords from the template haiku
	 */
	public final static boolean isKeep = true;
	/**
	 * Used to reduce the score for words picked from all-vocab
	 */
	public static final double DOWNVOTE_USEALLVOCAB = 0.01;
	public static int batchSize = 5;
}
