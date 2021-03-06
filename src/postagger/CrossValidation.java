package postagger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;

public class CrossValidation
{
	// Class variables
	/* static int countLines;		// Total no of lines in original training file
	static int[][] confMatrix;	// Confusion matrix depicting which tag (1st index) is confused with which tag (2nd index)
	static int[] correctTags;	// No of instances of a particular POS tag assigned correctly
	static int[] assignedTags;	// No of times we predict a particular tag
	static int[] corpusTags;	// No of instances of a particular tag in corpus
	static double[] precision;	// precision[i] = correctTags[i] / assignedTags[i]
	static double[] recall;		// recall[i] = correctTags[i] / corpusTags[i]
	static double[] f;		// F-Measure: f = 2*p*r / (p+r) */

	static final int NUMBER_OF_FOLDS = 5;

	static int countLines;		// Total no of lines in original training file

	// Confusion matrix depicting which tag (1st String) is confused with which tag (2nd String)
	static HashMap <String, HashMap <String, Integer>> confMatrix;

	// No of instances of a particular POS tag assigned correctly
	static HashMap <String, Integer> correctTags;

	// No of times we predict a particular tag
	static HashMap <String, Integer> assignedTags;

	// No of instances of a particular tag in corpus
	static HashMap <String, Integer> corpusTags;

	// precision[i] = correctTags[i] / assignedTags[i]
	static HashMap <String, Double> precision;

	// recall[i] = correctTags[i] / corpusTags[i]
	static HashMap <String, Double> recall;

	// F-Measure: f = 2*p*r / (p+r)
	static HashMap <String, Double> f;
	
	static HashMap<String, HashMap<String, PrintWriter>> errorProbeStreams;

	static
	{
		confMatrix = new HashMap <String, HashMap <String, Integer>>();
		correctTags = new HashMap <String, Integer>();
		assignedTags = new HashMap <String, Integer>();
		corpusTags = new HashMap <String, Integer>();
		precision = new HashMap <String, Double>();
		recall = new HashMap <String, Double>();
		f = new HashMap <String, Double>();
		errorProbeStreams = new HashMap<String, HashMap<String, PrintWriter>>();
	}

	/* createCorpusFiles(): Splits training.txt into 5 pairs of files, 1 for each fold */
	public static void createCorpusFiles()
	throws IOException
	{
		String corpus = "MediumCorpus.txt";
		
		// Get length of training file (Source: http://stackoverflow.com/questions/453018/number-of-lines-in-a-file-in-java)
		LineNumberReader lnr = new LineNumberReader(new BufferedReader (new FileReader(corpus)));
		lnr.skip(Long.MAX_VALUE);
		countLines = lnr.getLineNumber();
		lnr.close();

		for(int foldno = 0; foldno < NUMBER_OF_FOLDS; foldno++)
		{
			int lineno = 0;
			// Open train and test files for current fold here
			BufferedReader trainReader = new BufferedReader(new FileReader(corpus));//SmallCorpus.txt
			PrintWriter trainWriter = new PrintWriter(new FileWriter("Train-Fold" + foldno + ".txt"), true);
			PrintWriter testWriter = new PrintWriter(new FileWriter("Test-Fold" + foldno + ".txt"), true);

			// Write first portion of fold into train file
			for(; lineno < foldno*countLines/NUMBER_OF_FOLDS; lineno++)
			{
				// Read a line from corpus and write it to training file for current fold
				String l = trainReader.readLine();
				trainWriter.println(l);
			}

			for(; lineno < (foldno+1)*countLines/NUMBER_OF_FOLDS; lineno++)
			{
				// Read a line from corpus and write it to test file for current fold
				String l = trainReader.readLine();
				testWriter.println(l);
			}

			for(; lineno < countLines; lineno++)
			{
				// Read a line from corpus and write it to training file for current fold
				String l = trainReader.readLine();
				trainWriter.println(l);
			}

			// Close training and test files for current fold
			trainWriter.close();
			testWriter.close();
			trainReader.close();
		}
		System.out.println("Fold files created...");
	}

	/* createModelFiles(): Creates 5 files containing probability values - 1 for each fold */
	public static void createModelFiles()
	throws IOException
	{
		for(int i=0; i<NUMBER_OF_FOLDS; i++)
		{
			TrainBNC t = new TrainBNC();
			t.readCorpus("Train-Fold" + i + ".txt");
			t.storeProbabilities("model" + i + ".txt");
		}
	}

	/* crossValidate(): Accepts a fold no and reports accuracy for that fold */
	public static double crossValidate(int foldno)
	throws IOException
	{
		int incorrect = 0, total = 0;
		String testLine="", inputLine="", taggedLine="";
		ViterbiBNC v = new ViterbiBNC();
		v.loadCounts("model" + foldno + ".txt");

		BufferedReader testFile = new BufferedReader(new FileReader("Test-Fold" + foldno + ".txt"));

		while( (testLine = testFile.readLine()) != null )
		{
			// testLine = Utilities.uncapitalize(testLine);
			inputLine = stripTags(testLine);
			// System.out.println(inputLine);
			taggedLine = v.viterbi(inputLine, false);
			//System.out.println(taggedLine);
			total += countTags(testLine);
			incorrect += keepCounts(testLine, taggedLine);
		}

		return (total-incorrect) * 100 / (double) total;
		
	}

