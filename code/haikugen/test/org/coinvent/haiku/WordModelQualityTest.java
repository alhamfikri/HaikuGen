package org.coinvent.haiku;

import com.winterwell.maths.stats.distributions.cond.ExplnOfDist;
import com.winterwell.maths.stats.distributions.cond.Sitn;
import com.winterwell.maths.stats.distributions.cond.WWModel;
import com.winterwell.nlp.io.ITokenStream;
import com.winterwell.nlp.io.SitnStream;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.utils.StrUtils;

public class WordModelQualityTest {

	public static void main(String[] args) {
		LanguageModel lm = LanguageModel.get();
		new WordModelQualityTest().runTest(lm);
	}

	private void runTest(LanguageModel lm) {
		WWModel<Tkn> wm = 
//				lm.newWordModel(); 
				lm.getAllWordModel();
		
		System.out.println(wm.getDebugInfo());
		// Score some text -- pref the training text!
		ITokenStream tokeniser = lm.getTokeniser();
		ITokenStream words = tokeniser.factory("the stegosaurus");
		SitnStream ss = new SitnStream(null, words, wm.getContextSignature());
		
		for (Sitn<Tkn> sitn : new SitnStream(null, tokeniser.factory("What the bleep went wrong?"), wm.getContextSignature())) {
			wm.train1(sitn.context, sitn.outcome);
		}
		
		for (Sitn<Tkn> sitn : ss) {
			ExplnOfDist expln = new ExplnOfDist();
			double p = wm.probWithExplanation(sitn.outcome, sitn.context, expln);
			System.out.println(sitn+"\t"+StrUtils.toNSigFigs(p, 2)+" "+expln.toString());
		}			
		
	}
	
}
