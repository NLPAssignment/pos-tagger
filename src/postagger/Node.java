package postagger;

public class Node {
	double probability;
	String backpointer;
	public Node(double p, String b)
	{
		probability = p;
		backpointer = b;
	}
	
	public double getProbability() {
		return probability;
	}
	public void setProbability(double probability) {
		this.probability = probability;
	}
	public String getBackpointer() {
		return backpointer;
	}
	public void setBackpointer(String backpointer) {
		this.backpointer = backpointer;
	}
	
	public String toString()
	{
		return probability+" ; "+backpointer;
	}
	

}
