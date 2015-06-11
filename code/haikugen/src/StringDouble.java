
public class StringDouble implements Comparable<StringDouble>{
	public String s;
	public double d;
	StringDouble(String s,double d){
		this.s = s;
		this.d = d;
	}
	
	@Override
	public int compareTo(StringDouble o) {
		if (d < o.d)
			return -1;
		if (d > o.d)
			return 1;
		return 0;
	}
}
