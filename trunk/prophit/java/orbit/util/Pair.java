package orbit.util;

/**  
 * Utility class Pair holds 2 object references {@link #first} and {@link #second}. Useful for functions that need to 
 * either return 2 arguments, or need to return a key/value pair.
*/
public class Pair 
{
	/**	The first object in the Pair.
	*/
	public Object	first;

	/**	The second object in the Pair.
	*/
	public Object	second;

	/** Constructs a pair object comprised of first & second.
			@param first - the first object in the pair.
			@param second - the second object in the pair.
	*/
	public Pair( Object first, Object second )
	{
		this.first	= first;
		this.second	= second;
	}

	/**	
	 * Calls equals for each object and returns true if they both match. Handles all null cases.
	 */
	public boolean equals(Object pair)
	{
		if ( this == pair )
		{
			return true;
		}

		if ( pair == null )
		{
			return false;
		}

		try
		{
			Pair rhs= (Pair) pair;

			// short cut - if we match by identity, we are the same.
			if ( first == rhs.first && second == rhs.second )
			{
				return true;
			}

			// check that we don't have null vs. non-null on first

			if ( (first == null || rhs.first == null) && first != rhs.first )
			{
				return false;
			}

			// similarly check that we don't have null vs. non-null on second.

			if ( (second == null || rhs.second == null) && second != rhs.second )
			{
				return false;
			}

			// as we know we don't have a null vs. non-null mismatch we can just check for null or equals

			if( first == null || first.equals(rhs.first) )
			{
				if ( second == null || second.equals(rhs.second) )
				{
					return true;
				}
			}
			return false;
		}
		catch (ClassCastException e) { return false; }
	}

	/** Combination of the hash codes of both objects */
	public int hashCode()
	{
		int f = 0;
		if ( first != null )
		{
			f = first.hashCode();
		}
		int s = 0;
		if ( second != null )
		{
			s = second.hashCode();
		}
		return f + ( s >> 1);
	}

	/** returns '(first.toString(), second.toString())' */
	public String toString()
	{
		return "{ " + first + ", " + second + " }";
	}
}
