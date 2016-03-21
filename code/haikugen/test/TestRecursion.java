import java.util.ArrayList;

import org.coinvent.haiku.Haiku;
import org.coinvent.haiku.HaikuGenerator;
import org.coinvent.haiku.HaikuMain;
import org.coinvent.haiku.LanguageModel;
import org.junit.Test;

import winterwell.utils.Utils;


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
		
		Haiku[] haikus = HaikuMain.loadHaikus();
		LanguageModel languageModel = HaikuMain.loadCorpus();
		int constraint[] = {5,7,5};
		HaikuGenerator generator = new HaikuGenerator(languageModel, haikus, constraint);
		if (Utils.isBlank(topic)) {
			topic = languageModel.getRandomTopic();
		}

		System.out.print("Creating Haiku");
		for (int i=0;i<=10;i++){			
			Haiku res = generator.generate(topic);			
			res.setTopics(topic);
			System.out.println(res+"\n");
			topic = res.toString();
		}

	}
}
