package lp.solve;

public class LP
{
	public final static short CONSTRAINT_TYPE_LE = 0;
	public final static short CONSTRAINT_TYPE_EQ = 1;
	public final static short CONSTRAINT_TYPE_GE = 2;
	public final static short CONSTRAINT_TYPE_OF = 3;

	/* solve status values */
	final static short OPTIMAL = 0;
	final static short MILP_FAIL = 1;
	final static short INFEASIBLE = 2;
	final static short UNBOUNDED = 3;
	final static short FAILURE = 4;
	final static short RUNNING = 5;

	private LPLibrary lp = new LPLibrary();
	private long      hLP = 0;

	public LP(int rows, int columns)
	{
		hLP = lp.make_lp(rows, columns);
		if ( hLP == 0 )
		{
			throw new RuntimeException("Unable to construct LPLibrary with rows =  " + rows + ", cols = " + columns);
		}
	}

	public synchronized void release()
	{
		if ( hLP != 0 )
			lp.delete_lp(hLP);
		hLP = 0;
	}

	public void setObjectiveFunction(double[] row)
	{
		lp.set_obj_fn(hLP(), row);
	}

	/**
	 * @param constraintType see CONSTRAINT_TYPE constaints
	 */
	public void addConstraint(double[] row, short constraintType, double rh)
	{
		lp.add_constraint(hLP(), row, constraintType, rh);
	}

	/**
	 * @param minimize whether to minimize the objective function. If false, the
	 * objective function is maximized.
	 */
	public void setMinimize(boolean minimize)
	{
		if ( minimize )
			lp.set_minim(hLP());
		else
			lp.set_maxim(hLP());
	}

	/**
	 * @return true if the solution was found.
	 */
	public boolean solve()
	{
		int result = lp.solve(hLP());
		System.out.println("solve() returned result " + result);
		return result == OPTIMAL;
	}

	public double getSolutionValue(int column)
	{
		return lp.get_solution_value(hLP(), column);
	}

	public void printLP()
	{
		lp.print_lp(hLP());
	}

	public void printSolution()
	{
		lp.print_solution(hLP());
	}

	protected void finalize()
	{
		release();
	}

	private long hLP()
	{
		if ( hLP == 0 )
			throw new NullPointerException("No LPLibrary allocated in LP");
		return hLP;
	}
}
