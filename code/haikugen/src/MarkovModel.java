import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.text.html.HTMLDocument.Iterator;


public class MarkovModel {

	HashMap<String, HashMap<String,Integer>> count;
	int size;
	
	public MarkovModel() {
		count = new HashMap<String, HashMap<String,Integer>>();
		size = 0;
	}

	/**
	 * add new occurrence pair of word and wordPrev in markov model. 
	 * @param word
	 * @param wordPrev
	 */
	public void add(String wordPrev, String wordNext) {
		if (count.get(wordPrev) == null) {
			count.put(wordPrev, new HashMap<String,Integer>());
		}
		
		HashMap<String,Integer> count_map = count.get(wordPrev);
		
		if (count_map.get(wordNext) == null) {
			count_map.put(wordNext,1);
		} else {
			count_map.put(wordNext, count_map.get(wordNext) + 1);
		}
		
		size++;
	}

	public int getCount(String wordPrev, String wordNext) {
		if (count.get(wordPrev) == null)
			return 0;
		HashMap<String,Integer> count_map = count.get(wordPrev);
		if (count_map.get(wordNext) == null)
			return 0;
		
		return count_map.get(wordNext);
		
	}
	
	public int size(){
		return size;
	}

	/**
	 * Given a previous word, return list of string, all possible next word
	 * @param word
	 * @return
	 */
	public ArrayList<String> getAllPossiblePairs(String word) {
		ArrayList<String> res = new ArrayList<String>();
		if (count.get(word) == null)
			return null;
			
		HashMap<String,Integer> map = count.get(word);
		
		for (String key : map.keySet()) {
		    res.add(key);
		}
		return res;
	}
}
