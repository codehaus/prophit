package orbit.model;

/**
 * 'RCC' is a 'recorded caller/callee', read from the profile data file.
 * In general, each entry in the profile data file which records the number of calls and
 * the time spent in a particular {@link StackTrace} constitutes an RCC.
 * <p>
 * Each line in a profile data file is converted into exactly one RCC. However, a single RCC may
 * actually be instantiated multiple times in the call graph. Each of these instantiations is represented
 * by a {@link CallID}.
 */
public class RCC
{
	private final StackTrace st;
	private final int    key;

	private int    nCalls;
	private long   time;
	private long   exclusiveTime;

	// TODO: remove this method
	public RCC(StackTrace st, int nCalls, long time, int key)
	{
		this(st, nCalls, time, -1, key);
	}
	
	/**
	 * @param st the stack trace of the RCC as recorded in the data file
	 * @param nCalls number of times that this RCC was invoked.
	 * @param time total time spent in this RCC. May be -1 if unknown, but in that case
	 * exclusiveTime must not be -1.
	 * @param exclusiveTime exclusive time (time not spent in callees) spent in this RCC.
	 * May be -1 if unknown, but in that case exclusiveTime must not be -1.
	 * @param key a number which is unique to this RCC across all the RCCs
	 */
	public RCC(StackTrace st, int nCalls, long time, long exclusiveTime, int key)
	{
		this.st = st;
		this.nCalls = nCalls;
		this.time = time;
		this.exclusiveTime = exclusiveTime;
		this.key = key;
	}

	public StackTrace getStack() { return st; }

	/**
	 * The StackTrace of the <code>size</code>th parent call of this RCC. This stack trace is constructed by removing
	 * <code>size</code> leaf calls from the StackTrace of this RCC.
	 */
	public StackTrace getParentStack(int size) { return st.getParentStack(size); }

	/**
	 * @return the StackTrace constructed by removing the top <code>size</code> calls from the StackTrace of this RCC.
	 */
	public StackTrace getLeafStack(int size) { return st.getLeafStack(size); }

	/**
	 * @return the name of the method which is the parent of the leaf method in the RCC.
	 * @see #getLeafMethodName
	 */
	public String getLeafParentMethodName() { return st.getLeafParentMethod(); }

	/**
	 * @return the name of the leaf (bottom of the call stack) method in the RCC.
	 */
	public String getLeafMethodName() { return st.getLeafMethod(); }
	
	public int    getCallCount() { return nCalls; }
	public long   getTime() { return time; }
	public long   getExclusiveTime() { return exclusiveTime; }
	public int    getKey() { return key; }

	public void setTime(long time) { this.time = time; }
	public void setExclusiveTime(long time) { this.exclusiveTime = time; }

	protected void aggregate(int nCallsAdjust, long timeAdjust, long exclusiveTimeAdjust)
	{
		time += timeAdjust;
		exclusiveTime += exclusiveTimeAdjust;
		nCalls += nCallsAdjust;
	}
	
	public String toString()
	{
		return st.getLeafParentMethod() + " -> " + st.getLeafMethod() + " { nCalls=" + nCalls + ", time=" + time + ", key=" + key + " }";
	}
}

	
