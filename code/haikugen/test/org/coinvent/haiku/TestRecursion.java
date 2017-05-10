package org.coinvent.haiku;
import java.util.ArrayList;
import java.util.List;

import org.coinvent.haiku.Haiku;
import org.coinvent.haiku.HaikuMain;
import org.coinvent.haiku.LanguageModel;
import org.junit.Test;

import com.winterwell.utils.Utils;


/**
 * What happens if we feed the output Haiku back in as the concept?
 * Will it converge?
 * Nope. But interesting, and the underlying topic feels strong.
 * @author daniel
 *
 */
public class TestRecursion {

	@Test
	public void testRecursion() {
		String topic = "poetry";
		
		List<Haiku> haikus = HaikuMain.loadHaikus();
		LanguageModel languageModel = LanguageModel.get();
		int constraint[] = {5,7,5};
		PoemGenerator generator = new PoemGenerator(languageModel, haikus, constraint);
		if (Utils.isBlank(topic)) {
			topic = languageModel.getRandomTopic();
		}

		System.out.print("Creating Haiku");
		for (int i=0;i<=10;i++){			
			Poem res = generator.generate(topic);			
			res.setTopics(topic);
			System.out.println(res+"\n");
			topic = res.toString();
		}

	}
}
