package lp;

import lp.solve.LP;

import junit.framework.TestCase;

public class TestLPSolve
	extends TestCase
{
	public TestLPSolve(String name)
	{
		super(name);
	}

	/**
	 * Tests solving a representative profile-like problem.
	 * This test is basically designed to test the problem formulation.
	 * see design/lpsolve.txt for the problem formulation
	 */
	public void testSolveProfile()
	{
		int numVariables = 4;
		int numRows = 6;

		LP lpSolve = new LP(numRows, numVariables);
		double[] objective = { 0, 1, 1, 0, 0 };
		lpSolve.setObjectiveFunction(objective);
		lpSolve.setMinimize(true);
		double[] rhs  = { -1/3.0, 1/3.0, -2/3.0, 2/3.0, 1.0, 3.0 };
		short[]  test = { LP.CONSTRAINT_TYPE_GE, LP.CONSTRAINT_TYPE_GE, LP.CONSTRAINT_TYPE_GE, 
								LP.CONSTRAINT_TYPE_GE, LP.CONSTRAINT_TYPE_EQ, LP.CONSTRAINT_TYPE_LE };
		double[][] constraints  = {
			{ 0, 1,		0,	  -1,		 0 },
			{ 0, 1,		0,	  1,		 0 },
			{ 0, 0,		1,	  0,		 -1 },
			{ 0, 0,		1,	  0,		 1 },
			{ 0, 0,		0,	  1,		 1 },
			{ 0, 0,		0,	  3,       3 },
		};
    
		for (int i = 0; i < rhs.length; i++ ) 
		{
			lpSolve.addConstraint(constraints[i], test[i], rhs[i]);
		}
		
		lpSolve.printLP();

		boolean solved = lpSolve.solve();
		assertTrue(solved);

		lpSolve.printSolution();

		double[] expectedSolution = { 0, 0, 1/3.0, 2/3.0 };
		for ( int i = 0; i < expectedSolution.length; ++i )
		{
			assertEquals(expectedSolution[i], lpSolve.getSolutionValue(i + 1), 0.001);
		}
	}
}
