package orbit.model;

import orbit.util.IntStack;
import orbit.util.IntIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * The CallGraph implements the hierarchy of function calls in a profile. The CallGraph and all the calls
 * in the CallGraph implement the {@link Call} interface, which is a simple but complete representation.
 */
public class CallGraph
{
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	
	private final CallID     rootCallID;
	private final CallID[]   callIDs;
	private final int[]      parentRCCKeys;
	private final IntStack[] childRCCKeys;
	private final double[]   callFractions;
	private final CallImpl   rootCall;

	// This is only calculated on demand
	private int maxDepth = -1;

	/**
	 * @see #CallGraph(CallID[], double[])
	 */
	public CallGraph(List callIDs, double[] callFractions)
	{
		this((CallID[])callIDs.toArray(new CallID[0]), callFractions);
	}

	/**
	 * Construct a CallGraph from the complete list of method calls of which it is composed.
	 * @param callFractions As explained in the comments for {@link CallID}, the recorded time for an
	 * RCC may be split among more than one specific location in the call graph. Each of these locations
	 * is represented by a CallID. For each callID, the value <code>callFractions[callID.getKey()]</code>
	 * is the fraction of the RCC time which is allocated to that CallID. The sum of the callFractions across
	 * all CallIDs for a given RCC should equal to 1.
	 */
	public CallGraph(CallID[] callIDs, double[] callFractions)
	{
		if ( callIDs.length == 0 )
			throw new IllegalArgumentException("Must be at least 1 callID");
		if ( callIDs.length != callFractions.length )
			throw new IllegalArgumentException("Length of callIDs and callFractions arrays should be the same");

		// Compute the total time spent by all the top-level functions
		// Look for a unique top-level callID
		long rootTime = 0;
		ArrayList rootIDs = new ArrayList();
		for ( int i = 0; i < callIDs.length; ++i )
		{
			CallID id = callIDs[i];
			//System.out.println("index: " + i);
			//if (id != null) System.out.println("Parent: " + String.valueOf(id.getParentRCCKey()));
			if ( id != null && id.getParentRCCKey() == -1 )
			{
				rootIDs.add(id);
				rootTime += id.getRCC().getTime();
			}
		}

		if ( rootIDs.size() == 0 )
		{
			throw new IllegalArgumentException("Must be at least one root callID");
		}
		else if ( rootIDs.size() == 1 )
		{
			rootCallID = (CallID)rootIDs.get(0);
			this.callIDs = callIDs;
			this.callFractions = callFractions;
		}
		else
		{
			/*
			 * Construct a new CallID which will represent the root of the CallGraph
			 * Make it the parent of all the root CallIDs
			 * Add it to the end of the callIDs and callFractions arrays
			 */
			int numRCCs = callIDs.length + 1;
			this.callIDs = new CallID[numRCCs];
			this.callFractions = new double[numRCCs];

			RCC rootRCC = new RCC(new StackTrace(new String[0]), 1, rootTime, callIDs.length);
			rootCallID = new CallID(rootRCC, null);
			for ( Iterator i = rootIDs.iterator(); i.hasNext(); )
			{
				CallID callID = (CallID)i.next();
				callID.setParent(rootRCC);
			}
			
			System.arraycopy(callIDs, 0, this.callIDs, 0, callIDs.length);
			System.arraycopy(callFractions, 0, this.callFractions, 0, callFractions.length);
			
			this.callIDs[rootCallID.getKey()] = rootCallID;
			this.callFractions[rootCallID.getKey()] = 1;
		}
		rootCall = new CallImpl(rootCallID);
		
		int numRCCs = this.callIDs.length;
		parentRCCKeys = new int[numRCCs];
		childRCCKeys = new IntStack[numRCCs];
		
		for ( int i = 0; i < this.callIDs.length; ++i )
		{
			CallID id = this.callIDs[i];
			// Gaps in the callIDs array are filled in later by proxy calls
			if ( id != null )
			{
				int key = id.getKey();
				int parentKey = id.getParentRCCKey();
				parentRCCKeys[id.getKey()] = parentKey;
				if ( parentKey != -1 )
				{
					if ( childRCCKeys[parentKey] == null )
						childRCCKeys[parentKey] = new IntStack();
					childRCCKeys[parentKey].push(key);
				}
			}
		}
		
		for ( int i = 0; i < childRCCKeys.length; ++i )
		{
			if ( childRCCKeys[i] != null )
				childRCCKeys[i].trimToSize();
		}
	}

