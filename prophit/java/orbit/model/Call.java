package orbit.model;

import orbit.util.IntStack;

import java.util.List;

/**
 * Represents a particular method invocation in a {@link CallGraph}.
 */
public interface Call
{
	public int getKey();
		
	public String getName();

	public int getDepth();

	/** Get the maximum depth of any call stack which has this call as its root. */
	public int getMaxDepth();

	public int getCallCount();
	
	public double getTime();

	public Call getParent();
	
	public Call filter(Filter filter);
	
	public void depthFirstTraverse(Visitor visitor);
	
	public List getChildren();

	/** Get a string representation of the call */
	public String toString();

	/**
	 * Get a string representation of the call and up to <code>maxDepth</code>
	 * levels of child Calls.
	 */
	public String toString(int maxDepth);
	
	public interface Visitor
	{
		/**
		 * @return true if the Visitor wants to continue visiting the children of <code>callID</code>.
		 */
		public boolean visit(CallID callID, IntStack callStack);
	}
	
	public interface Filter
	{
		/**
		 * @param root the Call which was originally constructed using {@link Call#filter}
		 * @return true if the <code>child</code> should be returned as a child of <code>self</code>
		 */
		public boolean execute(Call root, Call child);
	}
}
