package orbit.gui;

public class WildcardMatchExpression
{
	private final char[] expressionChars;
	
	public WildcardMatchExpression(String searchExpression)
	{
		expressionChars = searchExpression.toCharArray();
	}

	public boolean match(String name)
	{
		return match(name, 0, 0);
	}

	private boolean match(String name, int nameIndex, int expressionIndex)
	{
		// System.out.print(nameIndex + ", " + expressionIndex);
		
		if ( nameIndex == name.length() && expressionIndex == expressionChars.length )
			return true;
		else if ( expressionIndex == expressionChars.length || nameIndex == name.length() )
			return false;
		
		char nameChar = name.charAt(nameIndex);
		char expressionChar = expressionChars[expressionIndex];

		// System.out.println(" [" + nameChar + ", " + expressionChar + "]");
		
		switch ( expressionChar )
		{
		// * can match by:
		//   matching any character and matching the same expression character against the next name character
		//   not matching this character, but matching the next expression character against the next name character
		case '*':
			if ( nameIndex == name.length() - 1 && expressionIndex == expressionChars.length - 1 )
				return true;
			else if ( match(name, nameIndex + 1, expressionIndex) )
				return true;
			else if ( match(name, nameIndex, expressionIndex + 1) )
				return true;
			else
				return false;
			
		// ? can match by:
		//   matching any character and matching the next expression character against the next name character
		case '?':
			if ( match(name, nameIndex + 1, expressionIndex + 1) )
				return true;
			else
				return false;

		default:
			return nameChar == expressionChar &&
				match(name, nameIndex + 1, expressionIndex + 1 );
		}
	}
}
	
