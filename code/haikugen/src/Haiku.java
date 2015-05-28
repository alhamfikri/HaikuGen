/**
 * @author Alham Fikri Aji
 * 
 * Class to storage Haiku. Records the Haiku's original text, parsed text, and its POS-Tag.
 */
public class Haiku {
	
	private String originalText;
	private String[][] text;
	private String[][] tag;
	private int part;
	
	/**
	 * construct the Haiku based on given text.
	 * 
	 * @param poem 	the haiku. Each line of haiku MUST be separated by % (percent dash) sign. 
	 * 				For example: "Lightning flash -- % what I thought were faces % are plumes of pampas grass ."
	 */
	public Haiku(String poem) {
		originalText = poem;
		String lines[] = originalText.split("&");
		part = lines.length;
		
		text = new String[part][];
		tag = new String[part][];
		
		for (int i=0;i<part;i++) {
			text[i] = lines[i].trim().split(" ");
			//removing extra white-spaces
			for (int j=0;j<text[i].length;j++)
				text[i][j] = text[i][j].replaceAll("\\s+","");
			
			tag[i] = HaikuPOSTagger.tag(text[i]);
		}	
	}
	
	/**
	 * Display the haiku in (hopefully) beautiful format
	 */
	public void print(){
		for (int i=0;i<part;i++) {
			for (int j=0;j<text[i].length;j++) {
				System.out.print(text[i][j]+"/"+tag[i][j]+" ");
			}
			System.out.println();
		}
	}

	/**
	 * Get Haiku's tag
	 * @return array or string of tag divided per-part
	 */
	public String[][] getTag() {
		return tag;
	}
	

}
