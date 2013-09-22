package postagger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class Viterbi {
	double [][] transitionProbabilites;
	HashMap<String, double[]> outputProbabilities;
	
	public Viterbi()
	{
		transitionProbabilites = new double[7][7];
		outputProbabilities = new HashMap<String, double[]>();
	}
	

	/* Maps a pos tag to respective index */
	public int getPosIndex(char tag)
	{
		switch(tag)
		{
			case 'N': return 1;

			case 'V': return 2;

			case 'A': return 3;

			case 'R': return 4;

			case 'O': return 5;

		}
		return -1;
	}
	
	/* Maps a matrix index to respective pos tag */
	public char getIndexPos(int index)
	{
		switch(index)
		{
			case 0: return '^';
			
			case 1: return 'N';
			
			case 2: return 'V';
				
			case 3: return 'A';
				
			case 4: return 'R';
				
			case 5: return 'O';
			
			case 6: return '.';
		}
		return ' ';
	}

	
	public void loadProbabilites(String filename) throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
		
		/*Read transition probabilties and store in 2 d array*/
		
		for( int i = 0 ; i < 7 ; i++ )
		{
			String line = br.readLine();
			System.out.println(line);
			String [] parts = line.split("\t");
			
			for(int j = 0 ; j < 7 ; j++ )
			{
				transitionProbabilites[i][j] = Double.parseDouble(parts[j]);
			}
		}
		
		
		/*read output probabilties and store in hashmap*/
		
		String line;
		double[] probabilities = new double[7];
		while((line=br.readLine())!=null)
		{
			String parts[] = line.split("\t");
			
			for(int i  = 0 ; i <7 ; i++ )
			{
				probabilities[i] = Double.parseDouble(parts[i]);
			}
			outputProbabilities.put(parts[0], probabilities);
		}
	}
	
	public void printProbabilities()
	{
		System.out.println("---Transition Probabilties:---");
		for(int i = 0 ; i < 7 ; i ++)
		{
			System.out.print("\t" + getIndexPos(i));
		}
		System.out.println();
		for( int i  = 0 ; i < 7 ; i ++ )
		{
			System.out.print(getIndexPos(i)+"\t");
			
			for (int j = 0 ; j < 7 ; j++ )
			{
				System.out.print(transitionProbabilites[i][j]+"\t");
			}
			System.out.println();
		}
		
		
		
		System.out.println("---Output Probabilites:---");
		for(int i = 0 ; i < 7 ; i ++)
		{
			System.out.print("\t" + getIndexPos(i));
		}
		System.out.println();

		for(String word : outputProbabilities.keySet())
		{
			System.out.print(word + "\t");
			for(double count : outputProbabilities.get(word))
				System.out.print(count + "\t");
			System.out.println();
		}
	}
	
	public static void main(String args[]) throws IOException
	{
		Viterbi v = new Viterbi();
		v.loadProbabilites("model.txt");
		v.printProbabilities();
	}
}
