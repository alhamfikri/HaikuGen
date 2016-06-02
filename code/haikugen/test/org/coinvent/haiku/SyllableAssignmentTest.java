package org.coinvent.haiku;

import static org.junit.Assert.*;

import org.junit.Test;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;

public class SyllableAssignmentTest {

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
			sum += syllables[i];
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
