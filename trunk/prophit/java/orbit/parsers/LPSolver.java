package orbit.parsers;

import orbit.model.CallGraph;
import orbit.model.CallID;
import orbit.model.RCC;
import orbit.util.Log;

import org.apache.log4j.Category;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.*;

import lp.solve.LP;

/**
 * Solves a call graph in which the RCCs times are specified as inclusive times. In this case, the
 * distribution of proxy calls across the call graph can be formulated as a linear program.
 */
// See design/lpsolve.txt
public class LPSolver
	implements Solver
{
	public static Category LOG          = Category.getInstance(LPSolver.class);
	public static Category LP_PRINT_LOG = Category.getInstance("lp.print.log");
	
	private static final double MAX_ALLOWED_TIME_SUM = 1.03;

	private int      nextLPID;
	private LPHelper lpHelper;
	private double[] fractions;

	private List callIDs;
	private List proxyCallIDs;
	private Map  proxyCallsByRCC;
	private Set  parentRCCSet;

	/**
	 * not implemented
	 */
	public CallGraph readFromCache(List callIDs, Reader r) throws IOException
	{
		return null;
	}
	
	/**
	 * not implemented
	 */
	public void writeToCache(Writer w)
	{
	}

	public CallGraph solve(List callIDs)
	{
		this.callIDs = callIDs;
		this.proxyCallIDs = CallID.getProxyCallIDs(callIDs);
		this.proxyCallsByRCC = new HashMap();
		this.parentRCCSet = new HashSet();

		for ( Iterator i = proxyCallIDs.iterator(); i.hasNext(); )
		{
			CallID callID = (CallID)i.next();
			parentRCCSet.add(callID.getParentRCC());
			List list = (List)proxyCallsByRCC.get(callID.getRCC());
			if ( list == null )
			{
				list = new ArrayList(3);
				proxyCallsByRCC.put(callID.getRCC(), list);
			}
			list.add(callID);
		}

		Log.debug(LOG, "Solving LP for proxyCallIDs ", proxyCallIDs);
		
		fractions = null;
		double maxTimeSum = 1.0;
		while ( ( fractions = solveCallFractions(maxTimeSum) ) == null )
		{
			maxTimeSum += 0.02;
		}

		CallGraph cg = new CallGraph(callIDs, fractions);

		return cg;
	}

	protected double[] getFractions()
	{
		return fractions;
	}

	/**
	 * @return the call fraction array, or null if a solution could not be found.
	 * @param maxTimeSum Sometimes it is not possible to make the sum of the child times be strictly less than
	 *   or equal to the parent time
	 * This parameter can be varied as a solution is repeatedly attempted
	 */
	// See design/lpsolve.txt
	private double[] solveCallFractions(double maxTimeSum)
	{
		Log.debug(LOG, "Attempting LP solution with maxTimeSum = ", maxTimeSum);

		lpHelper = new LPHelper();
		LP lpSolve = new LP(0, lpHelper.getNumVariables());

		Log.debug(LOG, "Number of proxy calls = ", proxyCallIDs.size());
		Log.debug(LOG, "Number of variables = ", lpHelper.getNumVariables());

		/* 
		 * Minimize the sum of the 'x' variables:
		 * x1 + x2 + ...
		 */
		double[] objective = new double[lpHelper.getRowArraySize()];
		for ( CallIDIterator i = new CallIDIterator(proxyCallIDs); i.hasNext(); )
		{
			CallID callID = i.next();
			lpHelper.setXValue(callID, objective, 1);
		}
		// lpSolve.set_obj_fn(lpIn, objective);
		// lpSolve.set_minim(lpIn);
		lpSolve.setObjectiveFunction(objective);
		lpSolve.setMinimize(true);

		/* 
		 * Add the absolute value constraints 
		 * For each x:
		 *   x1 - f(store1) >= - parentRCC(store1).nCalls / ( parentRCC(store1).nCalls + parentRCC(store2).nCalls )
		 *   x1 + f(store1) >= parentRCC(store1).nCalls / ( parentRCC(store1).nCalls + parentRCC(store2).nCalls )
		 */
		for ( CallIDIterator i = new CallIDIterator(proxyCallIDs); i.hasNext(); )
		{
			CallID callID = i.next();
			double parentNCalls = callID.getParentRCC().getCallCount();
			double totalNCalls = 0;
			for ( CallIDIterator j = new CallIDIterator((List)proxyCallsByRCC.get(callID.getRCC())); j.hasNext(); )
			{
				CallID sibling = j.next();
				totalNCalls += sibling.getParentRCC().getCallCount();
			}
			double ratio = parentNCalls / totalNCalls;

			double[] row;

			Log.debug(LOG, "Absolute value constraints for ", callID);
			Log.debug(LOG, "\tcall ratio = ", ratio);
			
			row = new double[lpHelper.getRowArraySize()];
			lpHelper.setXValue(callID, row, 1);
			lpHelper.setFValue(callID, row, -1);
			lpSolve.addConstraint(row, LP.CONSTRAINT_TYPE_GE, -ratio);

			row = new double[lpHelper.getRowArraySize()];
			lpHelper.setXValue(callID, row, 1);
			lpHelper.setFValue(callID, row, 1);
			lpSolve.addConstraint(row, LP.CONSTRAINT_TYPE_GE, ratio);
		}

		/*
		 * Add the sum of times constraints
		 * For each RCC (store):
		 * f(store1) * rcc(store1).time + f(store2) * rcc(store2).time <= 
		 *   ( parentRCC(store1).time - ( other singular child RCCs of parentRCC ) ) * TIME_FACTOR
		 */
		for ( Iterator i = parentRCCSet.iterator(); i.hasNext(); )
		{
			RCC rcc = (RCC)i.next();
			long time = rcc.getTime();
			// Subtract the time of all the non-proxy children from the parent time
			for ( CallIDIterator j = new CallIDIterator(callIDs); j.hasNext(); )
			{
				CallID child = j.next();
				if ( child != null && !child.isProxy() && child.getParentRCCKey() == rcc.getKey() )
					time -= child.getRCC().getTime();
			}
			if ( time < 0 )
			{
				LOG.warn("Child time adds up to " + -time + " more than parent time for RCC " + rcc);
				time = 0;
			}
			// Find all the proxy CallIDs whose parent is 'rcc' and add them to the constraint
			double[] row = new double[lpHelper.getRowArraySize()];
			for ( CallIDIterator j = new CallIDIterator(proxyCallIDs); j.hasNext(); )
			{
				CallID child = j.next();
				if ( child.getParentRCC() == rcc )
					lpHelper.setFValue(child, row, child.getRCC().getTime());
			}

			Log.debug(LOG, "Sum of time constraints for ", rcc);
			Log.debug(LOG, "\ttime = ", time);
			
			lpSolve.addConstraint(row, LP.CONSTRAINT_TYPE_LE, time * maxTimeSum);
		}

		/* 
		 * Add constraint sum of all fs on each RCC = 1
		 * f(store1) + f(store2) = 1
		 */
		int constraintCount = 0;
		for ( Iterator i = proxyCallsByRCC.values().iterator(); i.hasNext(); )
		{
			List proxyCallIDs = (List)i.next();
			double[] row = new double[lpHelper.getRowArraySize()];
			for ( CallIDIterator j = new CallIDIterator(proxyCallIDs); j.hasNext(); )
			{
				CallID callID = j.next();
				lpHelper.setFValue(callID, row, 1);
			}
			lpSolve.addConstraint(row, LP.CONSTRAINT_TYPE_EQ, 1);
		}
		
		if ( LP_PRINT_LOG.isDebugEnabled() )
		{
			lpSolve.printLP();
		}

		boolean solved = lpSolve.solve();
		if ( solved )
		{
			LOG.info("Optimal solution found for maxTimeSum = " + maxTimeSum);
			if ( LP_PRINT_LOG.isDebugEnabled() )
			{
				lpSolve.printSolution();
			}
			double[] fractions = new double[callIDs.size()];
			for ( int i = 0; i < fractions.length; ++i )
				fractions[i] = 1.0;

			for ( CallIDIterator i = new CallIDIterator(proxyCallIDs); i.hasNext(); )
			{
				CallID callID = i.next();
				fractions[callID.getKey()] = lpHelper.readFSolution(lpSolve, callID);
			}

			lpSolve.release();

			return fractions;
		}
		else
		{
			LOG.info("No optimal solution for maxTimeSum = " + maxTimeSum);

			lpSolve.release();

			return null;
		}		
	}

	static class CallIDIterator
	{
		private final Iterator iterator;

		public CallIDIterator(List list)
		{
			this(list.iterator());
		}

		public CallIDIterator(Iterator i)
		{
			this.iterator = i;
		}

		public CallID next()
		{
			return (CallID)iterator.next();
		}

		public boolean hasNext()
		{
			return iterator.hasNext();
		}
	}

	class LPHelper
	{
		private final CallID[] lpIDToCallID;
		private final int[]    keyToLPID;
		private final int      numLPIDs;

		public LPHelper()
		{
			nextLPID = 0;
			lpIDToCallID = new CallID[callIDs.size()];
			keyToLPID    = new int[callIDs.size()];
			for ( Iterator i = proxyCallIDs.iterator(); i.hasNext(); )
			{
				CallID callID = (CallID)i.next();
				int lpID = nextLPID++;
				lpIDToCallID[lpID] = callID;
				keyToLPID[callID.getKey()] = lpID;
			}			
			numLPIDs = nextLPID;
		}

		public int getNumVariables()
		{
			// an 'f' and an 'x' for each LP ID
			return numLPIDs * 2;
		}

		public int getRowArraySize()
		{
			// LP solver arrays are 1-based for some reason
			return getNumVariables() + 1;
		}

		public void setFValue(CallID callID, double[] row, double value)
		{
			row[lpIDOf(callID) + 1] = value;
		}

		public void setXValue(CallID callID, double[] row, double value)
		{
			row[lpIDOf(callID) + numLPIDs + 1] = value;
		}

		public double readFSolution(LP lp, CallID callID)
		{
			return lp.getSolutionValue(lpIDOf(callID) + 1);
		}

		private CallID callIDOf(int lpID)
		{
			return lpIDToCallID[lpID];
		}

		private int lpIDOf(CallID callID)
		{
			return keyToLPID[callID.getKey()];
		}
	}
}
