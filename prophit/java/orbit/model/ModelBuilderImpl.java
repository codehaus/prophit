package orbit.model;

import org.apache.log4j.Category;

import orbit.util.Log;

import java.util.*;

class ModelBuilderImpl
	implements ModelBuilder
{
	public static Category LOG = Category.getInstance(ModelBuilderImpl.class);

	private int       nextKey      = 1;
	private int       maxStackSize = 0;
	private TimeData  td           = null;
	private HashMap   stackStrings = new HashMap();
	private HashMap   rccsByStack  = new HashMap();
	private ArrayList rccList      = new ArrayList();
	private ArrayList callIDs      = null;
	
	public void initialize(TimeData td)
	{
		Log.debug(LOG, "Beginning construction with TimeData ", td);

		if ( this.td != null )
		{
			LOG.warn("TimeData for ModelBuilderImpl is already " + this.td);
			return;
		}
		
		this.td = td;
	}

	public ID newStackTrace(String[] stackArray)
	{
		// look up each of the stackArray elements in a String table
		// construct the StackTrace and return an ID that identifies it
		for ( int i = 0; i < stackArray.length; ++i )
		{
			String line = stackArray[i];
			String existing = (String)stackStrings.get(line);
			if ( existing != null )
				line = existing;
			else
				stackStrings.put(line, line);
			stackArray[i] = line;
		}
		StackTrace st = new StackTrace(stackArray);

		Log.debug(LOG, "New stack trace : ", st);
		
		if ( st.size() > maxStackSize )
			maxStackSize = st.size();
		
		return new StackTraceID(st);
	}

	public void newRecordedCall(ID stackTraceID, int nCalls, long time)
	{
		// look up the StackTrace
		// construct a new RCC
		// add it to the list
		if ( !( stackTraceID instanceof StackTraceID ) )
			throw new IllegalArgumentException("stackTraceID " + stackTraceID + " is not a StackTrace");
		StackTraceID stackID = (StackTraceID)stackTraceID;
		StackTrace st = stackID.getStackTrace();

		RCC rcc = (RCC)rccsByStack.get(st);
		if ( rcc == null )
		{
			if ( td == TimeData.Inclusive )
				rcc = new RCC(st, nCalls, time, -1, nextKey++);
			else // td == TimeData.Exclusive
				rcc = new RCC(st, nCalls, -1, time, nextKey++);
			Log.debug(LOG, "New RCC : ", rcc);
			rccsByStack.put(st, rcc);
			rccList.add(rcc);
		}
		else
		{
			Log.debug(LOG, "Adjusting RCC : ", rcc);
			long timeAdust = 0;
			long exclusiveTimeAdjust = 0;
			if ( td == TimeData.Inclusive )
				timeAdust = time;
			else // td == TimeData.Exclusive
				exclusiveTimeAdjust = time;
			rcc.aggregate(nCalls, timeAdust, exclusiveTimeAdjust);
		}
	}

	public void end()
	{
		Log.debug(LOG, "Construction complete");
	}

	public synchronized List getCallIDs()
	{
		if ( callIDs == null )
		{
			Log.debug(LOG, "Constructing call list");
			
			fillInStackTraces();
			constructCallIDs();
			constructCallGraphRoot();
		}
		return callIDs;
	}

	private void constructCallGraphRoot()
	{
		long rootTime = 0;
		ArrayList rootIDs = new ArrayList();
		for ( int i = 0; i < callIDs.size(); ++i )
		{
			CallID id = (CallID)callIDs.get(i);
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
		else if ( rootIDs.size() > 1 )
		{
			
			/*
			 * Construct a new CallID which will represent the root of the CallGraph
			 * Make it the parent of all the root CallIDs
			 */
			RCC rootRCC;
			if ( td == TimeData.Inclusive )
				rootRCC = new RCC(new StackTrace(new String[0]), 1, rootTime, -1, 0);
			else // td == TimeData.Exclusive
				rootRCC = new RCC(new StackTrace(new String[0]), 1, -1, 0, 0);
			
			CallID rootCallID = new CallID(rootRCC, null);
			for ( Iterator i = rootIDs.iterator(); i.hasNext(); )
			{
				CallID callID = (CallID)i.next();
				callID.setParent(rootRCC);
			}
			callIDs.set(rootCallID.getKey(), rootCallID);
		}
	}

	private void fillInStackTraces()
	{
		/*
		 * At each stack depth, if there is no existing stacktrace for a sub-stack of
		 * a stack trace, make a new RCC for the sub-stack and add it to the list
		 */
		HashSet stackTraceSet = new HashSet();
		ArrayList newRCCs = new ArrayList();
		
		// First add the 'natural' stacks
		for ( Iterator i = rccList.iterator(); i.hasNext(); )
		{
			RCC rcc = (RCC)i.next();
			hashStack(stackTraceSet, rcc.getStack());
		}

		// Don't construct stacks of size 1
		for ( int size = maxStackSize; size > 1; --size )
		{
			newRCCs.clear();
			for ( Iterator i = rccList.iterator(); i.hasNext(); )
			{
				RCC rcc = (RCC)i.next();
				if ( rcc.getStack().size() > size )
				{
					StackTrace parentStack = rcc.getParentStack(size);
					if ( parentStack != null &&
						 stackTraceSet.add(parentStack) )
					{
						RCC newRCC = new RCC(parentStack, rcc.getCallCount(), rcc.getTime(), 0, nextKey++);
						Log.debug(LOG, "Adding new rcc " + newRCC);
						newRCCs.add(newRCC);
						hashStack(stackTraceSet, parentStack);
					}
				}
			}
			rccList.addAll(newRCCs);
		}
	}

	private List constructCallIDs()
	{
		/*
		 * In general, the parent ('parent') of a stack trace ('leaf') may only have a sub-set of the
		 *   parent calls that are listed in the leaf.
		 * This algorithm matches each stack trace up with its parent traces in a greedy manner,
		 *   finding all parents which match 'n' calls in the leaf before moving to 'n - 1'.
		 * Each time, the algorithm is only run on the RCCs which are still marked as being 'roots'
		 * Some of these roots may get matched to a parent, the rest will be tried again during
		 *   the next iteration
		 */
		callIDs = new ArrayList();
		ArrayList rootRCCList = new ArrayList(rccList.size());
		rootRCCList.addAll(rccList);
		
		ModelBuilderImpl.ConstructCallsAlgorithm algorithm =
			new ModelBuilderImpl.ConstructCallsAlgorithm(rccList.size());
		
		int maxSize = maxStackSize - 1;
		for ( int size = maxSize; size > 0; )
		{
			// System.out.println("Size : " + size);
			// System.out.println("Looking for " + rootRCCList);
			
			Map rccListByCallee = mapByCallee(rccList, size);
			
			// System.out.println("Callee map " + rccListByCallee);
			
			algorithm.execute(rootRCCList, rccListByCallee, size);
			callIDs = algorithm.getCallIDs();
			
			--size;
			// Don't bother to re-build the root list on the last time through
			if ( size > 0 )
			{
				rootRCCList.clear();
				for ( Iterator j = rccList.iterator(); j.hasNext(); )
				{
					RCC rcc = (RCC)j.next();
					int key = rcc.getKey();
					CallID callID = (CallID)callIDs.get(key);
					if ( callID != null && callID.getParentRCC() == null )
						rootRCCList.add(callID.getRCC());
				}
			}
		}
		return callIDs;
	}
	
	private void hashStack(HashSet set, StackTrace st)
	{
		for ( int size = st.size(); size >= 1; --size )
		{
			set.add(st.getLeafStack(size));
		}
	}
	
	private Map mapByCallee(List rccList, int stackSize)
	{
		HashMap rccListByCallee = new HashMap();
		for ( Iterator i = rccList.iterator(); i.hasNext(); )
		{
			RCC rcc = (RCC)i.next();
			/*
			 * If there are longer stacks in the trace file, then the entire stack
			 * should be matched
			 */
			StackTrace calleeStack = rcc.getLeafStack(stackSize);
			if ( calleeStack != null )
			{
				ArrayList subList = (ArrayList)rccListByCallee.get(calleeStack);
				if ( subList == null )
				{
					subList = new ArrayList();
					rccListByCallee.put(calleeStack, subList);
				}
				subList.add(rcc);
			}
		}
		return rccListByCallee;
	}

	/**
	 * Constructs CallIDs from the RCCs.
	 * Computes the ids for those RCCs which are called ambiguously from multiple
	 * parents.
	 */
	private static class ConstructCallsAlgorithm
	{
		private final ArrayList callIDs;
		private int firstProxyKey = -1;
			
		public ConstructCallsAlgorithm(int numRCCs)
		{
			this.callIDs = new ArrayList(numRCCs + 1);
			for ( int i = 0; i < numRCCs + 1; ++i )
			{
				callIDs.add(i, null);
			}
		}

		public ArrayList getCallIDs()
		{
			return callIDs;
		}
		
		/*
		 * create CallIDs
		 * assign IDs properly for RCC calls and invocation calls
		 */
		public void execute(List rccList, Map rccListByCallee, int stackSize)
		{
			for ( Iterator i = rccList.iterator(); i.hasNext(); )
			{
				RCC rcc = (RCC)i.next();

				List callers = getCallersList(rccListByCallee, rcc, stackSize);
				CallID call = null;
				if ( callers == null )
				{
					call = new CallID(rcc, null);
				}
				else if ( callers.size() == 1 )
				{
					call = new CallID(rcc, (RCC)callers.get(0));
				}
				if ( call != null )
					callIDs.set(call.getKey(), call);
			}

			firstProxyKey = callIDs.size();
			int proxyKey = firstProxyKey;

			for ( Iterator i = rccList.iterator(); i.hasNext(); )
			{
				RCC rcc = (RCC)i.next();
				List callers = getCallersList(rccListByCallee, rcc, stackSize);
				if ( callers != null && callers.size() > 1 )
				{
					callIDs.set(rcc.getKey(), null);
					for ( Iterator j = callers.iterator(); j.hasNext(); )
					{
						RCC caller = (RCC)j.next();
						CallID id = new CallID(rcc, caller, proxyKey++);
						callIDs.add(id);
					}
				}
			}
		}

		private List getCallersList(Map rccListByCallee, RCC rcc, int stackSize)
		{
			List callers = null;
			Log.debug(LOG, "Getting callers for ", rcc);
			if ( rcc.getStack().size() > stackSize )
			{
				StackTrace callerStack = rcc.getParentStack(stackSize);
				callers = (List)rccListByCallee.get(callerStack);
				Log.debug(LOG, "Callers is ", callers);
			}
			else
			{
				Log.debug(LOG, "Stack is too small");
			}
			return callers;
		}
	}
	
	private static class StackTraceID
		implements ID
	{
		private final StackTrace st;

		public StackTraceID(StackTrace st) { this.st = st; }
		
		public int     hashCode()    { return st.hashCode(); }
		public boolean equals(Object other) { return st.equals(other); }
		public String  toString()    { return st.toString(); }

		protected StackTrace getStackTrace() { return st; }
	}
}
