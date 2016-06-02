package org.coinvent.haiku;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class HaikuGenerator2Test {

	@Test
	public void testGenerate() {
		List<Haiku> haikus = HaikuMain.loadHaikus();
		LanguageModel languageModel = LanguageModel.get();
		int constraint[] = {5,7,5};
		PoemGenerator generator = new PoemGenerator(languageModel, haikus, constraint);
		Poem haiku = generator.generate("love", "food");
		System.out.println("Love Food");
		System.out.println(haiku);
	}

}
