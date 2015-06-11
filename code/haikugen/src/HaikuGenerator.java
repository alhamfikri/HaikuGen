import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;


public class HaikuGenerator {

	private LanguageModel languageModel;
	private Haiku haikus[];
	private int syllableConstraint[];
	private int haikuSize;
	/**
	 * Constructor
	 * @param languageModel : set corpus for words and relational information
	 * @param haikus : set haikus for generating the grammatical skeleton
	 * @param syllables : syllables constraint, ex: [5,7,5]. Set 0 for no constraint
	 */
	public HaikuGenerator(LanguageModel languageModel, Haiku haikus[], int syllableConstraint[]){
		this.languageModel = languageModel;
		this.haikus = haikus;
		this.syllableConstraint = syllableConstraint;
		this.haikuSize = haikus.length;
	}
	
	public String[][] generate() {
		Random random = new Random();
		
		String[][] result = new String[3][];
		String[][] tag = new String[3][];
		int syllable[][] = new int[3][];
		
		for (int i=0;i<3;i++){
			tag[i] = haikus[random.nextInt(haikuSize)].getTag()[i];
			syllable[i] = randomizeSyllable(syllableConstraint[i],tag[i]);
			if(syllable[i] == null)
				return null;
		}
				
		for (int i=0;i<tag.length;i++){
			result[i] = new String[tag[i].length];
			
			String words[] = null;
			
			for (int j=0;j<tag[i].length;j++){
				
				words = languageModel.getWordlist(tag[i][j],syllable[i][j]);
				
				String prev = "/s";
				if (j > 0)
					prev = result[i][j-1];
				String word = words[random.nextInt(words.length)];
				ArrayList<String> wordPool = new ArrayList<String>();
				ArrayList<String> uniWordPool = new ArrayList<String>();
				
				//put all next possible word into word pool
				for (int k=0;k<words.length;k++){
					double prob = languageModel.getMarkovProbability(prev,words[k]);
					if (prob > 0.0)
						wordPool.add(""+String.format( "%.5f", prob )+" "+words[k]);
					else
						uniWordPool.add(""+String.format("%05d", languageModel.getUnigramCount(words[k])) +" "+words[k]);
				}
				
				 
				
				if (wordPool.size() > 0) {
					Collections.sort(wordPool);
					Collections.reverse(wordPool);
					
					word = wordPool.get(random.nextInt(1 + wordPool.size() / 10));
					}
				else {
					Collections.sort(uniWordPool);
					Collections.reverse(uniWordPool);
					
					word = uniWordPool.get(random.nextInt(1 + uniWordPool.size() / 10));
				}
				
				result[i][j] = word.split(" ")[1];
				
			}
		}
		
		return result;
	}

	/**
	 * Uniformly randomize the syllable distribution with dynamic programming
	 * @param N = target syllable
	 * @param tags = array of tag 
	 * @return array of integer : the syllable for each tag
	 */
	public int[] randomizeSyllable(int N, String[] tags) {
		int M = tags.length;
		int res[] = new int[M];
		
		ArrayList<Integer> options[] = new ArrayList[M];
		for (int i=0;i<M;i++)
			options[i] = languageModel.getPossibleSyllables(tags[i]);
		
		//dp[i][j] = 1, if we can make first j words subsequence with total of i syllables
		int dp[][] = new int[N+1][M+1];
		dp[0][0] = 1;
		
		//backtrack array to reconstruct the solution
		int backtrack[][] = new int[N+1][M+1];
		
		//bottom-up dynamic programming
		for (int i=0;i<N;i++){
			for (int j=0;j<M;j++){
				if (dp[i][j] == 0) 
					continue;
				for (int k=0;k<options[j].size() && i + options[j].get(k) <= N;k++){
					dp[i + options[j].get(k)][j+1] += dp[i][j];
				}
			}
		}
		
		if (dp[N][M] == 0)
			return null;
		
		//uniformly chooses the valid syllables distribution
		Random rand = new Random();
		int pos = N;
		for (int i=M;i>0;i--) {
			int chance = rand.nextInt(dp[pos][i]) + 1;
			for (int j=0;j<options[i-1].size();j++){
				if (pos - options[i-1].get(j) >= 0 && chance <= dp[pos - options[i-1].get(j)][i-1]) {
					res[i-1] = options[i-1].get(j);
					break;
				}
				if (pos - options[i-1].get(j) >= 0)
					chance -= dp[pos - options[i-1].get(j)][i-1];
			}
			pos = pos - res[i-1];
		}
		return res;
	}
	
}

