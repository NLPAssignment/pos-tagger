package postagger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.File;

public class CrossValidation
{
	// Class variables
	static int countLines;		// Total no of lines in original training file
	static int[][] confMatrix;	// Confusion matrix depicting which tag (1st index) is confused with which tag (2nd index)
	static int[] correctTags;	// No of instances of a particular POS tag assigned correctly
	static int[] assignedTags;	// No of times we predict a particular tag
	static int[] corpusTags;	// No of instances of a particular tag in corpus
	static double[] precision;	// precision[i] = correctTags[i] / assignedTags[i]
	static double[] recall;		// recall[i] = correctTags[i] / corpusTags[i]
	static double[] f;		// F-Measure: f = 2*p*r / (p+r)

	static
	{
		confMatrix = new int[5][5];
		correctTags = new int[5];
		assignedTags = new int[5];
		corpusTags = new int[5];
		precision = new double[5];
		recall = new double[5];
		f = new double[5];
	}

	/* createCorpusFiles(): Splits training.txt into 5 pairs of files, 1 for each fold */
	public static void createCorpusFiles()
	throws IOException
	{
		// Get length of training file (Source: http://stackoverflow.com/questions/453018/number-of-lines-in-a-file-in-java)
		LineNumberReader lnr = new LineNumberReader(new BufferedReader (new FileReader("training.txt")));
		lnr.skip(Long.MAX_VALUE);
		countLines = lnr.getLineNumber();
		lnr.close();

		for(int foldno = 0; foldno < 5; foldno++)
		{
			int lineno = 0;
			// Open train and test files for current fold here
			BufferedReader trainReader = new BufferedReader(new FileReader("training.txt"));
			PrintWriter trainWriter = new PrintWriter(new FileWriter("Train-Fold" + foldno + ".txt"), true);
			PrintWriter testWriter = new PrintWriter(new FileWriter("Test-Fold" + foldno + ".txt"), true);

			// Write first portion of fold into train file
			for(; lineno < foldno*countLines/5; lineno++)
			{
				// Read a line from training.java and write it to training file for current fold
				String l = trainReader.readLine();
				trainWriter.println(l);
			}

			for(; lineno < (foldno+1)*countLines/5; lineno++)
			{
				// Read a line from training.java and write it to test file for current fold
				String l = trainReader.readLine();
				testWriter.println(l);
			}

			for(; lineno < countLines; lineno++)
			{
				// Read a line from training.java and write it to training file for current fold
				String l = trainReader.readLine();
				trainWriter.println(l);
			}

			// Close training and test files for current fold
			trainWriter.close();
			testWriter.close();
			trainReader.close();
		}
	}

	/* createModelFiles(): Creates 5 files containing probability values - 1 for each fold */
	public static void createModelFiles()
	throws IOException
	{
		for(int i=0; i<5; i++)
		{
			train t = new train();
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
		Viterbi v = new Viterbi();
		v.loadProbabilities("model" + foldno + ".txt");

		BufferedReader testFile = new BufferedReader(new FileReader("Test-Fold" + foldno + ".txt"));

		while( (testLine = testFile.readLine()) != null )
		{
			testLine = Utilities.uncapitalize(testLine);
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

		for(int i=0; i<5; i++)
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
		for(int i=0; i<5; i++)
			if(!new File(prefix + i + ".txt").exists())
				return false;
		return true;
	}

	/* Accepts a line in tagged format and removes all tags from it */
	static String stripTags(String line)
	{	return line.replaceAll("_[NVARO]", "");	}

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
	static int keepCounts(String cline, String wline)
	{
		int errors = 0;
		for(int i=0; i<cline.length(); i++)
		{
			if(cline.charAt(i) == '_')
			{
				int corpusIndex = Utilities.getPosIndex(cline.charAt(i+1))-1;
				int taggedIndex = Utilities.getPosIndex(wline.charAt(i+1))-1;	// Bloody indexes
				confMatrix[corpusIndex][taggedIndex]++;

				// correctTags is simply made of diagonal entries in conf matrix
				correctTags[taggedIndex] = confMatrix[taggedIndex][taggedIndex];

				assignedTags[taggedIndex]++;
				corpusTags[corpusIndex]++; 
			}

			if(cline.charAt(i) != wline.charAt(i))
				errors++;
		}
		return errors;
	}

	public static void computePRF()
	{
		for(int i=0; i<5; i++)
		{
			precision[i] = correctTags[i] / (double) assignedTags[i];
			recall[i] = correctTags[i] / (double) corpusTags[i];
			f[i] = (2 * precision[i] * recall[i]) / (precision[i] + recall[i]);
		}
	}

	static void printResults()
	{
		System.out.println("--- Confusion Matrix ---");
		System.out.print("\t");
		for(int i=0; i<5; i++)
			System.out.print(Utilities.getIndexPos(i+1) + "\t");
		System.out.println();

		for(int i=0; i<5; i++)
		{
			System.out.print(Utilities.getIndexPos(i+1) + "\t");
			for(int j=0; j<5; j++)
				System.out.print(confMatrix[i][j] + "\t");
			System.out.println();
		}

		System.out.println("\n--- Precision ---");
		for(int i=0; i<5; i++)
			System.out.print(precision[i] + "\t");

		System.out.println("\n--- Recall ---");
		for(int i=0; i<5; i++)
			System.out.print(recall[i] + "\t");

		System.out.println("\n--- F-Measure ---");
		for(int i=0; i<5; i++)
			System.out.print(f[i] + "\t");
		System.out.println();
	}

	public static void main(String ar[])throws IOException
	{
		if(! checkFiles("model"))
		{
			if(!(checkFiles("Train-Fold") && checkFiles("Test-Fold")))
				createCorpusFiles();
			createModelFiles();
		}

		System.out.println("The overall accuracy is: " + findAccuracy());
		printResults();

		// System.out.println(countErrors("AIDS_N Immune_A Deficiency_N Syndrome_N is_V a_O condition_N caused_V by_O a_O virus_N called_V HIV_N Immuno_N Deficiency_N Virus_N ._O", "AIDS_N Immune_O Deficiency_N Syndrome_N is_V a_O condition_N caused_V by_O a_O virus_N called_V HIV_N Immuno_N Deficiency_N Virus_V ._O"));

		System.out.println("In progress...");
	}
}
