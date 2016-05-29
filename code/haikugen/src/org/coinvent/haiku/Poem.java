package org.coinvent.haiku;

import com.winterwell.utils.StrUtils;

public class Poem implements Comparable<Poem> {

	Line[] lines;
	public double score = -1;
	
	
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
}