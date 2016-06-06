package org.coinvent.haiku;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.ArrayMap;

public class Poem implements Comparable<Poem> {

	String topics;
	Line[] lines;
	public double score = -1;
	ArrayMap scoreInfo;
	
	
	/**
	 * 
	 * @param lineLengthSyllables Number of syllables in each line.
	 */
	public Poem(int[] lineLengthSyllables) {
		this.lines = new Line[lineLengthSyllables.length];
		for (int i = 0; i < lineLengthSyllables.length; i++) {
			lines[i] = new Line(lineLengthSyllables[i]);
		}
	}


	@Override
	public String toString() {
		return StrUtils.join(lines, "\n");
	}


	@Override
	public int compareTo(Poem arg0) {
		// highest score first
		return - Double.compare(score, arg0.score);
	}


	public void setTopics(String topic) {
		this.topics = topic;
	}
}
