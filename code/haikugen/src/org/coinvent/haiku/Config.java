package org.coinvent.haiku;

import com.winterwell.utils.Printer;

import winterwell.utils.containers.Containers;


public class Config {
	
	@Override
	public String toString() {
		return Printer.toString(Containers.objectAsMap(this));
	}
	
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
	public static final double P_IGNORE_RHYME = 0.05;
	public static int batchSize = 20;
	
	public double weight_topic = 0.1;
	public double weight_sense = 1;
	
	private static Config config = new Config();
	
	public static Config get() {
		return config;
	}
}
