package org.coinvent.haiku;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;

import com.winterwell.utils.MathUtils;

import winterwell.maths.stats.distributions.IntDistribution;
import winterwell.nlp.corpus.brown.BrownCorpusTags;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;

/**
 * Pick per-word syllable counts to fit a line-length constraint.
 * @author daniel
 *
 */
public class SyllableAssignment {
		

	private Line line;

	PoemVocab vocab;
	
	public SyllableAssignment(Line line, PoemVocab vocab) {
		this.line = line;
		this.vocab = vocab;
		Utils.check4null(line, vocab);
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
		assert numWords > 0 : "TODO support no-template lines"; 		
		int totalSyllables = line.syllables;
		assert totalSyllables > 0;

		double[][][] weightWordsTotalSyllablesLastSyllable = new double[numWords+1][totalSyllables+1][vocab.MAX_SYLLABLES + 1];
		// e.g. weightWordsSyllables[2][3][1] =~ number of ways we can make the 
		// first 2 words with a total length of 3 syllables, and the last word uses 1 syllable
		// kick things off: one option (blank) for 0 words (NB: this rules out starting with punctuation, but oh well).
//		for(int i=0; i<log_weightWordsTotalSyllablesLastSyllable.length; i++) {
//			for(int j=0; j<log_weightWordsTotalSyllablesLastSyllable[i].length; j++) {
//				double[] arr = log_weightWordsTotalSyllablesLastSyllable[i][j];
//				Arrays.fill(arr, Double.NEGATIVE_INFINITY);
//			}
//		}
		weightWordsTotalSyllablesLastSyllable[0][0][0] = 1; // i.e. log(1)
		// run through the other words
		for(int wi = 1; wi <= numWords; wi++) {			
			// Is this word fixed? -- treat 0-syllables as fixed
			WordInfo wordi = line.words.get(wi-1); // adjust wi back to zero index
			boolean fixed = wordi!=null && wordi.fixed;
			if (fixed || wordi.syllables()==0) {
				assert wordi.syllables() >= 0;
				for(int prevTotal=0; prevTotal <= totalSyllables; prevTotal++) {
					int totali = prevTotal + wordi.syllables();
					if (totali>totalSyllables) {
						continue; // too long
					}
					double optionsToHere = MathUtils.sum(weightWordsTotalSyllablesLastSyllable[wi-1][prevTotal]);
					// options are the routes to here * choices here (which is 1)
					weightWordsTotalSyllablesLastSyllable[wi][totali][wordi.syllables()] 
							= optionsToHere;
				}
				continue;
			}
			// what choices for word i?
			for(int s=1; s<=vocab.MAX_SYLLABLES; s++) {
				// skip if total is too high
				if (s>totalSyllables) continue;
				Set<String> words = vocab.getWordlist(wordi.pos, s);
				// e.g. verb of 7 syllables -- probably none
				if (words==null || words.isEmpty()) continue;
				for(int prevTotal=0; prevTotal <= totalSyllables; prevTotal++) {
					int totali = prevTotal + s;
					if (totali>totalSyllables) {
						continue; // too long
					}
					double optionsToHere = MathUtils.sum(weightWordsTotalSyllablesLastSyllable[wi-1][prevTotal]);
					assert optionsToHere >= 0 : optionsToHere;
					// options are the routes to here * choices here
					// ??should we use logs here??
					double options = optionsToHere * words.size();
					if (options > MathUtils.TOO_BIG) options = MathUtils.TOO_BIG;
					assert options >= 0 : options+" = "+optionsToHere+" * "+words.size();
					weightWordsTotalSyllablesLastSyllable[wi][totali][s] = options;
				}
			}
		}
		
		// Pick a path -- last word first
		// The probability of a given chain is based on the number of ways of making that chain.
		// So e.g. total 7 -> 3,1,3 will be more common than total 7 -> 7 
		int res[] = new int[numWords];
		int syllablesLeft = totalSyllables;
		for(int wi=numWords; wi>0; wi--) {
			double[] options = weightWordsTotalSyllablesLastSyllable[wi][syllablesLeft];
			IntDistribution poptions = new IntDistribution(options);
			double s = poptions.sample();
			// sanity check
			if (s==0) {				
				WordInfo w = line.words.get(wi-1);
				assert ! StrUtils.isWord(w.pos) : w.pos+" "+w+" "+s;
			}
			res[wi-1] = (int) s;			
			syllablesLeft -= s;
		}
		
		// set the answers
		for(int i=0; i<line.words.size(); i++) {
			assert res[i] >= 0;
			WordInfo wi = line.words.get(i);
			wi.setSyllables(res[i]);
		}
		return res;
	}
}
