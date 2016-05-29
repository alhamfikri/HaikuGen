package org.coinvent.haiku;

import java.util.List;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;

public class Line {

	public Line(int syllables) {
		this.syllables = syllables;
	}

	List<WordInfo> words = new ArrayList();
	
	int syllables;
	
	public String toString() {
		if (Utils.isEmpty(words)) return "Line[syllables="+syllables+"]";
		return StrUtils.join(words, " ");
	}
}