	/* findAccuracy(): Reports the overall accuracy upon 5-fold cross-validation */
	public static double findAccuracy()
	throws IOException
	{
		double accuracySum = 0.0;

		for(int i=0; i<NUMBER_OF_FOLDS; i++)
		{
			double currentAccuracy = CrossValidation.crossValidate(i);
			System.out.println("Accuracy for fold " + i + " is: " + currentAccuracy);
			accuracySum += currentAccuracy;
		}

		computePRF();

		return accuracySum / 5;
	}

	/* Checks if 5 files with specified prefix exist */
	static boolean checkFiles(String prefix)
	{
		for(int i=0; i<NUMBER_OF_FOLDS; i++)
			if(!new File(prefix + i + ".txt").exists())
				return false;
		return true;
	}

	/* Accepts a line in tagged format and removes all tags from it */
	static String stripTags(String line)
	{	return line.replaceAll("[A-Z0-9\\-]+_", "");	}

	/* Accepts a line and finds no of tags in it */
	static int countTags(String line)
	{
		int tags = 0;
		for(char c : line.toCharArray())
			if(c == '_')
				tags++;
		return tags;
	}

	/* 
	keepCounts(): Accepts the Viterbi-tagged line and Corpus line, and:
	1: Update confusion matrix
	2: Update counts of correctTags, assignedTags, corpusTags
	3: Calculate and return no of errors in tagging
	*/
	static int keepCounts(String cline, String wline)throws FileNotFoundException
	{
		int errors = 0;
		String[] carray = cline.split(" ");
		String[] warray = wline.split(" ");


		for(int i=0; i<carray.length; i++)
		{
			boolean errorOccured = true;		// Keeps track of whether error has occured; used in confusion matrix
			// carray[i] and warray[i] each contain a tagged word in the form TAG_word
			String ctaglist = carray[i].split("_")[0];
			String[] ctags = ctaglist.split("-");	// Handling of ambiguous corpus tags
			String wtag = warray[i].split("_")[0];	// This will never be ambiguous as it is trained on the model that already handles ambiguous tags

			// We have assigned a tag, increment its count in assignedTags
			if(assignedTags.containsKey(wtag))
			{
				int assignedCount = assignedTags.get(wtag);
				assignedTags.put(wtag, assignedCount+1);
			}
			else
				assignedTags.put(wtag, 1);

			if(!Arrays.asList(ctags).contains(wtag))	// Wrong tag!
			{
				errors++;
				
				for (String ctag : ctags)
				{
					if(errorProbeStreams.containsKey(wtag))
					{
						if(errorProbeStreams.get(wtag).containsKey(ctag))
							errorProbeStreams.get(wtag).get(ctag).write(carray[i]+"\t"+warray[i]+"\t"+cline+"\t"+wline+"\n");
						else
						{
							PrintWriter probeWriter = new PrintWriter(new File("errorProbe/"+ctag+"-"+wtag+".err"));
							errorProbeStreams.get(wtag).put(ctag, probeWriter);
							errorProbeStreams.get(wtag).get(ctag).write(carray[i]+"\t"+warray[i]+"\t"+cline+"\t"+wline+"\n");
								}
							
					}
					else
					{
						PrintWriter probeWriter = new PrintWriter(new File("errorProbe/"+ctag+"-"+wtag+".err"));
						HashMap<String, PrintWriter>h = new HashMap<String, PrintWriter>();
						h.put(ctag, probeWriter);
						errorProbeStreams.put(wtag, h);
						
						errorProbeStreams.get(wtag).get(ctag).write(carray[i]+"\t"+warray[i]+"\t"+cline+"\t"+wline+"\n");
					}
				}
				/*LEKHA - The following 4 lines print outputs to help in error analysis
				 * Change tags to probe different combos
				if(Arrays.asList(ctags).contains("AJ0") && Arrays.asList(wtag).contains("AV0")){
					System.out.print(carray[i]+"\t");
					System.out.print(warray[i]+"\t");
					System.out.println(cline);}
				LEKHA - error analysis probe ends*/
			}
			else						// Correct tag!
			{
				errorOccured = false;
				// wtag is correct, increment its count in correctTags
				if(correctTags.containsKey(wtag))
				{
					int correctCount = correctTags.get(wtag);
					correctTags.put(wtag, correctCount+1);
				}
				else
					correctTags.put(wtag, 1);

				// Update confusion matrix with single tag

				HashMap <String, Integer> ctagMap; // Intermediate hashmap
				if(confMatrix.containsKey(wtag))	// Tag is already seen
				{
					ctagMap = confMatrix.get(wtag);	// Get old values
					if(ctagMap.containsKey(wtag))	// "Diagonal entry" exists
					{
						int confCount = ctagMap.get(wtag);
						ctagMap.put(wtag, confCount+1);
					}
					else	// Diagonal entry doesn't exist, add new entry with count of 1
						ctagMap.put(wtag, 1);
				}

				else	// New tag
				{
					ctagMap = new HashMap <String, Integer>();	// Create brand-new inner HashMap
					ctagMap.put(wtag, 1);
				}

				confMatrix.put(wtag, ctagMap);
			}

			for(String ctag : ctags)
			{
				// Tag is seen in corpus, increment its count in corpusTags
				if(corpusTags.containsKey(ctag))	// Corpus tag is seen
				{
					int corpusCount = corpusTags.get(ctag);
					corpusTags.put(ctag, corpusCount+1);
				}
				else
					corpusTags.put(ctag, 1);

				HashMap <String, Integer> ctagMap; // Intermediate hashmap

				if(errorOccured)
				{
					if(confMatrix.containsKey(ctag))	// Corpus tag is already see
					{
						ctagMap = confMatrix.get(ctag);	// Get old values
						if(ctagMap.containsKey(wtag))	// Same confusion has occured before
						{
							int confCount = ctagMap.get(wtag);
							ctagMap.put(wtag, confCount+1);
						}
						else	// New confusion, add new entry with count of 1
							ctagMap.put(wtag, 1);
					}

					else	// New corpus tag
					{
						ctagMap = new HashMap <String, Integer>();	// Create brand-new inner HashMap
						ctagMap.put(wtag, 1);
					}

					confMatrix.put(ctag, ctagMap);
				}
			}
		}

		/* if(cline.charAt(i) == '_')
		{
			// int corpusIndex = Utilities.getPosIndex(cline.charAt(i+1))-1;
			// int taggedIndex = Utilities.getPosIndex(wline.charAt(i+1))-1;	// Bloody indexes
			// confMatrix[corpusIndex][taggedIndex]++;

			if(confMatrix.contains(

			// correctTags is simply made of diagonal entries in conf matrix
			correctTags[taggedIndex] = confMatrix[taggedIndex][taggedIndex];

			assignedTags[taggedIndex]++;
			corpusTags[corpusIndex]++; 
		}

		if(cline.charAt(i) != wline.charAt(i))
			errors++; */
		return errors;
	}

