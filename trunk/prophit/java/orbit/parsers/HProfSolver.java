package orbit.parsers;

import orbit.model.CallID;
import orbit.model.CallGraph;
import orbit.model.RCC;

import java.util.Iterator;
import java.util.List;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class HProfSolver
	implements Solver
{
	private List callIDs;
	private List proxyCallIDs;
	
	public CallGraph solve(List callIDs)
	{
		this.callIDs = callIDs;
		this.proxyCallIDs = CallID.getProxyCallIDs(callIDs);

		double[] fractions = solveCallFractions();
		computeInclusiveTimes();

		return new CallGraph(callIDs, fractions);
	}

	/**
	 * Always returns null. HProfSolver does not bother to cache because the graph is cheap to reconstruct.
	 */
	public CallGraph readFromCache(List callIDs, Reader r) throws IOException
	{
		return null;
	}
	
	/**
	 * Does nothing. HProfSolver does not bother to cache because the graph is cheap to reconstruct.
	 */
	public void writeToCache(Writer w)
	{
	}

	/**
	 * Each proxy of an RCC is assigned a call-fraction according to its parent's percentage of the total number
	 * of calls across all the proxies of that RCC.
	 */
	private double[] solveCallFractions()
	{
		int[] totalCallsByRCC = new int[callIDs.size()];
		for ( Iterator i = proxyCallIDs.iterator(); i.hasNext(); )
		{
			CallID callID = (CallID)i.next();
			RCC parent = callID.getParentRCC();
			// Parent should always be non-null for a proxy
			totalCallsByRCC[callID.getRCC().getKey()] += parent.getCallCount();
		}

		double[] fractions = new double[callIDs.size()];
		for ( int i = 0; i < fractions.length; ++i )
			fractions[i] = 1.0;

		for ( Iterator i = proxyCallIDs.iterator(); i.hasNext(); )
		{
			CallID callID = (CallID)i.next();
			RCC parent = callID.getParentRCC();
			fractions[callID.getKey()] = parent.getCallCount() / (double)totalCallsByRCC[callID.getRCC().getKey()];
		}
		
		return fractions;
	}

	/**
	 * The inclusive time of each RCC needs to be computed by summing the exclusive times of itself and all its children.
	 */
	private void computeInclusiveTimes()
	{
		// TODO (for now)
	}
}
