package orbit.parsers;

import orbit.model.Call;
import orbit.model.CallGraph;
import orbit.model.CallID;
import orbit.model.RCC;
import orbit.util.IntIterator;
import orbit.util.IntStack;
import orbit.util.Log;

import org.apache.log4j.Category;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

public class HProfSolver
	implements Solver
{
	public static Category LOG = Category.getInstance(HProfSolver.class);

	private List callIDs;
	private List proxyCallIDs;
	
	public CallGraph solve(List callIDs)
	{
		this.callIDs = callIDs;
		this.proxyCallIDs = CallID.getProxyCallIDs(callIDs);

		double[] fractions = solveCallFractions();

		CallGraph cg = new CallGraph(callIDs, fractions);

		computeInclusiveTimes(cg);

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
	private void computeInclusiveTimes(CallGraph cg)
	{
		final double[] fractions = cg.getFractions();
		final CallID[] callIDs = cg.getCallIDs();
		final IntStack traversalStack = new IntStack();
		final double[] inclusiveTimeAdjustments = new double[callIDs.length];
		final RCC[] rccs = new RCC[callIDs.length];

		class ComputeInclusiveTimeVisitor
			implements Call.Visitor
		{
			int maxDepth = 0;

			public int getMaxDepth() { return maxDepth; }
			
			public boolean visit(CallID callID, IntStack callStack)
			{
				Log.debug(LOG, "Visiting ", callID);

				// Store the RCC of the callID
				rccs[callID.getRCC().getKey()] = callID.getRCC();

				if ( callStack.size() > maxDepth )
					maxDepth = callStack.size();
				
				// The RCCs which are generated from sub-stacks of the calls in the profile file
				//   have time=0. There can be quite a few of these...
				if ( callID.getRCC().getExclusiveTime() > 0 )
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
						CallID parentCall = (CallID)callIDs[parentID];
						double adjustment = callID.getRCC().getExclusiveTime() * fraction;
						// System.out.println("Adding " + adjustment + " to " + parentCall.getRCC());
						inclusiveTimeAdjustments[parentCall.getRCC().getKey()] += adjustment;
						// fraction *= fractions[parentID];
					}
				}
				return true;
			}
		}

		ComputeInclusiveTimeVisitor visitor = new ComputeInclusiveTimeVisitor();
		cg.getRoot().depthFirstTraverse(visitor);
		cg.setMaxDepth(visitor.getMaxDepth());

		// Apply the time adjustments to the RCCs
		for ( int i = 0; i < rccs.length; ++i )
		{
			RCC rcc = rccs[i];
			if ( rcc != null )
			{
				rcc.setTime(rcc.getExclusiveTime() + (long)inclusiveTimeAdjustments[i]);
			}
		}
	}
}

