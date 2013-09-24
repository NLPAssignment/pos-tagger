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
		//	System.out.println(line);
			String [] parts = line.split("\t");
			
			for(int j = 0 ; j < 7 ; j++ )
			{
				transitionProbabilites[i][j] = Double.parseDouble(parts[j]);
			}
		}
		
		
		/*read output probabilties and store in hashmap*/
		
		String line;
		
		while((line=br.readLine())!=null)
		{
			String parts[] = line.split("\t");
			double[] probabilities = new double[7];
			for(int i  = 0 ; i <7 ; i++ )
			{
				//System.out.print(parts[i+1]+"\t");
				probabilities[i] = Double.parseDouble(parts[i+1]);
					//System.out.print(probabilities[i]+"\t");
			}
			//System.out.println();
			//System.out.println("--"+parts[0]+"--");
			outputProbabilities.put(parts[0], probabilities);
			
		}
		//System.out.println(Double.parseDouble("0.043478260869565216"));
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

		for (String words : outputProbabilities.keySet())
		{
			System.out.print(words+"\t");
			
			for (int i = 0 ; i< outputProbabilities.get(words).length ; i ++)
			{
				System.out.print(outputProbabilities.get(words)[i]+"\t");
			}
			System.out.println();
		}
	}
	
	public String viterbi(String sentence, boolean interactive)
	{
		
		String words []= sentence.split(" "); //split the sentence into words - last word is ? ! .or 
		double [][]probabilityTrellis = new double[5][words.length-1]; //trellis without start and end state
		int [][] tracebackTrellis = new int[5][words.length-1];
		
		//start to initial state on epsilon output
		for (int i = 0 ; i < 5 ; i++ )
		{
			probabilityTrellis[i][0] = transitionProbabilites[0][i+1]; //P(N/^), P(V/^) etc from trained counts
		}
		
		//fill the trellis in column major form
		for(int column = 1 ; column < (words.length - 1) ; column++ )
		{
			for( int row = 0 ; row < 5 ; row++ )
			{
				double max = 0.0;
				int maxPosTag = 1;
				for (int previousColumnRow = 0 ; previousColumnRow < 5 ; previousColumnRow++ )
				{
					double value = probabilityTrellis[previousColumnRow][column-1];
					
					value *= transitionProbabilites[previousColumnRow+1][row+1]; //pos tags are indexed 1 to 5, array indices are 0 to 4
					
					value *= outputProbabilities.get(words[column-1])[previousColumnRow+1];
				
					if(value > max)
					{
						max = value;
						maxPosTag = previousColumnRow+1; //stores pos tag index from 1 to 5 
					}
				}
				probabilityTrellis[row][column] = max;
				tracebackTrellis[row][column] = maxPosTag;
			}
		}
		
		double max = 0.0;
		int lastColumn = words.length-2;
		int maxPosTag = 1;
		for(int row = 0; row < 5 ; row ++)
		{
			double value = probabilityTrellis[row][lastColumn];
			value *= transitionProbabilites[row+1][6]; //transition of last pos tag to end state
			value *= outputProbabilities.get(words[lastColumn])[row+1];
			if(value > max)
			{
				max = value;
				maxPosTag = row+1;
			}
		}

		if(interactive)
		{
			/*print trellis*/
			System.out.println("---Probabiltity trellis---");
			for(int i = 0 ; i < 5 ; i++)
			{
				for  (int j = 0; j < (words.length-1) ; j++)
				{
					System.out.print(probabilityTrellis[i][j]+"\t");
				}
				System.out.println();
			}
			System.out.println("---Traceback trellis---");
			for(int i = 0 ; i < 5 ; i++)
			{
				for  (int j = 0; j < (words.length-1) ; j++)
				{
					System.out.print(tracebackTrellis[i][j]+"\t");
				}
				System.out.println();
			}
		
			System.out.println("max probability:"+max);
			System.out.println("traceback:"+maxPosTag);
		}

		// pos tag
		words[words.length-1] = words[words.length-1]+"_O";
		int currentTag = maxPosTag;
		for(int i = words.length-2 ; i >= 0 ; i--)
		{
			words[i] = words[i]+"_"+getIndexPos(currentTag);
			currentTag = tracebackTrellis[maxPosTag-1][i];
		}

		// Create final string to return and/or print
		String outputSentence = "";
		for(int i = 0 ; i < words.length ; i++)
			outputSentence += words[i]+" ";

		if(interactive)
			System.out.println(outputSentence + "\n");

		return outputSentence;
	}
	
	public static void main(String args[]) throws IOException
	{
		Viterbi v = new Viterbi();
		v.loadProbabilites("model.txt");
		v.viterbi("I need to help .", true);
		//v.printProbabilities();
		//System.out.println(v.outputProbabilities.get("other")[1]);
	}
}
