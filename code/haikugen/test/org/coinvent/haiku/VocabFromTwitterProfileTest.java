package org.coinvent.haiku;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import winterwell.jtwitter.Status;
import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterTest;
import com.winterwell.maths.stats.distributions.cond.Cntxt;
import com.winterwell.maths.stats.distributions.cond.Sitn;
import com.winterwell.maths.stats.distributions.cond.WWModel;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.nlp.io.DumbTokenStream;
import com.winterwell.nlp.io.ITokenStream;
import com.winterwell.nlp.io.SitnStream;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.nlp.io.pos.PosTagByOpenNLP;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.Containers;

import com.winterwell.utils.Printer;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.web.data.XId;

public class VocabFromTwitterProfileTest {

	static final File TWEET_FILE = new File("test-data/winterstein-tweets.xml");

	@Test
	public void testPOSTagging() {
		VocabFromTwitterProfile vftp = new VocabFromTwitterProfile(null,null);
		String text = "I'd like this tagged, but most can't - or won't - handle it.";
		ITokenStream tokens =  vftp.tokeniser.factory(text);		
		SitnStream ss = new SitnStream(null, tokens, LanguageModel.sig);
		String words = ss.getText();
		String[] tags = PosTagByOpenNLP.tag(words.split(" "));
		String[] tagsNoTokenising = PosTagByOpenNLP.tag(text.split(" "));
		Printer.out(tags);
	}
	
	@Test
	public void testSampleAll() {
		WWModel<Tkn> wordGen = LanguageModel.get().getAllWordModel();
		doSample(wordGen);
	}
	
	@Test
	public void testSampleTweets() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		VocabFromTwitterProfile vftp = new VocabFromTwitterProfile(jtwit, new XId("winterstein@twitter"));
		List<Status> tweets = FileUtils.load(TWEET_FILE);
		vftp.train(tweets);
		PoemVocab vocab = vftp.getVocab();
		assert vocab.getAllWords().size() > 10;
		WWModel<Tkn> wordGen = vftp.getWordModel();		
				
		for(int j=0; j<10; j++) {
			doSample(wordGen);
		}
	}
	
	private void doSample(WWModel<Tkn> wordGen) {
		List<Tkn> text = new ArrayList();
		text.add(new Tkn("go"));
		text.add(new Tkn("in"));
		text.add(new Tkn("the"));
		for(int i=0 ; i<10; i++) {
			text.add(new Tkn("dummy"));
			SitnStream ss = new SitnStream(null, new DumbTokenStream(text), LanguageModel.sig);
			List<Sitn<Tkn>> sitns = Containers.getList(ss);
			Sitn<Tkn> s = sitns.get(sitns.size()-1);
			Tkn sample = wordGen.sample(s.context);
			text.set(text.size()-1, sample);
		}
		System.out.println(text);
	}

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
