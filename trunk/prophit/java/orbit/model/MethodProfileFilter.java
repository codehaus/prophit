package orbit.model;

import orbit.util.IntIterator;
import orbit.util.IntStack;

/**
 * This Filter selects out only the Calls which are parent invocations of a particular method,
 * or are the method itself. 
 */
public class MethodProfileFilter
	implements Call.Filter
{
	private final Call      root;
	private final String    methodName;
	private final boolean[] callIDs;
		
	public MethodProfileFilter(String methodName, Call call)
	{
		this.root = call;
		this.methodName = methodName;
		
		MethodProfileVisitor visitor = new MethodProfileVisitor();
		call.depthFirstTraverse(visitor);
		
		this.callIDs = visitor.getCallIDs();
	}

	public boolean execute(Call root, Call child)
	{
		int key = child.getKey();
		return key < callIDs.length && callIDs[key];
	}	
	
	/**
	 * Visits the entire sub-tree. Each time the named method occurs as the leaf of the callStack, that
	 * callStack is cloned and saved. Then the {@link #execute} method only returns true if the
	 * callID in question is part of some callStack that was saved by the MethodProfileVisitor.
	 */
	class MethodProfileVisitor
		implements Call.Visitor
	{
		// boolean array elements are always 'false' by default
		private boolean[] callIDs = new boolean[200];

		{
			// Always show at least the root
			markKey(root.getKey());
		}	  
		
		public boolean visit(CallID callID, IntStack callStack)
		{
			String leafMethodName = callID.getRCC().getLeafMethodName();
			if ( leafMethodName != null && leafMethodName.equals(methodName) )
			{
				for ( IntIterator i = callStack.iterator(); i.hasNext(); )
				{
					int key = i.next();
					markKey(key);
				}
			}
			
			return true;
		}

		/**
		 * @return the keys of the CallIDs that are parents of the specified method, or are the method itself.
		 * If array[key] == true then key should be included in the filter.
		 */
		public boolean[] getCallIDs()
		{
			return callIDs;
		}

		private void markKey(int key)
		{
			if ( key >= callIDs.length )
			{
				boolean[] newCallIDs = new boolean[key * 2];
				System.arraycopy(callIDs, 0, newCallIDs, 0, callIDs.length);
				callIDs = newCallIDs;
			}
			callIDs[key] = true;
		}
	}
}
