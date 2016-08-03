package org.coinvent.haiku;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.winterwell.utils.containers.ArrayMap;

import creole.data.XId;
import winterwell.jtwitter.Status;
import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.User;
import winterwell.maths.stats.distributions.cond.Sitn;
import winterwell.maths.stats.distributions.cond.WWModel;
import winterwell.maths.stats.distributions.cond.WWModelFactory;
import winterwell.nlp.corpus.IDocument;
import winterwell.nlp.corpus.SimpleDocument;
import winterwell.nlp.corpus.brown.BrownDocument;
import winterwell.nlp.io.DumbTokenStream;
import winterwell.nlp.io.ITokenStream;
import winterwell.nlp.io.SitnStream;
import winterwell.nlp.io.Tkn;
import winterwell.nlp.io.pos.PosTagByOpenNLP;
import winterwell.utils.IFn;
import winterwell.utils.containers.Containers;
import winterwell.utils.reporting.Log;

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
	
	ITokenStream tokeniser = LanguageModel.get().tokeniser;
	private ArrayMap tweepInfo;
	
	public void setTokeniser(ITokenStream tokeniser) {
		this.tokeniser = tokeniser;
	}
	
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
		SitnStream ss = new SitnStream(doc, tokens, wordModel.getContextSignature());
		String text = ss.getText();
		// guess fallback POS tags
		List<Tkn> postags = LanguageModel.posTag(new DumbTokenStream(text));
		List<Sitn<Tkn>> tkns = Containers.getList(ss);
		assert postags.size() == tkns.size() : postags+" !~ "+tkns;
		for (int i=0; i<tkns.size(); i++) {
			Sitn<Tkn> sitn = tkns.get(i);
			// stats-model
			wordModel.train1(sitn.context, sitn.outcome);
			// vocab
			int s = LanguageModel.get().getSyllable(sitn.outcome.getText());
			WordInfo wi = new WordInfo(sitn.outcome.getText(), s);
			String tag = sitn.outcome.getPOS();
			if (tag==null) {
				tag = postags.get(i).getPOS();
			}
			wi.setPOS(tag);
			vocab.addWord(wi);
		}
	}

	List<Status> fetchTweets() {
		jtwit.setMaxResults(100);
		List<Status> tweets = jtwit.getUserTimeline(txid.getName());
		if (tweets.size()!=0) {
			User user = tweets.get(0).getUser();
			tweepInfo = new ArrayMap(
					"screenname", user.screenName,
					"name", user.name,
					"img", user.getProfileImageUrl()
					);
		}
		return tweets;
	}
	public Map getTweepInfo() {
		return tweepInfo;
	}
	
}