	public static void computePRF()
	{
		for(String tag : corpusTags.keySet())
		{
			double currentP = 0.0;
			double currentR = 0.0;

			if(correctTags.containsKey(tag) && assignedTags.containsKey(tag))
				currentP = correctTags.get(tag) / (double) assignedTags.get(tag);

			if(correctTags.containsKey(tag))
				currentR = correctTags.get(tag) / (double) corpusTags.get(tag);
			double currentF = (2 * currentP * currentR) / (currentP + currentR);

			precision.put(tag, currentP);
			recall.put(tag, currentR);
			f.put(tag, currentF);
		}
	}

	static void printResults()
	{
		for(String tag:errorProbeStreams.keySet())
		{
			for(String tag2:errorProbeStreams.get(tag).keySet())
			{
				errorProbeStreams.get(tag).get(tag2).close();
			}
		}
		System.out.println("\n--- Confusion Matrix ---");
		for(String tag2:confMatrix.keySet())
		{
			System.out.print("\t"+tag2);
		}
		System.out.println();
		for(String tag : confMatrix.keySet())
		{
			System.out.print(tag+"\t");
				for(String tag2 : confMatrix.keySet())
				{
					HashMap <String,Integer> confCounts = confMatrix.get(tag);
					if(confCounts.containsKey(tag2))
						System.out.print(confCounts.get(tag2)+"\t");
					else
						System.out.print("0"+"\t");
				}
		System.out.println();
		}

		System.out.println("\n--- Corpus Tag Counts ---");
		for(String tag : corpusTags.keySet())
			System.out.println(tag + "\t" + corpusTags.get(tag));

		System.out.println("\n--- Assigned Tag Counts ---");
		for(String tag : assignedTags.keySet())
			System.out.println(tag + "\t" + assignedTags.get(tag));

		System.out.println("\n--- Correct Tag Counts ---");
		for(String tag : correctTags.keySet())
			System.out.println(tag + "\t" + correctTags.get(tag));

		System.out.println("\nTag\tP\tR\tF\n");

		for(String tag : corpusTags.keySet())
			System.out.printf("%s\t%.5f\t%.5f\t%.5f\n", tag, precision.get(tag), recall.get(tag), f.get(tag));
	}

	public static void main(String ar[])throws IOException
	{
		//if(! checkFiles("model"))
		//{
			System.out.println("Model files not found, creating them. This may take a few minutes...");
			//if(!(checkFiles("Train-Fold") && checkFiles("Test-Fold")))
				createCorpusFiles();
			createModelFiles();
			System.out.println("Model files created.");
		//}

		System.out.println("The overall accuracy is: " + findAccuracy());
		printResults();

		// System.out.println(countTags("NP0_ACET NN1_Director NP0_Dr NP0_Patrick NP0_Dixon AV0_recently VVD-VVN_told AT0_the AJ0_National NN1_Symposium PRP_on AJ0_Teenage NN1_Sexuality PRP_at NP0_Swanwick PUN_."));

		System.out.println("In progress...");
	}
}
