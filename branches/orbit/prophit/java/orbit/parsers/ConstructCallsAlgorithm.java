package orbit.parsers;

import orbit.model.CallID;
import orbit.model.RCC;
import orbit.model.StackTrace;

import java.util.*;
	
/**
 * Constructs CallIDs from the RCCs.
 * Computes the ids for those RCCs which are called ambiguously from multiple
 * parents.
 */
class ConstructCallsAlgorithm
{
	private final ArrayList callIDs;
	private int firstProxyKey = -1;
			
	public ConstructCallsAlgorithm(int numRCCs)
	{
		this.callIDs = new ArrayList(numRCCs);
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
		/*
		if ( rcc.getStack().toString().startsWith("test.HelloList.buildAsStrings") ||
			 rcc.getStack().toString().startsWith("test.HelloList.linkedAndString") )
			System.out.println("Caller stack for " + rcc + " is : " + rcc.getParentStack(stackSize));
		*/
		
		List callers = null;
		if ( rcc.getStack().size() > stackSize )
		{
			StackTrace callerStack = rcc.getParentStack(stackSize);
			callers = (List)rccListByCallee.get(callerStack);
			// System.out.println("Callers is " + callers);
		}
		else
		{
			// System.out.println("stack is to small");
		}
		return callers;
	}
}
