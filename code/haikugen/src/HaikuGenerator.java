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
		tag[0] = haikus[random.nextInt(haikuSize)].getTag()[0];
		tag[1] = haikus[random.nextInt(haikuSize)].getTag()[1];
		tag[2] = haikus[random.nextInt(haikuSize)].getTag()[2];
		
		for (int i=0;i<tag.length;i++){
			result[i] = new String[tag[i].length];
			
			int syllableCount = syllableConstraint[i];
			String words[] = null;
			
			for (int j=0;j<tag[i].length - 1;j++){
				
				words = corpus.getWordlist(tag[i][j]);
				
				if (words == null)
					return null;
				
				//randomly chooses the word
				int id = random.nextInt(words.length);
				String word = words[id];
				result[i][j] = word;
				
				WordInfo wordInfo = corpus.getWordInfo(word);
				
				//update the constraint
				syllableCount -= wordInfo.syllables;
				
				//check validity
				if (syllableCount <= 0)
					return null;
			}
			
			//randomly chooses the last word
			words = corpus.getWordlist(tag[i][tag[i].length - 1], syllableCount);
			if (words == null)
				return null;
			
			int id = random.nextInt(words.length);
			String word = words[id];
			result[i][tag[i].length - 1] = word;
			
		}
		
		return result;
	}
	
}

