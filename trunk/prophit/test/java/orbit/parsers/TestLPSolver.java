package orbit.parsers;

import util.*;

import orbit.model.*;
import orbit.parsers.*;

import junit.framework.TestCase;

import java.util.*;
import java.io.*;

public class TestLPSolver
	extends TestCase
{
	public TestLPSolver(String name)
	{
		super(name);
	}

	public void testSolveSimpleProf() throws Exception
	{
		File file = new File(System.getProperty("basedir") + "/data/simple.prof");
		DashProfParser parser = new DashProfParser(new FileReader(file));
		parser.execute();
		List callIDs = parser.getCallIDs();
		LPSolver solver = new LPSolver();
		solver.solve(callIDs);
		double[] fractions = solver.getFractions();

		assertEquals(14, fractions.length);
		assertEquals(1, fractions[0], TestUtil.TOLERANCE);
		assertEquals(1, fractions[1], TestUtil.TOLERANCE);
		assertEquals(0.333333, fractions[10], TestUtil.TOLERANCE);
		assertEquals(0.666667, fractions[11], TestUtil.TOLERANCE);
		/*
		for ( int i = 0; i < fractions.length; ++i )
		{
			System.out.println(i + " = " + fractions[i]);
		}
		*/
	}
}
