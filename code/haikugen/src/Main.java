import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;


public class Main {
	public static void main(String args[]){		

		Haiku haikus[] = loadHaikus();
		LanguageModel languageModel = loadCorpus();
		
		int constraint[] = {5,7,5};
		
		HaikuGenerator generator = new HaikuGenerator(languageModel, haikus, constraint);
		
		for (int i=0;i<=5;i++){
				String[][] res = generator.generate();
				if (res != null) {
					System.out.println(Arrays.toString(res[0]));
					System.out.println(Arrays.toString(res[1]));
					System.out.println(Arrays.toString(res[2]));
					System.out.println();
				}
			}
		
		System.out.println(languageModel.markov.size());

		languageModel.saveMarkovModel("test1.txt");
		
		while (true) {
			Scanner scanner = new Scanner(System.in);
			String x = scanner.nextLine();
			String y = scanner.nextLine();
			System.out.println(languageModel.getRelevance(x, y));
		}
	}
	
	private static Haiku[] loadHaikus() {
		
		String currentDirectory = System.getProperty("user.dir");
		String line = null;

		Haiku haikus[] = null;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(currentDirectory + "/res/model/haiku"));
			String input = br.readLine();
			int i = 0;
			int N = Integer.parseInt(input);
			haikus = new Haiku[N];
			
			while ((input = br.readLine()) != null) {
				haikus[i++] = new Haiku(input);
				haikus[i-1].print();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		return haikus;
	}

	private static void brownOpen(LanguageModel languageModel,int N,String code) {
		ArrayList<ArrayList<String>> data;
		
		//adding corpus 
		for (int i=1;i<N;i++) {
			//System.out.println("Loading corpus: );
			if (i < 10)
				data = CorpusReader.readBrown(code+"0"+i);
			else
				data = CorpusReader.readBrown(code+i);
		
			languageModel.trainMarkov(data);
			languageModel.trainTopicModel(data);
		}		
	}
	
	/**
	 * load and train new a corpus
	 * @param corpus
	 */
	private static LanguageModel loadCorpus() {
		LanguageModel languageModel = new LanguageModel();

		languageModel.loadForbiddenDictionary("names__f.csv");
		languageModel.loadForbiddenDictionary("names__m.csv");
		languageModel.loadStopWords("names__f.csv");
		languageModel.loadStopWords("names__m.csv");
		languageModel.loadStopWords("stop-words_english_1_en.txt");
		languageModel.loadStopWords("stop-words_english_2_en.txt");
		languageModel.loadStopWords("stop-words_english_3_en.txt");
		languageModel.loadStopWords("stop-words_english_4_google_en.txt");
		languageModel.loadStopWords("stop-words_english_5_en.txt");
		languageModel.loadStopWords("stop-words_english_6_en.txt");
		
		//loading word dictionary
		languageModel.loadDictionary("cmudict");
		for (int i=0;i<1;i++) {
			System.err.println("TRAIN WIKI! " + i);
			ArrayList<ArrayList<String>> data;
			data = CorpusReader.readWikipedia("englishText_"+i*10000+"_"+(i+1)*1000+"0");
			languageModel.trainMarkov(data);
			languageModel.trainTopicModel(data);
			
		}
		//languageModel.trainMarkov(CorpusReader.readWikipedia("englishText_10000_20000"));

		brownOpen(languageModel,44,"ca");
		brownOpen(languageModel,75,"cg");
		brownOpen(languageModel,80,"cj");
		brownOpen(languageModel,24,"ch");
		brownOpen(languageModel,20,"ck");
		brownOpen(languageModel,9,"cr");

		System.err.println("DONE");
		return languageModel;
	}
}