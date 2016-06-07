package org.coinvent.haiku;

import java.util.List;

import winterwell.nlp.io.DumbTokenStream;
import winterwell.nlp.io.ITokenStream;
import winterwell.nlp.io.Tkn;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.StrUtils;

/**
 * @author Alham Fikri Aji
 * 
 * Class to storage Haiku. Records the Haiku's original text, parsed text, and its POS-Tag.
 */
public class Haiku implements Comparable<Haiku>{
	
	private String originalText;
	private String[][] text;
	private String[][] tag;
	private int part;
	private String topic;
	final double topicScore;
	final double meaningScore;
	double beautyScore;
	private double totalScore;
	/**
	 * For loading Haiku from the corpus - construct the Haiku based on given text.
	 * 
	 * @param poem 	the haiku. Each line of haiku MUST be separated by & sign. 
	 * 				For example: "Lightning flash -- & what I thought were faces & are plumes of pampas grass ."
	 */
	public Haiku(String poem) {
		originalText = poem;
		String lines[] = originalText.split("&");
		part = lines.length;
		
		text = new String[part][];
		tag = new String[part][];
				
		for (int i=0;i<part;i++) {
			text[i] = lines[i].trim().split(" ");
			//removing extra white-spaces
			for (int j=0;j<text[i].length;j++) {
				text[i][j] = text[i][j].trim();
			}
			ITokenStream linei = new DumbTokenStream(text[i]);
			List<Tkn> tkns = LanguageModel.posTag(linei);			
			assert tkns.size() == text[i].length;
			tag[i] = new String[tkns.size()];
			for (int j = 0; j < tag[i].length; j++) {
				tag[i][j] = tkns.get(j).getPOS();
			}			
		}	
		// This is a corpus poem - just put any value in for the scores
		meaningScore=1;
		topicScore=1;
		totalScore = meaningScore + topicScore;
	}
	
	

	/**
	 * Display the haiku in (hopefully) beautiful format
	 */
	public void print(){
		System.out.println("TOPIC = "+topic);
		System.out.println(toString());
		//System.out.println("Haiku topic closeness score = "+topicScore);
		//System.out.println("Haiku meaningful score = "+meaningScore);		
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i=0;i<part;i++) {
			for (int j=0;j<text[i].length;j++) {
				sb.append(text[i][j]+" ");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	/**
	 * Get Haiku's tag
	 * @return array or string of tag divided per-part
	 */
	public String[][] getTag() {
		return tag;
	}

	@Override
	public int compareTo(Haiku o) {		
		if (o.getTotalScore() - this.getTotalScore() < 0) return -1;
		if (o.getTotalScore() - this.getTotalScore() > 0) return 1;
		return 0;
	}

	public double getTotalScore() {
		return totalScore;
	}

	public String[][] getWord() {
		return text;
	}

	public void setTopics(String string) {
		topic = string;
	}
	

}
