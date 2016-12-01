package org.coinvent.haiku;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.winterwell.nlp.io.ATokenStream;
import com.winterwell.nlp.io.ITokenStream;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.utils.containers.AbstractIterator;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;

public class Line extends ATokenStream {

	/**
	 * zero indexed
	 */
	int num;
	
	public Line() {
		
	}
	public Line(int syllables) {
		this.syllables = syllables;
	}

	List<WordInfo> words = new ArrayList();
	
	int syllables;
	
	public String toString() {
		if (Utils.isEmpty(words)) return "Line[syllables="+syllables+"]";
		return StrUtils.join(words, " ");
	}

	@Override
	public AbstractIterator<Tkn> iterator() {
		final Iterator<WordInfo> it = words.iterator();
		return new AbstractIterator<Tkn>() {
			@Override
			protected Tkn next2() throws Exception {
				if ( ! it.hasNext()) return null;
				WordInfo wi = it.next();
				return wi.getTkn();
			}
		};
	}
	
	@Override
	public ITokenStream factory(String input) {
		throw new UnsupportedOperationException();
	}
}
