package org.coinvent.haiku;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.sun.xml.internal.ws.api.ComponentFeature.Target;

import creole.data.XId;
import winterwell.jtwitter.Status;
import winterwell.jtwitter.Twitter;
import winterwell.maths.stats.distributions.cond.Sitn;
import winterwell.maths.stats.distributions.cond.WWModel;
import winterwell.maths.stats.distributions.cond.WWModelFactory;
import winterwell.nlp.corpus.IDocument;
import winterwell.nlp.corpus.SimpleDocument;
import winterwell.nlp.io.ITokenStream;
import winterwell.nlp.io.SitnStream;
import winterwell.nlp.io.Tkn;
import winterwell.nlp.io.WordAndPunctuationTokeniser;
import winterwell.nlp.io.pos.PosTagByOpenNLP;
import winterwell.utils.IFn;

public class VocabFromTwitterProfile {

	private Twitter jtwit;
	private XId txid;
	private PoemVocab vocab;
	private WWModel<Tkn> wordModel;

	public PoemVocab getVocab() {
		return vocab;
	}
	public WWModel<Tkn> getWordModel() {
		return wordModel;
	}
	
	public VocabFromTwitterProfile(Twitter jtwit, XId target) {
		this.jtwit = jtwit;
		this.txid = target;
	}
	
	ITokenStream tokeniser = LanguageModel.get().tweetTokeniser;
	
	void run() {
		List<Status> tweets = fetchTweets();
		train(tweets);
	}
	
	void train(List<Status> tweets) {
		vocab = new PoemVocab();
		List<String> sig = Arrays.asList(LanguageModel.get().sig);
		WWModelFactory wwmf = new WWModelFactory();
		IFn<List<String>, int[]> trackedFormula = wwmf.trackedFormula(1000, 2, 100, 2);
		wordModel = wwmf.fullFromSig(sig, null, 
				trackedFormula, 
				1, 100, new HashMap()
				);
		for (Status status : tweets) {
			IDocument doc = new SimpleDocument(null, status.text, status.getUser().screenName);
			train(doc);
		}
	}

	void train(IDocument doc) {
		ITokenStream tokens = tokeniser.factory(doc.getContents());		
		SitnStream ss = new SitnStream(doc, tokens, LanguageModel.get().sig);
		for (Sitn<Tkn> sitn : ss) {
			// stats-model
			wordModel.train1(sitn.context, sitn.outcome);
			// vocab
			int s = LanguageModel.get().getSyllable(sitn.outcome.getText());
			WordInfo wi = new WordInfo(sitn.outcome.getText(), s);
			List<String> tags = PosTagByOpenNLP.getPossibleTags(sitn.outcome.getText());
			if (tags!=null && ! tags.isEmpty()) wi.setPOS(tags.get(0));
			vocab.addWord(wi);
		}
	}

	List<Status> fetchTweets() {
		jtwit.setMaxResults(100);
		List<Status> tweets = jtwit.getUserTimeline(txid.getName());
		return tweets;
	}
	
}
