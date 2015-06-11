import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


public class TopicModel {
	HashMap<String, HashMap<String,Double>> count;
	HashSet<String> topicWords;
	
	public TopicModel() {
		count = new HashMap<String, HashMap<String,Double>>();
		topicWords = new HashSet<String>();
	}

	public void add(String wordPrev, String wordNext) {
		if (count.get(wordPrev) == null) {
			count.put(wordPrev, new HashMap<String,Double>());
		}
		
		HashMap<String,Double> count_map = count.get(wordPrev);
		
		if (count_map.get(wordNext) == null) {
			count_map.put(wordNext,1.0);
		} else {
			count_map.put(wordNext, count_map.get(wordNext) + 1);
		}		
	}
	
	public void add(ArrayList<String> sentence) {
		ArrayList<String> tmp = new ArrayList<String>();
		int N = sentence.size();
		
		for (int i=0;i<N;i++) {
			if (topicWords.contains(sentence.get(i))) {
				tmp.add(sentence.get(i));
			}
		}
		
		N = tmp.size();
		for (int i=0;i<N;i++){
			for (int j=0;j<N;j++){
				add(tmp.get(i),tmp.get(j));
			}
		}
	}

	public void addTopicWord(String word) {
		topicWords.add(word);
	}
	
	/**
	 * Compute the topic relevance score from given 2 words
	 * @return
	 */
	public double topicRelevanceScore(String[] sentence1, String[] sentence2) {
		HashMap<String,Double> vector1 = getWordVector(sentence1);
		HashMap<String,Double> vector2 = getWordVector(sentence2);
		
		double res = 0.0;
		
		for (String word : vector1.keySet()) {
			if (vector2.get(word) == null)
				continue;
		    res += (vector1.get(word) * vector2.get(word));
		}
		
		return res;
	}

	private HashMap<String, Double> getWordVector(String[] sentence) {
		HashMap<String,Double> res = new HashMap<String,Double>();
		for (int i=0;i<sentence.length;i++) {
			HashMap<String,Double> m = count.get(sentence[i]);
			if (m == null)
				continue;
			System.err.println(sentence[i]);
			System.err.println(m.size());
			
			m.forEach((k, v) -> res.merge(k, v, (v1, v2) -> v1 + v2));

			System.err.println(res.size());
		}
		//compute the magnitude
		double len = 0.0;
		for (double value : res.values()) {
		    len += (value * value);
		}
		
		final double len2 = Math.sqrt(len);
		System.err.println("Topic vector length "+len2);
		res.forEach((k, v) -> res.put(k, v / len2));
		
		len = 0.0;
		for (double value : res.values()) {
		    len += (value * value);
		}
		System.err.println(Math.sqrt(len));
		return res;
		
	}
}
