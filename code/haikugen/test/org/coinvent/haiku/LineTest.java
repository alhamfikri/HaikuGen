package org.coinvent.haiku;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import com.winterwell.nlp.io.Tkn;
import com.winterwell.utils.containers.Containers;

public class LineTest {

	@Test
	public void testIterator() {
		Line line = new Line(5);
		WordInfo wi = new WordInfo("thingy", 2).setPOS("JJ");
		line.words.add(new WordInfo("the", 1).setPOS("DT"));
		line.words.add(wi);
		line.words.add(new WordInfo("cat", 1).setPOS("NN"));

		List<Tkn> tkns = Containers.getList(line);
		assert tkns.size() == 3 : tkns;
	}

}
