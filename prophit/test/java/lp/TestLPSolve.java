package lp;

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
		int numVariables = 6;
		int numRows = 6;

		solve lpSolve = new solve();
		lprec lpIn = new lprec(numRows, numVariables);
		String s;
		s = "0			 0				1		1		0		 0";
		lpSolve.str_set_obj_fn(lpIn, s);
		lpSolve.set_minim(lpIn);
		double[] rhs  = { -1/3.0, 1/3.0, -2/3.0, 2/3.0, 1.0, 3.0 };
		short[]  test = { constant.GE, constant.GE, constant.GE, constant.GE, constant.EQ, constant.LE };
		String[] str  = {
			"0			 0				1		0	  -1		 0",
			"0			 0				1		0	  1		 0",
			"0			 0				0		1	  0		 -1",
			"0			 0				0		1	  0		 1",
			"0			 0				0		0	  1		 1",
			"0			 0				0		0	  3       3",
		};
    
		int numConstraints = 0;
		for (int i = 0; i < rhs.length; i++ ) 
		{
			numConstraints = lpSolve.str_add_constraint(lpIn, str[i], test[i], rhs[i]);
		}
		
		// lpSolve.write_LP(lpIn, System.out);

		int result = lpSolve.solve(lpIn);
		assertTrue(result == constant.OPTIMAL);

		// lpSolve.print_solution(lpIn);

		double[] expectedSolution = { 0, 0, 0, 0, 1/3.0, 2/3.0 };
		for ( int i = 0; i < expectedSolution.length; ++i )
		{
			assertEquals(expectedSolution[i], lpIn.getBestSolution(numConstraints + i + 1), 0.001);
		}
	}
}
