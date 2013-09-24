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
	{
		return -1.0;
	}

	/* findAccuracy(): Reports the overall accuracy upon 5-fold cross-validation */
	public static double findAccuracy()
	{
		double accuracySum = 0.0;

		for(int i=0; i<5; i++)
			accuracySum += CrossValidation.crossValidate(i);

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

	public static void main(String ar[])
	throws IOException
	{
		if(! checkFiles("model"))
		{
			if(!(checkFiles("Train-Fold") && checkFiles("Test-Fold")))
				createCorpusFiles();
			createModelFiles();
		}

		if(!(checkFiles("Train-Fold") && checkFiles("Test-Fold")))
			createCorpusFiles();

		System.out.println("In progress...");
	}
}
