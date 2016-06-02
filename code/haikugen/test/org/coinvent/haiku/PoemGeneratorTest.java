package org.coinvent.haiku;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import winterwell.maths.stats.distributions.cond.UnConditional;
import winterwell.maths.stats.distributions.discrete.IDiscreteDistribution;
import winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import winterwell.nlp.io.Tkn;

public class PoemGeneratorTest {

	@Test
	public void testGenerateWord() {
		List<Haiku> haikus = HaikuMain.loadHaikus();
		int constraint[] = {5,7,5};
		PoemGenerator pg = new PoemGenerator(LanguageModel.get(), haikus, constraint);
		ObjectDistribution<Tkn> dist = new ObjectDistribution<>();
		dist.train1(new Tkn("bloody"));
		dist.train1(new Tkn("damned"));
		dist.train1(new Tkn("damned"));
		dist.train1(new Tkn("sodding"));
		dist.train1(new Tkn("lovely"));
		pg.wordGen = new UnConditional<Tkn>(dist);
		
		Line line = new Line(5);
		WordInfo wi = new WordInfo("thingy", 2).setPOS("JJ");
		line.words.add(new WordInfo("the", 1).setPOS("DT"));
		line.words.add(wi);
		line.words.add(new WordInfo("cat", 1).setPOS("NN"));
		
		for(int i=0; i<5; i++) {
			Object gen = pg.generateWord(wi, line);
			System.out.println(gen);
		}
	}

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
