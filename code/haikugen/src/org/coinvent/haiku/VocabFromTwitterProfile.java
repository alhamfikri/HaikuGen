package org.coinvent.haiku;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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
import winterwell.nlp.io.pos.PosTagByOpenNLP;
import winterwell.utils.IFn;

/**
 * @testedby {@link VocabFromTwitterProfileTest}
 * @author daniel
 *
 */
public class VocabFromTwitterProfile {

	private Twitter jtwit;
	private XId txid;
	private PoemVocab vocab = new PoemVocab();
	private WWModel<Tkn> wordModel = LanguageModel.newWordModel();

	public PoemVocab getVocab() {
		return vocab;
	}
	public WWModel<Tkn> getWordModel() {
		return wordModel;
	}
	
	/**
	 * 
	 * @param jtwit Can be null provided you don't use a fetch
	 * @param target Can be null provided you don't use a fetch
	 */
	public VocabFromTwitterProfile(Twitter jtwit, XId target) {
		this.jtwit = jtwit;
		this.txid = target;
	}
	
	ITokenStream tokeniser = LanguageModel.get().tweetTokeniser;
	
	public void run() {
		List<Status> tweets = fetchTweets();
		train(tweets);
		// and the bio
		if ( ! tweets.isEmpty()) {
			Status t = tweets.get(0);
			String desc = t.getUser().getDescription();
			if (desc!=null) {
				IDocument doc = new SimpleDocument(null, desc, t.getUser().screenName);			
				train(doc);
			}
		}
	}
	
	void train(List<Status> tweets) {		
		for (Status status : tweets) {
			IDocument doc = new SimpleDocument(null, status.text, status.getUser().screenName);
			train(doc);
		}
	}

	void train(IDocument doc) {
		ITokenStream tokens = tokeniser.factory(doc.getContents());		
		SitnStream ss = new SitnStream(doc, tokens, LanguageModel.sig);
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
