package org.coinvent.haiku;

import java.util.ArrayList;
import java.util.Random;

public class SyllableAssignment {
	
	private Line line;

	public SyllableAssignment(Line line) {
		this.line = line;
	}
	
	LanguageModel languageModel = LanguageModel.get();

	/**
	 * Uniformly randomize the syllable distribution with dynamic programming
	 * @param N = target syllable
	 * @param tags = array of tag 
	 * @param isKeep = array of integer. If isKeep[i] equal to 1, we keep the original words, thus we force their syllable is equal to the origin.
	 * @return array of integer : the syllable count for each tag
	 */
	public int[] randomizeSyllable() {
		int numWords = line.words.size();
		assert numWords > 0;
		int res[] = new int[numWords];
		int syllablesLeft = line.syllables;
		assert syllablesLeft > 0;
		
		// word -> list of syllable lengths
		ArrayList<Integer> options[] = new ArrayList[numWords];
		for (int i=0; i<numWords; i++) {
			WordInfo word = line.words.get(i);
			if ( ! word.fixed) {
				options[i] = languageModel.getPossibleSyllables(word.pos);
			} else {
				options[i] = new ArrayList<Integer>();
				options[i].add(0);				
				syllablesLeft -= word.syllables;
			}
		}
		//dp[i][j] = 1, if we can make first j words subsequence with total of i syllables
		if (numWords < 0) {
			return null;
		}
		int dp[][] = new int[numWords+1][syllablesLeft+1];
		dp[0][0] = 1;
		
		//bottom-up dynamic programming
		for (int i=0;i<=numWords;i++){
			for (int j=0;j<syllablesLeft;j++){
				if (dp[i][j] == 0) 
					continue;
				for (int k=0;k<options[j].size() && i + options[j].get(k) <= numWords; k++){
					dp[i + options[j].get(k)][j+1] += dp[i][j];
				}
			}
		}
		
		if (dp[numWords][syllablesLeft] == 0)
			return null;
		
		//uniformly chooses the valid syllables distribution
		Random rand = new Random();
		int pos = numWords;
		for (int i=syllablesLeft;i>0;i--) {
			int chance = rand.nextInt(dp[pos][i]) + 1;
			for (int j=0;j<options[i-1].size();j++){
				if (pos - options[i-1].get(j) >= 0 && chance <= dp[pos - options[i-1].get(j)][i-1]) {
					res[i-1] = options[i-1].get(j);
					break;
				}
				if (pos - options[i-1].get(j) >= 0)
					chance -= dp[pos - options[i-1].get(j)][i-1];
			}
			pos = pos - res[i-1];
		}
		// set the answers
		for(int i=0; i<line.words.size(); i++) {
			line.words.get(i).syllables = res[i];
		}
		return res;
	}
}
