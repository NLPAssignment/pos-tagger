package postagger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class train {
	
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
	
	/*
	^-0
	N-1
	V-2
	A-3
	R-4
	O-5
	.-6
	*/
	int [][] transitionCounts;
	int [] priorStateCounts;
	
	public train()
	{
		transitionCounts = new int [7][7];
		
		for( int i  = 0 ; i < 7 ; i ++ )
		{
			for (int j = 0 ; j < 7 ; j ++ )
			{
				transitionCounts[i][j] = 0;
			}
		}
		
		priorStateCounts = new int [7];
		
		for(int i = 0 ; i < 7 ; i ++ )
			priorStateCounts[i] = 0;
	}
	
	public void printtransitionCounts()
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
				System.out.printf("%.3f \t",((double)transitionCounts[i][j]/priorStateCounts[i]));
			}
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
			
			for (int i = 0 ; i < words.length ; i++ )
			{
				
				String taggedword[]= words[i].split("_");
				current = getPosIndex(taggedword[1].charAt(0));
				if(current==5 && taggedword[0].equals("."))
				{
					current = 6;
				}
				transitionCounts[previous][current]++;
				priorStateCounts[previous]++;
				previous = current;
			}
		}
	}
	
	
	public double getTransitionProbability(int previous, int next)
	{
		return (double)transitionCounts[previous][next] / priorStateCounts[previous];
	}
	
	public static void main(String args[]) throws IOException
	{
		train t = new train();
		t.readCorpus("training.txt");
		t.printtransitionCounts();
	}
}
