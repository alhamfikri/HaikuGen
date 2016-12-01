package org.coinvent.haiku;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import winterwell.jtwitter.Status;
import winterwell.jtwitter.TwitterTest;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.web.data.XId;

public class SyllableAssignmentTest {

	@Test
	public void testBug() {
		VocabFromTwitterProfile vftp = new VocabFromTwitterProfile(TwitterTest.newTestTwitter(), new XId("winterstein@twitter"));
		List<Status> tweets = FileUtils.load(VocabFromTwitterProfileTest.TWEET_FILE);
		assert tweets != null;
		vftp.train(tweets);		
		
		PoemVocab vocab = vftp.getVocab();
		Line line = new Line(5);
		line.words.add(new WordInfo().setPOS("JJ"));
		line.words.add(new WordInfo().setPOS("NNP"));
		line.words.add(new WordInfo().setPOS("NN"));
		SyllableAssignment sa = new SyllableAssignment(line, vocab);
		
		int[] s = sa.randomizeSyllable();
		Printer.out("syllables: ", s);
	}
	
	@Test
	public void testWithFallbackVocab() {
		VocabFromTwitterProfile vftp = new VocabFromTwitterProfile(TwitterTest.newTestTwitter(), new XId("winterstein@twitter"));
		List<Status> tweets = FileUtils.load(VocabFromTwitterProfileTest.TWEET_FILE);
		assert tweets != null;
		vftp.train(tweets);		
		
		PoemVocab vocab = vftp.getVocab();
		Line line = new Line(5);
		line.words.add(new WordInfo().setPOS("JJ"));
		line.words.add(new WordInfo().setPOS("NNP"));
		line.words.add(new WordInfo().setPOS("NN"));
		SyllableAssignment sa = new SyllableAssignment(line, vocab);
		sa.setFallbackVocab(LanguageModel.get().getAllVocab());
		
		int[] s = sa.randomizeSyllable();
		Printer.out("syllables: ", s);
	}
	
	@Test
	public void testTemplateLine() {
		String line = "the null has null --";
	}
	
	@Test
	public void testRandomizeSyllableBlankLine() {
		Line line = new Line(5);
		SyllableAssignment sa = new SyllableAssignment(line, LanguageModel.get().allVocab);		
		int[] syllables = sa.randomizeSyllable();
		int sum = 0;
		for (int i : syllables) {
			sum += syllables[i];
		}
		Printer.out(syllables);
		assert sum == 5 : sum;
	}
	
	
	@Test
	public void testRandomizeSyllableTemplateLine() {
		Line line = new Line(5);
		line.words.add(new WordInfo());
		line.words.add(new WordInfo("of", 1).setFixed(true));
		line.words.add(new WordInfo());
		SyllableAssignment sa = new SyllableAssignment(line, LanguageModel.get().allVocab);		
		int[] syllables = sa.randomizeSyllable();
		int sum = 0;
		for (int i : syllables) {
			sum += i;
		}
		Printer.out(syllables);
		assert sum == 5 : sum;
	}
	
	@Test
	public void testRandomizeSyllableAllFixed() {
		Line line = new Line(5);
		line.words.add(new WordInfo("the", 1).setFixed(true));
		line.words.add(new WordInfo("joyful", 2).setFixed(true));
		line.words.add(new WordInfo("hippo", 2).setFixed(true));
		SyllableAssignment sa = new SyllableAssignment(line, LanguageModel.get().allVocab);		
		int[] syllables = sa.randomizeSyllable();
		Printer.out(syllables);
		int sum = 0;
		for (int si : syllables) {
			sum += si;
		}
		assert sum == 5 : sum;
	}

}
