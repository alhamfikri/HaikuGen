import java.util.ArrayList;
import java.util.Random;


public class HaikuGenerator {

	private Corpus corpus;
	private Haiku haikus[];
	private int syllableConstraint[];
	private int haikuSize;
	/**
	 * Constructor
	 * @param corpus : set corpus for words and relational information
	 * @param haikus : set haikus for generating the grammatical skeleton
	 * @param syllables : syllables constraint, ex: [5,7,5]. Set 0 for no constraint
	 */
	public HaikuGenerator(Corpus corpus, Haiku haikus[], int syllableConstraint[]){
		this.corpus = corpus;
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
				
				words = corpus.getWordlist(tag[i][j],syllable[i][j]);

				//randomly samples of the next word.
				//pick the most probable one
				String word = words[random.nextInt(words.length)];
				
				if (j > 0) {
					//sample
					for (int SAMPLE=0;SAMPLE<100;SAMPLE++){
						String word2 = words[random.nextInt(words.length)];
						if (corpus.getMarkovCount(word2,result[i][j-1]) > corpus.getMarkovCount(word,result[i][j-1]))
							word = word2;
					}
				}
				
				result[i][j] = word;
				
			}
		}
		
		return result;
	}

	/**
	 * Randomize the syllable distribution with dynamic programming
	 * @param N = target syllable
	 * @param tags = array of tag 
	 * @return array of integer : the syllable for each tag
	 */
	public int[] randomizeSyllable(int N, String[] tags) {
		int M = tags.length;
		int res[] = new int[M];
		
		ArrayList<Integer> options[] = new ArrayList[M];
		for (int i=0;i<M;i++)
			options[i] = corpus.getPossibleSyllables(tags[i]);
		
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
					dp[i + options[j].get(k)][j+1] = 1;
					backtrack[i + options[j].get(k)][j+1] = options[j].get(k);
				}
			}
		}
		
		if (dp[N][M] == 0)
			return null;
		
		int pos = N;
		for (int i=M;i>0;i--) {
			res[i-1] = backtrack[pos][i];
			pos = pos - res[i-1];
		}
		return res;
	}
	
}

