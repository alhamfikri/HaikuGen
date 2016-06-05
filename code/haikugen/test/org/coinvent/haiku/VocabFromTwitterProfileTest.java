package org.coinvent.haiku;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.junit.Test;

import creole.data.XId;
import winterwell.jtwitter.Status;
import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterTest;
import winterwell.maths.stats.distributions.cond.Cntxt;
import winterwell.maths.stats.distributions.cond.WWModel;
import winterwell.nlp.corpus.IDocument;
import winterwell.nlp.io.Tkn;

import com.winterwell.utils.io.FileUtils;

public class VocabFromTwitterProfileTest {

	private static final File TWEET_FILE = new File("test-data/tweets.xml");

	@Test
	public void testRun() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		VocabFromTwitterProfile vftp = new VocabFromTwitterProfile(jtwit, new XId("winterstein@twitter"));
		vftp.run();
	}

	@Test
	public void testTrain() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		VocabFromTwitterProfile vftp = new VocabFromTwitterProfile(jtwit, new XId("winterstein@twitter"));
		List<Status> tweets = FileUtils.load(TWEET_FILE);
		vftp.train(tweets);
		PoemVocab vocab = vftp.getVocab();
		assert vocab.getAllWords().size() > 10;
		WWModel<Tkn> wm = vftp.getWordModel();
		System.out.println(wm);
		Object[] bits = new Object[]{Tkn.START_TOKEN, Tkn.START_TOKEN, null};
		Cntxt cntxt = new Cntxt(LanguageModel.sig, bits);
		for(int i=0; i<100; i++) {
			System.out.print(wm.sample(cntxt)+" ");
		}
		System.out.println(wm.getMarginal(cntxt));
	}

	@Test
	public void testFetchTweets() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		VocabFromTwitterProfile vftp = new VocabFromTwitterProfile(jtwit, new XId("winterstein@twitter"));
		List<Status> fetched = vftp.fetchTweets();
		assert fetched.size() > 10;
		FileUtils.save(fetched, TWEET_FILE);
	}

}
