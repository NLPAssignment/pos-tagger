package postagger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.ArrayList;
public class ViterbiBNC {
	
	/*
	 * class variables 
	 * transition probabilties for each pair of states
	 * output probabilities for word given a state
	 * 
	 * */
	
	HashMap<String, HashMap<String, Integer>> outputCounts;
	HashMap<String, HashMap<String, Integer>> transitionCounts;
	HashMap<String, Integer> priorStateCounts;

	
	//constructor- initializes data structures to empty
	public ViterbiBNC()
	{

		transitionCounts = new HashMap<String, HashMap<String,Integer>>();
		priorStateCounts = new HashMap<String, Integer>();
		outputCounts = new HashMap<String, HashMap<String,Integer>>();
	}
	

	
	//reads corpus file and builds data structures
	public void loadCounts(String filename) throws IOException
	{
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(filename)));
		try{
		priorStateCounts = (HashMap<String, Integer>)ois.readObject();
		transitionCounts =(HashMap<String, HashMap<String,Integer>>) ois.readObject();
		outputCounts = (HashMap<String, HashMap<String,Integer>>)ois.readObject();
		
		//System.out.println(outputCounts.get("aids").get("NN1"));
		}
		catch(ClassNotFoundException e){}
	}
	
	public void printTransitionCounts()
	{
		System.out.println(transitionCounts.toString());
	}

	public void printOutputCounts()
	{
		System.out.println(outputCounts.toString());		
	}

	public void printPriorStateCounts()
	{
		System.out.println(priorStateCounts.toString());
	}
	
	
	public double getTransitionProbability(String first,String second)
	{
		if(transitionCounts.containsKey(first))
		{
			if(transitionCounts.get(first).containsKey(second))
				return (double)transitionCounts.get(first).get(second) / priorStateCounts.get(first);
			else 
				return 0.0;
		}
		else
		{
			return 0.0;
		}
	}
	
	//returns P(word|state)
	public double getOutputProbability(String word, String posTag)
	{
		if(outputCounts.containsKey(word))
		{
			if(outputCounts.get(word).containsKey(posTag))
			return (double)outputCounts.get(word).get(posTag) / priorStateCounts.get(posTag);
			else 
				return 0.0; /*TODO should this need smoothing? seen word not seen with a particular pos tag*/
		}
		else
		{
			/*
			 * TODO
			 * Smoothing goes here
			 */
			//return 0.0;
			//for now return a standard very small number
			return ( (double) 1 / outputCounts.size() );
		}
	}
	
	public String viterbi(String sentence, boolean interactive)
	{
		// Make it case-insensitive
		sentence = Utilities.uncapitalize(sentence);
		
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
			//System.out.println("---col---"+column);
			for( int row = 0 ; row < 5 ; row++ )
			{
				double max = 0.0;
				int maxPosTag = 1;
				for (int previousColumnRow = 0 ; previousColumnRow < 5 ; previousColumnRow++ )
				{
					double value = probabilityTrellis[previousColumnRow][column-1];
					
					/*
				 	System.out.println("State: "+(row+1));
					System.out.println("Word: "+words[column-1]);
					System.out.println("prev state: "+(previousColumnRow+1));
					System.out.println("Stored val: "+value);
					System.out.println("transition probabiltiy: "+transitionProbabilites[previousColumnRow+1][row+1]);
					System.out.println("output probability: "+getOutputProbability(words[column-1], previousColumnRow+1));
					
					*/
					value *= transitionProbabilites[previousColumnRow+1][row+1]; //pos tags are indexed 1 to 5, array indices are 0 to 4
					
					
					value *= getOutputProbability(words[column-1], previousColumnRow+1);//outputProbabilities.get(words[column-1])[previousColumnRow+1];
				
					if(value > max)
					{
						max = value;
						maxPosTag = previousColumnRow+1; //stores pos tag index from 1 to 5 
						// was changed from previousColumnRow+1
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
			value *= getOutputProbability(words[lastColumn], row+1);//outputProbabilities.get(words[lastColumn])[row+1];
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
	
	public String viterbi(String sentence)
	{
		String taggedSentence="";
		//trellis
		ArrayList<HashMap<String,Node>> trellis  = new ArrayList<HashMap<String,Node>>();
		
		//chunk the sentence into words
		String words [] = sentence.split(" ");
		
		/*initialize first row of trellis*/
		HashMap<String, Node> firstColumn = new HashMap<String, Node>();
		for(String posTag:priorStateCounts.keySet())
		{
			if(posTag.equals("^")){/*ignore since the start state is not in every trellis column*/}
			else
			firstColumn.put(posTag, new Node(getTransitionProbability("^", posTag),"^"));
		}
		trellis.add(firstColumn);

		
		System.out.println(trellis.toString());
		
		//further rows of trellis
		for(int wordIndex = 0 ; wordIndex < (words.length - 1) ; wordIndex++)
		{
			HashMap<String, Node> previousColumn = trellis.get(wordIndex);
			HashMap<String, Node> thisColumn = new HashMap<String, Node>();
			String thisWord = words[wordIndex];
			String lowercaseWord = thisWord.toLowerCase();
			System.out.println(lowercaseWord);
			
			double maxProbability = 0.0;
			String maxPosTag = "";
			for(String currentPosTag:previousColumn.keySet())
			{
				for(String previousPosTag:previousColumn.keySet())
				{
					double value = previousColumn.get(previousPosTag).getProbability();
					value *= getTransitionProbability(previousPosTag, currentPosTag);
					value *= getOutputProbability(lowercaseWord, previousPosTag);
					
					if(value >= maxProbability)
					{
						maxProbability = value;
						maxPosTag = previousPosTag;
					}
				}
				thisColumn.put(currentPosTag, new Node(maxProbability , maxPosTag));
			} // column done
			trellis.add(thisColumn);
		} //entire trellis done
		
		
		System.out.println(trellis.toString());
		
		
		
		/*find max sequence and pos tag*/
		HashMap<String, Node> lastColumn = trellis.get((words.length - 1));
		String lastWord = words[words.length -1];
		String lowercaseLastWord = lastWord.toLowerCase();
		
		double maxProbability = 0.0;
		String maxPosTag = "";
		for(String previousPosTag:lastColumn.keySet())
		{
			double value = lastColumn.get(previousPosTag).getProbability();
			value *= getTransitionProbability(previousPosTag, ".");
			value *= getOutputProbability(lowercaseLastWord, previousPosTag);
			
			if(value >= maxProbability)
			{
				maxProbability = value;
				maxPosTag = previousPosTag;
			}
		}
		String posTag = maxPosTag;
		for(int wordIndex = (words.length -1 ); wordIndex >= 0 ; wordIndex --)
		{
			words[wordIndex] = posTag+"_"+words[wordIndex];
			System.out.println("wordIndex = "+wordIndex);
			posTag = trellis.get(wordIndex).get(posTag).getBackpointer();
			System.out.println(trellis.size());
			
		}
		for(int i = 0 ; i < words.length ; i++)
		{
			taggedSentence += words[i]+" ";
		}
		return taggedSentence;
	}
	
	public static void main(String args[]) throws IOException
	{
		ViterbiBNC v = new ViterbiBNC();
		v.loadCounts("model_BNC.txt");
		System.out.println(v.viterbi("People see new things ."));
	}
}
