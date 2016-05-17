package org.coinvent.haiku;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import winterwell.utils.Utils;
import winterwell.utils.io.FileUtils;
import winterwell.utils.io.LineReader;


public class HaikuMain {
	public static void main(String args[]){		
		List<Haiku> haikus = loadHaikus();
		LanguageModel languageModel = loadCorpus();
		
		
		//System.out.println(languageModel.markov.size());
		//languageModel.saveMarkovModel("test1.txt");
		System.out.println("To use this system, please type :haiku <your_keywords>\n for example: haiku winter\n");
		System.out.println("\nBy default, our system uses (5,7,5) syllable distribution\n.Additionally, you can configure your desired syllable count by using");
		System.out.println("haiku(A,B,C) <keyword> where A,B,C are the syllable of each line");
		System.out.println("for example: haiku(3,5,3) snow");
		System.out.println("Please be wary if your syllable count is too small or too big, the system might failed to create the Haiku\n");
		
		while (true) {
			Scanner scanner = new Scanner(System.in);		
			String input = scanner.next().trim();
			int constraint[] = {5,7,5};
			String rex = "^(h|H)aiku\\(\\d+,\\d+,\\d+\\)$";
			if (input.matches(rex)) {
				String parsed[] = input.split("(\\(|,|\\))");
				input = "haiku";
				constraint[0] = Integer.parseInt(parsed[1]);
				constraint[1] = Integer.parseInt(parsed[2]);
				constraint[2] = Integer.parseInt(parsed[3]);
				
			}
			if (input.equalsIgnoreCase("Haiku")) {

				
				HaikuGenerator generator = new HaikuGenerator(languageModel, haikus, constraint);
				String idea = scanner.nextLine().trim();
				ArrayList<Haiku> candidates = new ArrayList<Haiku>();
				String randomizedIdea = "";
				if (idea.length() == 0) {
					randomizedIdea = languageModel.getRandomTopic();
				}
				ArrayList<Double> score = new ArrayList<Double>();
				System.out.print("Creating Haiku");
				for (int i=0;i<=100;i++){
					if (i % 20 == 0)
						System.out.print(".");
					
					Haiku res = generator.generate(idea + randomizedIdea);
					
					if (res!=null){
						res.setTopics(idea + randomizedIdea);
						candidates.add(res);
					}	
				}
				System.out.println("\n");
				
				Collections.sort(candidates);
				System.out.println("TOP 5 HAIKUS: ");
				for (int i=0;i<Math.min(candidates.size(), 5);i++){
					System.out.println("\n");
					candidates.get(i).print();
				}			
				if (candidates.size() > 0) {
//					candidates.get(0).print();
				} else {
					System.out.println("Ooops, we failed to create the Haiku :(");
				}
				
			}
			if (input.equalsIgnoreCase("equation")) {
				
			}
			if (input.equalsIgnoreCase("same")) {
				String idea = scanner.nextLine().trim();
				String res[] = languageModel.getClosestWords(idea, 20);
				System.out.println(Arrays.toString(res));
			}
			
			if (input.equalsIgnoreCase("predict")) {
				String text = scanner.nextLine().trim();
				System.out.println(Arrays.toString(languageModel.getClosestTopic(text)));
			}
			
			if (input.equalsIgnoreCase("predict_exclude")) {
				String text = scanner.nextLine().trim();
				System.out.println(languageModel.getClosestExcludedTopic(text));
			}
			
		}
	}
	
	public static List<Haiku> loadHaikus() {		
		File currentDirectory = FileUtils.getWorkingDirectory();
		File fhaikus = new File(currentDirectory, "res/model/haiku");
		List<Haiku> haikus = new ArrayList();
		LineReader lr = new LineReader(fhaikus);
		for (String line : lr) {
			if (Utils.isBlank(line)) continue;
			haikus.add(new Haiku(line));
		}
		lr.close();
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
		}		
	}
	
	/**
	 * load and train new a corpus
	 * @param corpus
	 */
	public static LanguageModel loadCorpus() {
		LanguageModel languageModel = new LanguageModel();
		System.err.print("Preparing..\nIt may take a minute");
		languageModel.loadDictionary("en");
		languageModel.loadForbiddenDictionary("names__f.csv");
		languageModel.loadForbiddenDictionary("names__m.csv");
		//languageModel.loadStopWords("names__f.csv");
		//languageModel.loadStopWords("names__m.csv");
		languageModel.loadStopWords("stop-words_english_1_en.txt");
		languageModel.loadStopWords("stop-words_english_2_en.txt");
		languageModel.loadStopWords("stop-words_english_3_en.txt");
		languageModel.loadStopWords("stop-words_english_4_google_en.txt");
		languageModel.loadStopWords("stop-words_english_5_en.txt");
		languageModel.loadStopWords("stop-words_english_6_en.txt");
		
		
		//loading word dictionary
		languageModel.loadSyllableDictionary("cmudict");
		
//		languageModel.loadVectorModel("glove.6B.300d.txt");
		
		// TODO data from somewhere??
//		for (int i=0;i<10;i++) {
//			System.err.print(".");
//			System.err.flush();
//			ArrayList<ArrayList<String>> data;
//			data = CorpusReader.readWikipedia("englishText_"+i*10000+"_"+(i+1)*1000+"0");
//			assert data != null;
//			languageModel.trainMarkov(data);
//		}
		
		//languageModel.trainMarkov(CorpusReader.readWikipedia("englishText_10000_20000"));
		brownOpen(languageModel,44,"ca");
		brownOpen(languageModel,75,"cg");
		brownOpen(languageModel,80,"cj");
		brownOpen(languageModel,24,"ch");
		brownOpen(languageModel,20,"ck");
		brownOpen(languageModel,9,"cr");
		
		System.err.println("\nDONE");
		return languageModel;
	}
}