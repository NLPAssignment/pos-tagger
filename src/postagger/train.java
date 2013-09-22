package postagger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class train
{
	/*
	^-0
	N-1
	V-2
	A-3
	R-4
	O-5
	.-6
	*/

	// Class variables

	int [][] transitionCounts;
	int [] priorStateCounts;
	HashMap<String, int[]> outputCounts;

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

	/* Class constructor */
	public train()
	{
		// Create & initialize transition probabilities
		transitionCounts = new int [7][7];
		for( int i  = 0 ; i < 7 ; i ++ )
			for (int j = 0 ; j < 7 ; j ++ )
				transitionCounts[i][j] = 0;

		// Create and initialize pos tag counts
		priorStateCounts = new int [7];
		for(int i = 0 ; i < 7 ; i ++ )
			priorStateCounts[i] = 0;

		// Create empty HashMap for output counts
		outputCounts = new HashMap<String, int[]>();
	}

	public void printTransitionCounts()
	{
		System.out.println("---Transition Counts:---");
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
				System.out.print(transitionCounts[i][j]+"\t");
			}
			System.out.println();
		}
		
		System.out.println("---Prior State Counts:---");
		for (int i= 0 ; i < 7 ; i++ )
		{
			System.out.println(getIndexPos(i)+"\t"+priorStateCounts[i]);
		}
		
		System.out.println("---Transition Probabilites:---");
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
				System.out.printf("%.3f\t",((double)transitionCounts[i][j]/priorStateCounts[i]));
			}
			System.out.println();
		}
	}

	public void printOutputCounts()
	{
		System.out.println("---Output Counts:---");
		for(int i = 0 ; i < 7 ; i ++)
		{
			System.out.print("\t" + getIndexPos(i));
		}
		System.out.println();

		for(String word : outputCounts.keySet())
		{
			System.out.print(word + "\t");
			for(int count : outputCounts.get(word))
				System.out.print(count + "\t");
			System.out.println();
		}
		
		System.out.println("---Output Probabilites:---");
		for(int i = 0 ; i < 7 ; i ++)
		{
			System.out.print("\t" + getIndexPos(i));
		}
		System.out.println();

		for(String word : outputCounts.keySet())
		{
			System.out.print(word + "\t");
			int sum = 0;
			for (int count : outputCounts.get(word))
			{
				sum += count;
			}
			for(int count : outputCounts.get(word))
				System.out.printf("%.3f \t", ( (double) count / sum ));
			System.out.println();
		}
		
		
	}
	
	
	public void readCorpus(String filename) throws IOException
	{
		BufferedReader br  = new BufferedReader(new FileReader(new File(filename)));
		String line ;
	
		int previous, current;
		while(( line = br.readLine() ) != null )
		{
			previous = 0;
			String words[] = line.split(" ");

			// Process a single line, word-by-word
			for (int i = 0 ; i < words.length ; i++ )
			{
				String taggedword[]= words[i].split("_");	// Separate word and tag

				// Get the tag transition
				current = getPosIndex(taggedword[1].charAt(0));
				if(current==5 && taggedword[0].equals("."))
				{
					current = 6;
				}
				transitionCounts[previous][current]++;
				priorStateCounts[previous]++;
				previous = current;

				// Get the tag output

				if(!outputCounts.containsKey(taggedword[0]))	// This is a new word. Create the corresponding array
					outputCounts.put(taggedword[0], new int[7]);

				int[] currentOutputCounts = outputCounts.get(taggedword[0]);	// Obtain the values for current word
				currentOutputCounts[current]++;
				outputCounts.put(taggedword[0], currentOutputCounts);
			}
		}
	}
	
	public double getOutputProbability(String word, int state)
	{
		int[] counts = outputCounts.get(word);
		int sum  = 0;
		for(int i = 0 ; i < counts.length ; i ++)
		{
			sum += counts[i];
		}
		return (double)counts[state] / sum ;
	}
	
	public double getTransitionProbability(int previous, int next)
	{
		return (double)transitionCounts[previous][next] / priorStateCounts[previous];
	}
	
	public void storeProbabilites(String filename)throws IOException
	{
		FileWriter writer = new FileWriter(new File(filename));
		
		
		//write transition probabilities to file
		for( int i  = 0 ; i < 7 ; i ++ ) // tab separated transition probabilites 
		{
			for (int j = 0 ; j < 7 ; j++ )
			{
				writer.write(((double)transitionCounts[i][j]/priorStateCounts[i])+"\t");
			}
			writer.write("\n");
		}
		
		//write output probabilities to file
		

		for(String word : outputCounts.keySet())
		{
			writer.write(word + "\t");
			int sum = 0;
			for (int count : outputCounts.get(word))
			{
				sum += count;
			}
			for(int count : outputCounts.get(word))
				writer.write(( (double) count / sum )+"\t");
			writer.write("\n");
		}
		
		writer.flush();
		writer.close();
		
	}
	public static void main(String args[]) throws IOException
	{
		train t = new train();
		t.readCorpus("training.txt");
		t.printTransitionCounts();
		//t.printOutputCounts();
		System.out.println("P(noun to verb) : "+t.getTransitionProbability(1, 2));
		System.out.println("P(guarantee/noun) : "+t.getOutputProbability("guarantee", 1));
		t.storeProbabilites("model.txt");
		
	}
}
