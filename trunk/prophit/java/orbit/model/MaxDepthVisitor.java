package orbit.model;

import orbit.util.IntStack;

import org.apache.log4j.Category;

/**
 * Computes the maximum stack depth of a {@link CallGraph} by visiting all its nodes. The traversal may
 * be specified to only run for a limited amount of time. The clock starts ticking as soon as the MaxDepthVisitor
 * is constructed. If time runs out before the traversal is complete, {@link #getMaxCallDepth} returns -1.
 */
public class MaxDepthVisitor
	implements Call.Visitor
{
	public static Category LOG = Category.getInstance(MaxDepthVisitor.class);

	/**
	 * Only checks the time after this many calls have been visited, to avoid
	 * spending lots of time in System.currentTimeMillis
	 */
	private static final int CALLS_PER_CHECK = 100;

	private int     maxCallDepth = 0;
	private long    callCounter  = 0;
	private boolean completed    = true;

	private final long maxTime;
	private final long startTime;

	/**
	 * @param maxTime maximum amount of time in milliseconds to spend looking for {@link #getMaxDepth}.
	 * -1 means look forever
	 */
	public MaxDepthVisitor(long maxTime)
	{
		this.maxTime = maxTime;
		this.startTime = System.currentTimeMillis();
	}

	/**
	 * @return -1 if the traversal was not completed. Otherwise, the maxCallDepth
	 */
	public int getMaxDepth()
	{
		if ( completed )
		{
			return maxCallDepth;
		}
		else
		{
			return -1;
		}
	}
	
	public boolean visit(CallID callID, IntStack callStack)
	{
		++callCounter;
		long elapsed;
		if ( maxTime != -1 &&
			 ( callCounter % CALLS_PER_CHECK ) == 0 &&
			 ( elapsed = ( System.currentTimeMillis() - startTime ) ) > maxTime )
		{
			LOG.debug("Time expired on MaxDepthVisitor after " + elapsed + " ms");
			completed = false;
			return false;
		}
		if ( callStack.size() > maxCallDepth )
			maxCallDepth = callStack.size();
		return true;
	}
	
}
