import java.util.ArrayList;


public class WordVector {

	public static double euclidian(ArrayList<Double> v1, ArrayList<Double> v2) {
		int N = v1.size();
		double res = 0.0;
		for (int i =0;i<N;i++){
			double dist = v1.get(i) - v2.get(i);
			res += dist*dist;
		}
		
		return Math.sqrt(res);
	}
	
	public static ArrayList<Double> subtract(ArrayList<Double> v1, ArrayList<Double> v2) {
		ArrayList<Double> res = new ArrayList<Double>();
		int N = v1.size();
		for (int i =0;i<N;i++){
			res.add(v1.get(i) - v2.get(i));
		}
		return res;
	}
	
	public static ArrayList<Double> add(ArrayList<Double> v1, ArrayList<Double> v2) {
		ArrayList<Double> res = new ArrayList<Double>();
		int N = v1.size();
		for (int i =0;i<N;i++){
			res.add(v1.get(i) + v2.get(i));
		}
		return res;
	}

	public static double dotProduct(ArrayList<Double> v1, ArrayList<Double> v2) {
		int N = v1.size();
		double res = 0.0;
		for (int i =0;i<N;i++){
			double dist = v1.get(i) * v2.get(i);
			res += dist;
		}
		
		return res;
	}
	
	public static double cosineSimilarity(ArrayList<Double> v1,
			ArrayList<Double> v2) {
		
		double dot = dotProduct(v1, v2);
		double len1 = Math.sqrt(dotProduct(v1, v1));
		double len2 = Math.sqrt(dotProduct(v2, v2)); 
		
		return dot/(len1*len2);
	}

}
