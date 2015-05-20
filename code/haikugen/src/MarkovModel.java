import java.util.HashMap;


public class MarkovModel {

	HashMap<String, HashMap<String,Integer>> count;
	
	public MarkovModel() {
		count = new HashMap<String, HashMap<String,Integer>>();
	}

	/**
	 * add new occurrence pair of word and wordPrev in markov model. 
	 * @param word
	 * @param wordPrev
	 */
	public void add(String word, String wordPrev) {
		if (count.get(word) == null) {
			count.put(word, new HashMap<String,Integer>());
		}
		
		HashMap<String,Integer> count_map = count.get(word);
		
		if (count_map.get(wordPrev) == null) {
			count_map.put(wordPrev,0);
		} else {
			count_map.put(wordPrev, count_map.get(wordPrev) + 1);
		}
	}

	public int getCount(String word, String word2) {
		if (count.get(word) == null)
			return 0;
		HashMap<String,Integer> count_map = count.get(word);
		if (count_map.get(word2) == null)
			return 0;
		
		return count_map.get(word2);
		
	}

}
