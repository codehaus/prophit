package orbit.model;

/**
 * Filters calls according to their inclusive time as a fraction of the inclusive time of the root call.
 */
public class InclusiveTimeFilter
	implements Call.Filter
{
	private final double threshold;
	
	public InclusiveTimeFilter(double threshold)
	{
		this.threshold = threshold;
	}

	public int hashCode()
	{
		return (int)( threshold * 100 );
	}
	
	public boolean equals(Object other)
	{
		if ( !( other instanceof InclusiveTimeFilter ) )
			return false;
		return equals((InclusiveTimeFilter)other);
	}

	public boolean equals(InclusiveTimeFilter other)
	{
		return other != null &&
			threshold == other.threshold;
	}

	public boolean execute(Call root, Call child)
	{
		return child.getTime() / root.getTime() > threshold;
	}	
}
