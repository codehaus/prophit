package orbit.model;

/**
 * 'RCC' is a 'recorded caller/callee', read from the profile data file.
 * In general, each entry in the profile data file which records the number of calls and
 * the time spent in a particular {@link StackTrace} constitutes an RCC.
 * <p>
 * Each line in a profile data file is converted into exactly one RCC. However, a single RCC may
 * actually represent several different locations in the call graph. For instance, if a function X calls
 * a function Y, and X is called by A and B, then the RCC represented by {X, Y} is present in at least two places
 * in the call graph : once as a child of A, and once as a child of B. If A and B are themselves called from multiple
 * places, then {X, Y} has yet more instantiations in the call graph.
 */
public class RCC
{
	private final StackTrace st;
	private final int    key;

	private int    nCalls;
	private long   time;

	public RCC(StackTrace st, int nCalls, long time, int key)
	{
		this.st = st;
		this.nCalls = nCalls;
		this.time = time;
		this.key = key;
	}

	public StackTrace getStack() { return st; }
	public StackTrace getParentStack(int size) { return st.getParentStack(size); }
	public StackTrace getLeafStack(int size) { return st.getLeafStack(size); }
	public String getLeafParentMethodName() { return st.getLeafParentMethod(); }
	public String getLeafMethodName() { return st.getLeafMethod(); }
	public int    getCallCount() { return nCalls; }
	public long   getTime() { return time; }
	public int    getKey() { return key; }

	public void adjustTime(long delta) { time += delta; }
	public void adjustCalls(int delta) { nCalls += delta; }
	
	public String toString()
	{
		return nCalls + " " + st.getLeafMethod() + " " + st.getLeafParentMethod() + " " + time + " [" + key + "]";
	}
}

	
