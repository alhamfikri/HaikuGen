package org.coinvent.haiku;

import java.util.List;

/**
 * http://www.renga-platform.co.uk/webpages/renga_01.htm
 * 
 * Verses connect to previous (markov chain)
 * Link and shift
 * Avoid repetition
 * 
 * Nijuin format: 4 verses, 12, 4
 * 
 * Alternate: 3 lines, 2 lines
 * 
 * Move through 4 seasons, not necc in order
 * 
 * 3rd verse: moon reference
 * 19th verse: flower
 * love somewhere for 2-3 verses
 * 
 * @author daniel
 *
 */
public class RengaMaker {

	public static void main(String[] args) {
		List<Haiku> haikus = HaikuMain.loadHaikus();
		LanguageModel languageModel = LanguageModel.get();
		int constraint[] = {5,7,5};
		
		PoemGenerator generator1 = new PoemGenerator(languageModel, haikus, constraint);
		PoemGenerator generator2 = new PoemGenerator(languageModel, haikus, new int[]{7,7});
		String idea = "japan";
		
		Poem v1 = generator1.generate(idea, "spring season");
		System.out.println(v1.toString().trim());		
		System.out.println("");
		
		Poem v2 = generator2.generate(v1.toString(), null);
		System.out.println(v2.toString().trim());
		System.out.println("");
		
		Poem v3 = generator1.generate(v2.toString(), "moon moon moon moon moon moon");
		System.out.println(v3.toString().trim());		
		System.out.println("");
		Poem prev = v3;
		
		Poem v4 = generator2.generate(prev.toString(), null);
		prev = v4;
		System.out.println(prev.toString().trim());
		System.out.println("");
		
		Poem v5 = generator1.generate(prev.toString(), "love love love love love");
		prev = v5;
		System.out.println(prev.toString().trim());
		System.out.println("");

		Poem v6 = generator2.generate(prev.toString(), "love love");
		prev = v6;
		System.out.println(prev.toString().trim());
		System.out.println("");
		
		Poem v7 = generator1.generate(prev.toString(), null);
		prev = v7;
		System.out.println(prev.toString().trim());
		System.out.println("");

		Poem v8 = generator2.generate(prev.toString(), "summer summer summer summer summer");
		prev = v8;
		System.out.println(prev.toString().trim());
		System.out.println("");
		
		Poem v9 = generator1.generate(prev.toString(), null);
		prev = v9;
		System.out.println(prev.toString().trim());
		System.out.println("");

		Poem v10 = generator2.generate(prev.toString(), null);
		prev = v10;
		System.out.println(prev.toString().trim());
		System.out.println("");
		
		Poem v11 = generator1.generate(prev.toString(), null);
		prev = v11;
		System.out.println(prev.toString().trim());
		System.out.println("");

		Poem v12 = generator2.generate(prev.toString(), "autumn autumn autumn");
		prev = v12;
		System.out.println(prev.toString().trim());
		System.out.println("");
		
		Poem v13 = generator1.generate(prev.toString(), null);
		prev = v13;
		System.out.println(prev.toString().trim());
		System.out.println("");

		Poem v14 = generator2.generate(prev.toString(), null);
		prev = v14;
		System.out.println(prev.toString().trim());
		System.out.println("");
		
		Poem v15 = generator1.generate(prev.toString(), null);
		prev = v15;
		System.out.println(prev.toString().trim());
		System.out.println("");

		Poem v16 = generator2.generate(prev.toString(), null);
		prev = v16;
		System.out.println(prev.toString().trim());
		System.out.println("");
		
		Poem v17 = generator1.generate(prev.toString(), "winter winter winter winter");
		prev = v17;
		System.out.println(prev.toString().trim());
		System.out.println("");

		Poem v18 = generator2.generate(prev.toString(), null);
		prev = v18;
		System.out.println(prev.toString().trim());
		System.out.println("");
		
		Poem v19 = generator1.generate(prev.toString(), "flower rose lily honeysuckle bloom blossom");
		prev = v19;
		System.out.println(prev.toString().trim());
		System.out.println("");

		Poem v20 = generator2.generate(prev.toString(), null);
		prev = v20;
		System.out.println(prev.toString().trim());
		System.out.println("");
		
	}
	
}
