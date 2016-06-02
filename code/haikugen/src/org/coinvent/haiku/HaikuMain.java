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
import winterwell.utils.reporting.Log;


public class HaikuMain {
	public static void main(String args[]){		
		List<Haiku> haikus = loadHaikus();
		LanguageModel languageModel = LanguageModel.get();
		
		
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
				PoemGenerator generator = new PoemGenerator(languageModel, haikus, constraint);
				String idea = scanner.nextLine().trim();
				ArrayList<Poem> candidates = new ArrayList<>();
				String randomizedIdea = "";
				if (idea.length() == 0) {
					randomizedIdea = languageModel.getRandomTopic();
				}
				ArrayList<Double> score = new ArrayList<Double>();
				System.out.print("Creating Haiku");
				for (int i=0;i<=100;i++){
					if (i % 20 == 0)
						System.out.print(".");
					
					Poem res = generator.generate(idea, randomizedIdea);
					
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
					candidates.get(i).toString();
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
		Log.d("haikus", "loaded corpus of "+haikus.size()+" haikus");
		return haikus;
	}

}