package orbit.model;

import orbit.util.IntStack;
import orbit.util.IntIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CallGraph
	implements Call
{
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	
	private final CallID[] callIDs;
	private final int[] parentRCCKeys;
	private final IntStack[] childRCCKeys;
	private final double[] callFractions;

	public CallGraph(List callIDs, double[] callFractions)
	{
		this((CallID[])callIDs.toArray(new CallID[0]), callFractions);
	}

	public CallGraph(CallID[] callIDs, double[] callFractions)
	{
		this.callIDs = callIDs;
		this.callFractions = callFractions;
		
		parentRCCKeys = new int[this.callIDs.length];
		childRCCKeys = new IntStack[this.callFractions.length];
		for ( int i = 0; i < this.callIDs.length; ++i )
		{
			CallID id = this.callIDs[i];
			// Gaps in the callIDs array are filled in later by proxy calls
			if ( id != null )
			{
				int parentKey = id.getParentRCCKey();
				parentRCCKeys[id.getKey()] = parentKey;
				if ( parentKey > 0 )
				{
					if ( childRCCKeys[parentKey] == null )
						childRCCKeys[parentKey] = new IntStack();
					childRCCKeys[parentKey].push(id.getKey());
				}
			}
		}
		for ( int i = 0; i < childRCCKeys.length; ++i )
		{
			if ( childRCCKeys[i] != null )
				childRCCKeys[i].trimToSize();
		}
	}

	public int getKey() { return callIDs.length; }
	public String getName() { return "<root>"; }
	public int getDepth() { return 0; }
	public int getCallCount() { return 1; }

	public double getTime()
	{
		double time = 0;
		for ( Iterator i = getChildren().iterator(); i.hasNext(); )
		{
			Call call = (Call)i.next();
			time += call.getTime();
		}
		return time;
	}

	public Call getParent() { return null; }
	
	public List getChildren()
	{
		ArrayList roots = new ArrayList();
		for ( int i = 0; i < parentRCCKeys.length; ++i )
		{
			if ( parentRCCKeys[i] == -1 )
				roots.add(new CallImpl(callIDs[i]));
		}
		return roots;
	}

	public void depthFirstTraverse(Visitor visitor)
	{
		for ( Iterator i = getChildren().iterator(); i.hasNext(); )
		{
			CallImpl root = (CallImpl)i.next();
			depthFirstTraverse(root.id, new IntStack(), visitor);
		}
	}
	
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(getName());sb.append(" { numInvocations : ");sb.append(getCallCount());
		sb.append(", timeMillis : ");sb.append(getTime());sb.append(" }");
		return sb.toString();
	}
		
	public String toString(int maxDepth)
	{
		StringBuffer sb = new StringBuffer();
		for ( Iterator i = getChildren().iterator(); i.hasNext(); )
		{
			CallImpl root = (CallImpl)i.next();
			toString(root, sb, 0, maxDepth);
		}
		return sb.toString();
	}

	private void depthFirstTraverse(CallID id, IntStack stack, Visitor visitor)
	{
		stack.push(id.getKey());
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
		stack.pop();
	}
	
	private void toString(CallImpl call, StringBuffer sb, int depth, int maxDepth)
	{
		if ( maxDepth != -1 && depth > maxDepth )
			return;
		
		for ( int i = 0; i < depth; ++i )
			sb.append("  ");
		sb.append(call.getName());sb.append(" ");sb.append(call.getTime());sb.append(" [");sb.append(call.getParentKey());sb.append("->");sb.append(call.getKey());sb.append("]");
		sb.append(LINE_SEPARATOR);
		for ( Iterator i = call.getChildren().iterator(); i.hasNext(); )
		{
			CallImpl child = (CallImpl)i.next();
			toString(child, sb, depth + 1, maxDepth);
		}
	}

	public interface Visitor
	{
		/**
		 * @return true if the Visitor wants to continue visiting the children of <code>callID</code>.
		 */
		public boolean visit(CallID callID, IntStack callStack);
	}
	
	public class CallImpl
		implements Call
	{
		private final CallID id;
		private final IntStack stack;
		private int callCount = -1;
		private double time = -1;

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

		private CallImpl createChild(CallID id)
		{
			IntStack copy = (IntStack)stack.clone();
			copy.push(id.getKey());
			return new CallImpl(copy, id);
		}

		private Call createParent()
		{
			if ( stack.size() == 1 )
				return CallGraph.this;
			IntStack copy = (IntStack)stack.clone();
			copy.pop();
			return new CallImpl(copy, callIDs[copy.top()]);
		}
		
		public int getDepth()
		{
			return stack.size();
		}

		public String getName()
		{
			return id.getRCC().getLeafMethodName();
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
	
		public List getChildren()
		{
			// System.out.println("Children of " + id);
			ArrayList children = new ArrayList();
			IntStack childKeys = childRCCKeys[getRCCKey()];
			if ( childKeys != null )
			{
				for ( IntIterator i = childKeys.iterator(); i.hasNext(); )
				{
					int childKey = i.next();
					if ( !stack.contains(childKey) )
					{
						// System.out.println("\tAdding child " + callIDs[childKey]);
						children.add(createChild(callIDs[childKey]));
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
		
		public String toString()
		{
			StringBuffer sb = new StringBuffer();
			sb.append(getName());sb.append(" { numInvocations : ");sb.append(getCallCount());
			sb.append(", timeMillis : ");sb.append(getTime());sb.append(" }");
			return sb.toString();
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
}
