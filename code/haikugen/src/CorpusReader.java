import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


public class CorpusReader {
	/**
	 * Reads brown corpus
	 * @param filepath: location of the corpus
	 * @return ArrayList of sentence, each contains ArrayList of word
	 */
	public static ArrayList<ArrayList<String>> readBrown(String filepath){
		ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
		
		String currentDirectory = System.getProperty("user.dir");
		String line = null;
		try {
		  BufferedReader br = new BufferedReader(new FileReader(currentDirectory + "/res/brown/" + filepath));
	         while ((line = br.readLine()) != null) {
	        	ArrayList<String> sentence = new ArrayList<String>();
	            String tokens[] = line.trim().toLowerCase().split(" ");
	            if (tokens.length == 0) 
	            	continue;
	            
	            //remove the tagset, create cleaned corpus and update the wordlist
	            for (int i=0;i<tokens.length;i++) {
	            	String word = tokens[i].split("/")[0];
	            	
	            	if (word.length() > 0)
	            		sentence.add(word);
	            }
	            if (sentence.size() > 0)
	            	data.add(sentence);
	         }
		} catch (IOException e) {
			  // Model loading failed, handle the error
			  e.printStackTrace();
			  return null;
			}
	return data;	
	}
	
	public static ArrayList<ArrayList<String>> readWikipedia(String filepath) {
		ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
		String currentDirectory = System.getProperty("user.dir");
		String line = null;
		try {
		  BufferedReader br = new BufferedReader(new FileReader(currentDirectory + "/res/wiki/raw.en/" + filepath));
	         while ((line = br.readLine()) != null) {
	        	ArrayList<String> sentence = new ArrayList<String>();
	        	//blank space or XML code: ignore
	        	if (line.length() == 0 || (line.charAt(0) == '<' && line.charAt(line.length() - 1) == '>' ))
	        		continue;
	        	
	        	//replace non-alphanumeric char into space
	        	line = line.replaceAll("[^A-Za-z0-9 ]", " ");
	        	line = line.trim();
	        	//end of document: ignore
	        	if (line.equalsIgnoreCase("ENDOFARTICLE"))
	        		continue;
	        	
	            String tokens[] = line.trim().toLowerCase().split(" ");
	            if (tokens.length == 0) 
	            	continue;
	            
	            //remove the tagset, create cleaned corpus and update the wordlist
	            for (int i=0;i<tokens.length;i++) {
	            	String word = tokens[i].trim();
	            	
	            	if (word.length() > 0)
	            		sentence.add(word);
	            }
	            if (sentence.size() > 0)
	            	data.add(sentence);
	           
	        
	         }
		} catch (IOException e) {
			  // Model loading failed, handle the error
			  e.printStackTrace();
			  return null;
			}
		return data;	
	
	}
}
