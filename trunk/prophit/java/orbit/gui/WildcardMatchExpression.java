package orbit.gui;

public class WildcardMatchExpression
{
	private static final boolean debug = false;

	private final char[] expressionChars;
	
	public WildcardMatchExpression(String searchExpression)
	{
		if ( searchExpression == null || searchExpression.length() == 0 )
			throw new IllegalArgumentException("WildcardMatchExpression must have at least 1 character");
		expressionChars = searchExpression.toCharArray();
	}

	public boolean match(String name)
	{
		return match(name, 0, 0);
	}
	
	/*
	 * Accepts patterns containing the characters '*', '?' as special characters, and any other characters
	 *   matching a literal character
	 * Algorithm terminates with 'true' when:
	 *   match() is invoked with '*' as the last expression character and the entire string has been consumed
	 *   all expression and string characters have been consumed
	 * Algorithm terminates with 'false' when:
	 *   match() is invoked with an expression character and the entire string has been consumed
	 */
	private boolean match(String name, int nameIndex, int expressionIndex)
	{
		if ( debug )
		{
			for ( int i = 0; i < expressionIndex; ++i ) System.out.print(" ");
			System.out.print(nameIndex + ", " + expressionIndex);
		}
		
		if ( nameIndex == name.length() &&
			 ( expressionIndex == expressionChars.length ||
			   ( expressionIndex == expressionChars.length - 1 && expressionChars[expressionIndex] == '*' ) ) )
		{
			if ( debug ) System.out.println(" -> match");
			return true;
		}
		else if ( nameIndex == name.length() || expressionIndex == expressionChars.length )
		{
			if ( debug ) System.out.println(" -> no match");
			return false;
		}
		
		char nameChar = name.charAt(nameIndex);
		char expressionChar = expressionChars[expressionIndex];

		if ( debug ) System.out.println(" [" + nameChar + ", " + expressionChar + "]");
		
		switch ( expressionChar )
		{
		// * can match by:
		//   matching any character and matching the same expression character against the next name character
		//   not matching this character, but matching the next expression character against the next name character
		case '*':
			if ( match(name, nameIndex + 1, expressionIndex) )
				return true;
			else if ( match(name, nameIndex, expressionIndex + 1) )
				return true;
			else
			{
				if ( debug ) System.out.println(" -> no match");
				return false;
			}
			
		// ? can match by:
		//   matching any character and matching the next expression character against the next name character
		case '?':
			if ( match(name, nameIndex + 1, expressionIndex + 1) )
				return true;
			else
			{
				if ( debug ) System.out.println(" -> no match");
				return false;
			}

		default:
			return nameChar == expressionChar &&
				match(name, nameIndex + 1, expressionIndex + 1 );
		}
	}
}
	
