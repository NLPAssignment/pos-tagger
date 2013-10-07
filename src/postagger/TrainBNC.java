package postagger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

public class TrainBNC
{

	// Class variables


	HashMap<String, HashMap<String, Integer>> outputCounts;
	HashMap<String, HashMap<String, Integer>> transitionCounts;
	HashMap<String, Integer> priorStateCounts;



	/* Class constructor */
	public TrainBNC()
	{
		transitionCounts = new HashMap<String, HashMap<String,Integer>>();
		priorStateCounts = new HashMap<String, Integer>();
		outputCounts = new HashMap<String, HashMap<String,Integer>>();
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
	
	public void outputSeen(String tag, String word)
	{
		if(outputCounts.containsKey(word)) //word already seen before
		{
			HashMap<String, Integer> oc = outputCounts.get(word); // get old values
			if(oc.containsKey(tag)) //tag already seen before
			{
				int oldCount = oc.get(tag); //get old value
				oc.put(tag, (oldCount+1));
			}
			else
			{
				oc.put(tag, 1); //add count for tag seen first time
			}
			outputCounts.put(word, oc);
		}
		else //word seen for first time
		{
			HashMap<String, Integer> hash = new HashMap<String, Integer>();
			hash.put(tag, 1);
			outputCounts.put(word, hash);
		}
	}

	public void transitionSeen(String first, String second)
	{

		if(transitionCounts.containsKey(first)) //word already seen before
		{
			HashMap<String, Integer> oc = transitionCounts.get(first); // get old values
			if(oc.containsKey(second)) //tag already seen before
			{
				int oldCount = oc.get(second); //get old value
				oc.put(second, (oldCount+1));
			}
			else
			{
				oc.put(second, 1); //add count for tag seen first time
			}
			transitionCounts.put(first, oc);
		}
		else //word seen for first time
		{
			HashMap<String, Integer> hash = new HashMap<String, Integer>();
			hash.put(second, 1);
			transitionCounts.put(first, hash);
		}
	}

	public void readCorpus(String filename) throws IOException
	{
	/*	File directory = new File(filename);
		if(directory.isDirectory())
			for(File file:directory.listFiles())
			{*/
				BufferedReader br  = new BufferedReader(new FileReader(filename));	

				String line ;
				String previous, current;
				while(( line = br.readLine() ) != null ) //read line by line
				{
					
					previous = "^";
					String words[] = line.split(" "); //chunk into words
					// Process a single line, word-by-word
					for (int i = 0 ; i < words.length ; i++ )
					{
						if(!words[i].contains("_")) 
							break;
						String taggedword[]= words[i].split("_");	// Separate word and tag

						// Get the current tag
						current = taggedword[0];

						transitionSeen(previous, current);
						
						int count = ( priorStateCounts.get(previous) == null ) ? 1 : priorStateCounts.get(previous)+1 ;
						priorStateCounts.put(previous, count);
						
						if(taggedword[1].equals(".") ||taggedword[1].equals("!")||taggedword[1].equals("?"))
						{
							transitionSeen(current, ".");
							int count2 = ( priorStateCounts.get(current) == null ) ? 1 : priorStateCounts.get(current)+1 ;
							priorStateCounts.put(current, count2);
							previous = "^";
						}
						else
						{
							previous = current;
						}

						// Get the tag output
						taggedword[1] = taggedword[1].toLowerCase();
						outputSeen(taggedword[0], taggedword[1]);
					}
				}
			//}
	}




	public void storeProbabilities(String filename)throws IOException
	{
		FileOutputStream fos = new FileOutputStream(new File(filename));
		ObjectOutputStream oos =  new ObjectOutputStream(fos);
		oos.writeObject(priorStateCounts);
		oos.writeObject(transitionCounts);
		oos.writeObject(outputCounts);
		oos.flush();
		oos.close();

		
		
	}


	
	public static void main(String args[]) throws IOException
	{

		TrainBNC t = new TrainBNC();
		t.readCorpus("BNC_Cleaned/FullCorpus-Cleaned.txt");
		t.storeProbabilities("model_BNC_full.txt");
		t.printPriorStateCounts();
		t.printTransitionCounts();
	}
}
