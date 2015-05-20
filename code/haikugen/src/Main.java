import java.util.Arrays;
import java.util.Random;


public class Main {
	public static void main(String args[]){		
		Corpus corpus = loadCorpus();
		Haiku haikus[] = loadHaikus();
		
		int constraint[] = {5,7,5};
		System.out.println(corpus.getPossibleSyllables("NN").toString());
		
		HaikuGenerator generator = new HaikuGenerator(corpus, haikus, constraint);
		System.out.println(Arrays.toString(generator.randomizeSyllable(10, new String[]{"NN","NN","NN"})));
		System.out.println(corpus.getMarkovCount("is", "he"));
		for (int i=0;i<=5;i++){
				String[][] res = generator.generate();
				if (res != null) {
					System.out.println(Arrays.toString(res[0]));
					System.out.println(Arrays.toString(res[1]));
					System.out.println(Arrays.toString(res[2]));
					System.out.println();
				}
			}
	
	}
	
	private static Haiku[] loadHaikus() {
		Haiku haikus[] = new Haiku[3];
		
		haikus[0] = new Haiku("lightning flash % what i thought were faces % are plumes of pampas grass");
		haikus[1] = new Haiku("the first cold shower % even the monkey seems to want % a little coat of straw");
		haikus[2] = new Haiku("easter bunny hides % easter eggs are out of sight % kids look everywhere");	
		
		return haikus;
	}

	/**
	 * load and train new a corpus
	 * @param corpus
	 */
	private static Corpus loadCorpus() {
		Corpus corpus = new Corpus();
		
		//loading word dictionary
		corpus.loadDictionary("cmudict");
		
		//adding corpus ROMANCE
		for (int i=1;i<0;i++) {
			System.out.println("Loading corpus: ROMANCE "+i);
			if (i < 10)
				corpus.add("cp0"+i);
			else
				corpus.add("cp"+i);
		}
		
		//adding corpus HOBBIES
		for (int i=1;i<37;i++) {
			System.out.println("Loading corpus: HOBBIES "+i);
			if (i < 10)
				corpus.add("ce0"+i);
			else
				corpus.add("ce"+i);
		}
		
		//adding corpus PRESS
		for (int i=1;i<17;i++) {
			System.out.println("Loading corpus: PRESS "+i);
			if (i < 10)
				corpus.add("cc0"+i);
			else
				corpus.add("cc"+i);
		}
		
		//adding corpus ADVENTURE
		for (int i=1;i<0;i++) {
			System.out.println("Loading corpus: ADVENTURE "+i);
			if (i < 10)
				corpus.add("cn0"+i);
			else
				corpus.add("cn"+i);
		}

		//adding corpus GENERAL
		for (int i=1;i<0;i++) {
			System.out.println("Loading corpus: GENERAL "+i);
			if (i < 10)
				corpus.add("ck0"+i);
			else
				corpus.add("ck"+i);
		}	
		
		return corpus;
	}
}
