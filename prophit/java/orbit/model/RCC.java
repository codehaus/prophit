package orbit.model;

// 'RCC' is a 'recorded caller/callee', read from the profile data file
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

	
