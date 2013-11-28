package postagger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
				return (double)transitionCounts.get(first).get(second) / (priorStateCounts.get(first)+transitionCounts.get(first).size());
			else 
				//return 0.0;
				return 1/(priorStateCounts.get(first)+transitionCounts.get(first).size());
		}
		else
		{
			//we never really come here during our cross validation atleast
			return 0.0;
			//return 1/(priorStateCounts.size()+1);
		}
	}

	//returns P(word|state)
	public double getOutputProbability(String word, String posTag)
	{
		
		if(outputCounts.containsKey(word))
		{
			if(outputCounts.get(word).containsKey(posTag))
				return (double)outputCounts.get(word).get(posTag) / (priorStateCounts.get(posTag)+0.01);
			else 
				return (double)0.01/(priorStateCounts.get(posTag)+0.01);
				//return smooth(word, posTag);
				//return 0.0; /*TODO should this need smoothing? seen word not seen with a particular pos tag*/
		}
		else
		{
			/*
			 * TODO
			 * Smoothing goes here
			 */
			//return 0.0;
			//for now return a standard very small number
			//return (double)0.01/(priorStateCounts.get(posTag)+0.01);
		
			return smooth(word, posTag);
		}
		
		
		
	}

	public double getPriorProbability(String tag)
	{
		int sum=0;
		for(String key : priorStateCounts.keySet())
			sum += priorStateCounts.get(key);

		return ((double) priorStateCounts.get(tag) / sum);
	}

	// Implement suffix-based smoothing for the given word and return P(tag | word)
	public double smooth(String word, String tag)
	{
		int LONGEST_SUFFIX_LENGTH =4;// 4;
		double result = -1;
		// 1: Basic "small value" smoothing
		// result = (double) 1 / outputCounts.size();

		// 2: Most common tag approach
		/* if(tag.equals("NN1"))
			result = 1.0;
		else
			result = 0.0; 
		 */
		// 3: Recursive suffix-based smoothing

		// Assume biggest suffix length of 4
		int suffixLength = word.length() > LONGEST_SUFFIX_LENGTH ? LONGEST_SUFFIX_LENGTH : word.length();
		String initialSuffix = word.substring(word.length() - suffixLength);

		result = smoothRecursion(initialSuffix, tag);

		// This is P(tag | suffix). TODO: Apply bayesian inversion here.

		//System.out.println("P(" + word + " | " + tag + ") = " + result);

		return result;
		//return ( (double) 1 / outputCounts.size() );
	}

	public double smoothRecursion(String suffix, String tag)
	{
		double theta = 0.2;	// Assume a value for now

		// Recursive case: MLE of suffix + theta * recursion over smaller suffix
		if(!suffix.equals(""))
		{
			// A: Initial MLE of P(tag | suffix)

			// double MLE = getPriorProbability(tag);	// Placeholder value
			double MLE = 0.0;
			int numerator = 0;
			int denominator = 0;

			// 1: For each key in outputCounts, see if it ends with required suffix
			for(String corpusWord : outputCounts.keySet())
			{
				// 2: If yes, add all its counts to denominator
				if(corpusWord.endsWith(suffix))
				{
					// Add counts to denominator here
					for(String tagKey : outputCounts.get(corpusWord).keySet())
						denominator += outputCounts.get(corpusWord).get(tagKey);

					// 3: Now add count of MATCHING TAG ONLY to numerator
					if(outputCounts.get(corpusWord).containsKey(tag))
						numerator += outputCounts.get(corpusWord).get(tag);
				}
			}
			// 5: Compute MLE = numerator/denominator
			if(denominator != 0)
				MLE = numerator / (double) denominator;
			else
				MLE = 0;

			// B: Make the recursive call
			double recursiveResult = smoothRecursion(suffix.substring(1), tag);

			return (MLE + theta * recursiveResult) / (1 + theta);
		}

		// Base case: Only MLE
		else
		{
			// C: MLE for P(tag) without suffix

			double MLE = getPriorProbability(tag);
			return MLE;
		}
	}

	public String viterbi(String sentence, boolean interactive)
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
			else /*a Node as a probability and a back pointer*/
				firstColumn.put(posTag, new Node(getTransitionProbability("^", posTag),"^"));
		}
		trellis.add(firstColumn);

		if(interactive)
			System.out.println(trellis.toString());

		//further rows of trellis
		for(int wordIndex = 0 ; wordIndex < (words.length - 1) ; wordIndex++)
		{
			HashMap<String, Node> previousColumn = trellis.get(wordIndex);
			HashMap<String, Node> thisColumn = new HashMap<String, Node>();
			String thisWord = words[wordIndex];
			String lowercaseWord = thisWord.toLowerCase();
			if(interactive)
				System.out.println(lowercaseWord);

			
			HashMap<String, Double> outputProbabilites =new HashMap<String, Double>();
			for (String previousPosTag:previousColumn.keySet()) //calculate output probabilities once for each tag and store, used later for efficiency
			{
				outputProbabilites.put(previousPosTag,getOutputProbability(lowercaseWord, previousPosTag));
				//calculate and store output probability of lowerCaseWord and previousPosTag
			}
			for(String currentPosTag:previousColumn.keySet())
			{
				double storedMax = 0.0;
				double maxProbability = 0.0;
				String maxPosTag = "NN1";
				for(String previousPosTag:previousColumn.keySet())
				{
					double value = previousColumn.get(previousPosTag).getProbability();
					//if(getTransitionProbability(previousPosTag, currentPosTag) == 0)System.out.println(previousPosTag+" - - - - -"+currentPosTag);
					value *= getTransitionProbability(previousPosTag, currentPosTag);
					value *= outputProbabilites.get(previousPosTag); //better efficiency
					//earlier the following line repeated function calls and made things inefficient
					//value *=getOutputProbability(lowercaseWord, previousPosTag);

					if(value > maxProbability)
					{
						maxProbability = value;
						maxPosTag = previousPosTag;
					}
				/*	else if (value == maxProbability)
					{
						if((previousColumn.get(previousPosTag).getProbability()) > storedMax)
						{
							maxProbability = value;
							maxPosTag = previousPosTag;
							storedMax = previousColumn.get(previousPosTag).getProbability();
							
						}
					}*/
				}
				thisColumn.put(currentPosTag, new Node(maxProbability , maxPosTag));
			} // column done
			trellis.add(thisColumn);
		} //entire trellis done

		if(interactive)
			System.out.println(trellis.toString());



		/*find max sequence and pos tag*/
		HashMap<String, Node> lastColumn = trellis.get((words.length - 1));
		String lastWord = words[words.length -1];
		String lowercaseLastWord = lastWord.toLowerCase();
		double storedMax = 0.0;
		double maxProbability = 0.0;
		String maxPosTag = "NN1";
		for(String previousPosTag:lastColumn.keySet())
		{
			double value = lastColumn.get(previousPosTag).getProbability();
			value *= getTransitionProbability(previousPosTag, ".");
			value *= getOutputProbability(lowercaseLastWord, previousPosTag);

			if(value > maxProbability)
			{
				maxProbability = value;
				maxPosTag = previousPosTag;
			}
			/*else if (value == maxProbability)
			{
				if((lastColumn.get(previousPosTag).getProbability()) > storedMax)
				{
					maxProbability = value;
					maxPosTag = previousPosTag;
					storedMax = lastColumn.get(previousPosTag).getProbability();
					
				}
			}*/
		}
		String posTag = maxPosTag;
		for(int wordIndex = (words.length -1 ); wordIndex >= 0 ; wordIndex --)
		{
			words[wordIndex] = posTag+"_"+words[wordIndex];
			if(interactive)
				System.out.println("wordIndex = "+wordIndex);
			posTag = trellis.get(wordIndex).get(posTag).getBackpointer();
			if(interactive)
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
		//java.io.Console con = System.console();
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		v.loadCounts("model_BNC_full.txt");
		System.out.println("Enter a sentence to be tagged:");
		String sentence = br.readLine();
		System.out.println(v.viterbi(sentence, true));
	}
}
