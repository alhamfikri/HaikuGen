import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

/**
 * Static POS Tagger class
 */
public class HaikuPOSTagger {
	
	private static POSTaggerME tagger = null;
	
	/*
	 * One time call only. Automatically called when using POSTagger for the first time.
	 */
	private static void init() {
		InputStream modelIn = null;
		String currentDirectory = System.getProperty("user.dir");
		
		try {
		  modelIn = new FileInputStream(currentDirectory + "/res/model/en-pos-maxent.bin");
		  POSModel model = new POSModel(modelIn);
		  tagger = new POSTaggerME(model);

		}
		catch (IOException e) {
		  // Model loading failed, handle the error
		  e.printStackTrace();
		}
		finally {
		  if (modelIn != null) {
		    try {
		      modelIn.close();
		    }
		    catch (IOException e) {
		    }
		  }
		}
	}
	
	/**
	 * Tags given string
	 * @param sentence : String of English that wants to be tagged
	 * @return array of string, the tag label of the input for each word
	 */
	public static String[] tag(String sentence) {
		
		//first time initialization
		if (tagger == null) {
			init();
		}
		
		String sent[] = sentence.split(" ");		  
		String tags[] = tagger.tag(sent);
		
		return tags;
	}
	
	/**
	 * Tags given string
	 * @param sentence : Array of string of English words that wants to be tagged
	 * @return array of string, the tag label of the input for each word
	 */
	public static String[] tag(String[] sentence) {
		
		//first time initialization
		if (tagger == null) {
			init();
		}
		
		String tags[] = tagger.tag(sentence);
		
		return tags;
	}
}
