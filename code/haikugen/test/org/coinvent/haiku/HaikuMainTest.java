package org.coinvent.haiku;
import java.util.ArrayList;
import java.util.List;

import org.coinvent.haiku.Haiku;
import org.coinvent.haiku.HaikuGenerator;
import org.coinvent.haiku.HaikuMain;
import org.coinvent.haiku.LanguageModel;
import org.junit.Test;


public class HaikuMainTest {

	@Test
	public void testLove() {
		List<Haiku> haikus = HaikuMain.loadHaikus();
		LanguageModel languageModel = HaikuMain.loadCorpus();
		int constraint[] = {5,7,5};
		HaikuGenerator generator = new HaikuGenerator(languageModel, haikus, constraint);
		String idea = "love";
		String randomizedIdea = "";
		if (idea.length() == 0) {
			randomizedIdea = languageModel.getRandomTopic();
		}
		System.out.println("Creating Haiku...");
		Haiku res = generator.generate(idea + randomizedIdea);
		System.out.println(res);		
	}
	
}
