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
		ModelBuilder builder = ModelBuilderFactory.newModelBuilder();
		parser.execute(builder);

		HashMap callIDsByKey = new HashMap();
		
		List callIDs = builder.getCallIDs();
		for ( Iterator i = callIDs.iterator(); i.hasNext(); )
		{
			CallID callID = (CallID)i.next();
			if ( callID != null )
				callIDsByKey.put(new Integer(callID.getKey()), callID);
		}

		// Should not be any CallID with key = 0
		assertEquals(callIDsByKey.get(new Integer(0)), null);
		
		LPSolver solver = new LPSolver();
		solver.solve(callIDs);
		double[] fractions = solver.getFractions();

		/*
		for ( int i = 0; i < fractions.length; ++i )
		{
			System.out.println(callIDsByKey.get(new Integer(i)) + ".fraction = " + fractions[i]);
		}
		*/
		
		assertEquals(14, fractions.length);
		for ( int i = 0; i < 10; ++i )
		{
			assertEquals(1, fractions[i], TestUtil.TOLERANCE);
		}
		assertEquals(0.333333, fractions[10], TestUtil.TOLERANCE);
		assertEquals(0.666667, fractions[11], TestUtil.TOLERANCE);
		assertEquals(0.333333, fractions[12], TestUtil.TOLERANCE);
		assertEquals(0.666667, fractions[13], TestUtil.TOLERANCE);
	}
}
