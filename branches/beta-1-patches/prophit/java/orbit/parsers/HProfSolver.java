package orbit.parsers;

import orbit.model.CallID;
import orbit.model.CallGraph;
import orbit.model.RCC;
import orbit.util.IntStack;
import orbit.util.IntIterator;

import java.util.Iterator;
import java.util.List;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class HProfSolver
	implements Solver
{
	private static int STACK_THRESHOLD = 6;
	// When solving the call graph, skip times which are less than 0.1 unit in size
	private static double GRAPH_THRESHOLD = 0.01;

	private List callIDs;
	private List proxyCallIDs;
	
	public CallGraph solve(List callIDs)
	{
		this.callIDs = callIDs;
		this.proxyCallIDs = CallID.getProxyCallIDs(callIDs);

		double[] fractions = solveCallFractions();

		CallGraph cg = new CallGraph(callIDs, fractions);

		computeInclusiveTimes(cg, fractions);

		return cg;
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
			// System.out.println(callID.getKey() + " = " + fractions[callID.getKey()]);
		}
		
		return fractions;
	}

	/**
	 * The inclusive time of each RCC needs to be computed by summing the exclusive times of itself and all its children.
	 */
	private void computeInclusiveTimes(CallGraph cg, final double[] fractions)
	{
		final IntStack traversalStack = new IntStack();
		int numRCCs = callIDs.size() - proxyCallIDs.size();
		final double[] inclusiveTimeAdjustments = new double[callIDs.size() - proxyCallIDs.size()];
		final RCC[] rccs = new RCC[numRCCs];

		class ComputeInclusiveTimeVisitor
			implements CallGraph.Visitor
		{
			public boolean visit(CallID callID, IntStack callStack)
			{
				// System.out.println("Visiting " + callID);
				
				// Store the RCC of the callID
				rccs[callID.getRCC().getKey()] = callID.getRCC();

				// The RCCs which are generated from sub-stacks of the calls in the profile file
				//   have time=0. There can be quite a few of these...
				if ( callID.getRCC().getTime() > 0 )
				{
					// Traverse through the parents of the current CallID
					// For each parent, add the fraction-adjusted time of this CallID to
					//   its inclusive time
					traversalStack.clear();
					traversalStack.addAll(callStack);
					double fraction = 1.0; // fractions[callID.getKey()];
					for ( IntIterator i = callStack.iterator(); i.hasNext(); )
					{
						fraction *= fractions[i.next()];
					}
					// Start with the parent of the current stack
					traversalStack.pop();
					while ( !traversalStack.isEmpty() )
					{
						int parentID = traversalStack.pop();
						CallID parentCall = (CallID)callIDs.get(parentID);
						double adjustment = callID.getRCC().getTime() * fraction;
						// System.out.println("Adding " + adjustment + " to " + parentCall.getRCC());
						inclusiveTimeAdjustments[parentCall.getRCC().getKey()] += adjustment;
						// fraction *= fractions[parentID];
					}
				}
				// return callStack.size() > STACK_THRESHOLD && fraction * callID.getRCC().getTime() > GRAPH_THRESHOLD;
				return true;
			}
		}

		cg.depthFirstTraverse(new ComputeInclusiveTimeVisitor());

		// Apply the time adjustments to the RCCs
		for ( int i = 0; i < rccs.length; ++i )
		{
			RCC rcc = rccs[i];
			if ( rcc != null )
			{
				rcc.adjustTime((long)inclusiveTimeAdjustments[i]);
			}
		}
	}
}