	public CallID[] getCallIDs() { return callIDs; }

	public double[] getFractions() { return callFractions; }
	
	public Call getRoot()
	{
		return rootCall;
	}
	
	public void setMaxDepth(int maxDepth)
	{
		if ( this.maxDepth != -1 &&
			 this.maxDepth != maxDepth )
		{
			throw new IllegalArgumentException("CallGraph.maxDepth was already determined to be " + this.maxDepth +
											   ", and now it is being set to " + maxDepth);
		}
		this.maxDepth = maxDepth;
	}

	public int getMaxDepth()
	{
		if ( maxDepth == -1 )
		{
			MaxDepthVisitor visitor = new MaxDepthVisitor(-1);
			getRoot().depthFirstTraverse(visitor);
			maxDepth = visitor.getMaxDepth();
		}
		return maxDepth;
	}

	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("CallGraph [ root = ");sb.append(rootCallID);
		sb.append(", maxDepth = ");sb.append(maxDepth);sb.append(" ]");
		return sb.toString();
	}
		
	public String toString(int maxDepth)
	{
		CallImpl root = (CallImpl)getRoot();
		return root.toString(maxDepth);
	}

	class CallImpl
		implements Call
	{
		protected final CallID id;
		protected final IntStack stack;
		
		private int    callCount = -1;
		private int    maxDepth  = -1;
		private double time      = -1;

		private CallImpl(CallID id)
		{
			this.stack = new IntStack();
			this.stack.push(id.getKey());
			this.id = id;
		}

		private CallImpl(IntStack stack, CallID id)
		{
			this.stack = stack;
			this.id = id;
		}

		public boolean equals(Object other)
		{
			if ( other instanceof CallImpl )
				return equals((CallImpl)other);
			else
				return false;
		}
		
		public boolean equals(CallImpl other)
		{
			if ( other == null )
				return false;
			return stack.equals(other.stack);
		}
		
		public int hashCode()
		{
			return stack.hashCode();
		}

		public int getDepth()
		{
			return stack.size();
		}
		
		public synchronized int getMaxDepth()
		{
			if ( maxDepth == -1 )
			{
				if ( getParent() == null && !hasFilter() )
				{
					maxDepth = CallGraph.this.getMaxDepth();
				}
				else
				{
					MaxDepthVisitor visitor = new MaxDepthVisitor(-1);
					depthFirstTraverse(visitor);
					maxDepth = visitor.getMaxDepth();
				}
			}
			return maxDepth;
		}
		
		public String getName()
		{
			StackTrace st = id.getRCC().getStack();
			if ( st.size() > 0 )
				return id.getRCC().getLeafMethodName();
			else
				return "<root>";
		}

		public synchronized int getCallCount()
		{
			if ( callCount == -1 )
			{
				double multiplier = getMultiplier();
				callCount = (int)Math.round(multiplier * id.getRCC().getCallCount());
			}
			return callCount;
		}
		
		public synchronized double getTime()
		{
			if ( time < 0 )
			{
				double multiplier = getMultiplier();
				time = id.getRCC().getTime() * multiplier;
			}
			return time;
		}
		
		public Call getParent()
		{
			return createParent();
		}

		public Call filter(Filter filter)
		{
			return new FilteredCallImpl(stack, id, this, filter);
		}

		public void depthFirstTraverse(Visitor visitor)
		{
			depthFirstTraverse(id, new IntStack(), visitor);
		}
		
		public List getChildren()
		{
			ArrayList children = new ArrayList();
			IntStack childKeys = childRCCKeys[getRCCKey()];
			if ( childKeys != null )
			{
				for ( IntIterator i = childKeys.iterator(); i.hasNext(); )
				{
					int childKey = i.next();
					if ( !stack.contains(childKey) )
					{

						CallImpl child = createChild(callIDs[childKey]);
						if ( addChild(child) )
						{
							children.add(child);
						}
					}
				}
			}
			return children;
		}

		public int getKey()
		{
			return stack.get(stack.size() - 1);
		}
		
		public int getParentKey()
		{
			if ( stack.size() > 1 )
				return stack.get(stack.size() - 2);
			else
				return -1;
		}
		
		public String toString(int maxDepth)
		{
			StringBuffer sb = new StringBuffer();
			toString(sb, 0, maxDepth);
			return sb.toString();
		}

		public String toString()
		{
			StringBuffer sb = new StringBuffer();
			sb.append(getName());sb.append(" { numInvocations : ");sb.append(getCallCount());
			sb.append(", timeMillis : ");sb.append(getTime());sb.append(", callStack : ");sb.append(stack);sb.append(" }");
			return sb.toString();
		}

		protected boolean hasFilter()
		{
			return true;
		}
		
		protected boolean addChild(CallImpl child)
		{
			return true;
		}
		
		protected boolean doTraverse(CallID id, IntStack stack)
		{
			return true;
		}
		
		protected CallImpl createChild(CallID id)
		{
			IntStack copy = (IntStack)stack.clone();
			copy.push(id.getKey());
			return new CallImpl(copy, id);
		}

		protected Call createParent()
		{
			if ( stack.size() == 1 )
				return null;
			IntStack copy = (IntStack)stack.clone();
			copy.pop();
			return new CallImpl(copy, callIDs[copy.top()]);
		}
		
		private void depthFirstTraverse(CallID id, IntStack stack, Visitor visitor)
		{
			stack.push(id.getKey());
			try
			{
				if ( doTraverse(id, stack) )
				{
					if ( visitor.visit(id, stack) )
					{
						IntStack childKeys = childRCCKeys[id.getRCC().getKey()];
						if ( childKeys != null )
						{
							for ( IntIterator i = childKeys.iterator(); i.hasNext(); )
							{
								int childKey = i.next();
								if ( !stack.contains(childKey) )
								{
									CallID child = callIDs[childKey];
									depthFirstTraverse(child, stack, visitor);
								}
							}
						}
					}
				}
			}
			finally
			{
				stack.pop();
			}
		}
	
		private void toString(StringBuffer sb, int depth, int maxDepth)
		{
			if ( maxDepth != -1 && depth > maxDepth )
				return;
			
			for ( int i = 0; i < depth; ++i )
				sb.append("  ");
			sb.append(getName());sb.append(" ");sb.append(getTime());sb.append(" [");sb.append(getParentKey());sb.append("->");sb.append(getKey());sb.append("]");
			sb.append(LINE_SEPARATOR);
			for ( Iterator i = getChildren().iterator(); i.hasNext(); )
			{
				CallImpl child = (CallImpl)i.next();
				child.toString(sb, depth + 1, maxDepth);
			}
		}
		
		private int getRCCKey()
		{
			return id.getRCC().getKey();
		}		

		private double getMultiplier()
		{
			double multiplier = 1;
			for ( IntIterator i = stack.iterator(); i.hasNext(); )
			{
				int key = i.next();
				multiplier *= callFractions[key];
			}
			return multiplier;
		}
	}

	class FilteredCallImpl
		extends CallImpl
	{
		private final CallImpl root;
		private final Filter   filter;
		
		private FilteredCallImpl(IntStack stack, CallID id, CallImpl root, Filter filter)
		{
			super(stack, id);

			this.root = root;
			this.filter = filter;
		}

		public boolean equals(Object other)
		{
			if ( !( other instanceof FilteredCallImpl ) )
				return false;
			return equals((FilteredCallImpl)other);
		}

		public boolean equals(FilteredCallImpl other)
		{
			if ( !super.equals(other) )
				return false;
			return root.equals(other.root) && filter.equals(other.filter);
		}
		
		protected boolean doTraverse(CallID id, IntStack stack)
		{
			CallImpl call = new CallImpl(stack, id);
			return filter.execute(root, call);
		}
		
		protected boolean hasFilter()
		{
			return false;
		}
		
		protected boolean addChild(CallImpl child)
		{
			return filter.execute(root, child);
		}
		
		protected CallImpl createChild(CallID id)
		{
			IntStack copy = (IntStack)stack.clone();
			copy.push(id.getKey());
			return new FilteredCallImpl(copy, id, root, filter);
		}

		protected Call createParent()
		{
			if ( stack.size() == 1 )
				return null;
			IntStack copy = (IntStack)stack.clone();
			copy.pop();
			/*
			if ( copy.size() < root.getDepth() )
			{
				throw new RuntimeException("It is illegal to get the parent of a root FilteredCallImpl");
			}
			*/
			return new FilteredCallImpl(copy, callIDs[copy.top()], root, filter);
		}
	}
}
