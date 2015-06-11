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

}
