package org.coinvent.haiku;

import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;

import winterwell.utils.Utils;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.web.IHasJson;

public class Poem implements Comparable<Poem>, IHasJson {

	String id = Utils.getUID();
	
	String topics;
	Line[] lines;
	public double score = -1;
	ArrayMap scoreInfo;
	private Map recipient;
	
	
	/**
	 * 
	 * @param lineLengthSyllables Number of syllables in each line.
	 */
	public Poem(int[] lineLengthSyllables) {
		this.lines = new Line[lineLengthSyllables.length];
		for (int i = 0; i < lineLengthSyllables.length; i++) {
			lines[i] = new Line(lineLengthSyllables[i]);
			lines[i].num = i;
		}
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(Line line : lines) {
			for(WordInfo word : line.words) {
				// TODO no whitespace gap??
				if (".,".contains(word.word)) {
					
				}
				sb.append(word.word);
				sb.append(" ");
			}
			sb.append("\n");
		}
		return sb.toString();
	}


	@Override
	public int compareTo(Poem arg0) {
		// highest score first
		return - Double.compare(score, arg0.score);
	}


	public void setTopics(String topic) {
		this.topics = topic;
	}


	@Override
	public void appendJson(StringBuilder sb) {
		sb.append(toJSONString());
	}


	@Override
	public String toJSONString() {
		return JSON.toString(toJson2());
	}


	@Override
	public Map<String,String> toJson2() throws UnsupportedOperationException {
		return new ArrayMap(
				"id", id,
				"topic", topics,
				"text", toString(),
				"recipient", recipient,
				"debug", StrUtils.join(lines, "\n")
				);
	}


	public void setFor(Map tweepInfo) {
		recipient = tweepInfo;
	}
}
