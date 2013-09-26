package postagger;

public class Utilities
{
	/* Accepts a string and uncapitalizes its first character */
	public static String uncapitalize(String input)
	{
		char[] inputArray = input.toCharArray();
		inputArray[0] = Character.toLowerCase(inputArray[0]);
		return (new String(inputArray));
	}

	/* Maps a pos tag to respective index */
	public static int getPosIndex(char tag)
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
	public static char getIndexPos(int index)
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

	public static void main(String[] ar)
	{
		System.out.println(uncapitalize("Hello"));
	}
}
