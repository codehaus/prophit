package orbit.model;

import util.*;

import junit.framework.TestCase;

public class TestCallGraph
	extends TestCase
{
	public TestCallGraph(String name)
	{
		super(name);
	}

	private SimpleCallGraph simpleCG;
	
	public void setUp()
	{
		simpleCG = new SimpleCallGraph();
	}

	public void testSimpleModel() throws Exception
	{
		Call main = (Call)simpleCG.cg.getRoot();
		Call test = (Call)main.getChildren().get(0);
		Call DBExec1 = (Call)test.getChildren().get(0);

		assertEquals(main, simpleCG.main);
		assertEquals(test, simpleCG.test);
		assertEquals(DBExec1, simpleCG.DBExec1);
		assertTrue(!DBExec1.equals(simpleCG.DBExec2));

		DBExec1 = (Call)simpleCG.test.getChildren().get(0);
		assertEquals(DBExec1, simpleCG.DBExec1);
		
		assertEquals(simpleCG.cg.getMaxDepth(), 5);
	}

	public void testFilter() throws Exception
	{
		// Filter out all calls which take less than 10% of the time
		Call.Filter filter;

		// Apply the filter to the main() function (the root function)
		// Filter out everything
		filter = new InclusiveTimeFilter(2.0);
		Call main = simpleCG.main.filter(filter);

		assertEquals(simpleCG.main.filter(filter), simpleCG.main.filter(filter));
		// Would be better if this was able to ascertain that the filtered call is not the same
		//   as the unfiltered call
		assertTrue(simpleCG.main.equals(simpleCG.main.filter(filter)));
		// It works if you call FilteredCallImpl.equals() though
		assertTrue(!simpleCG.main.filter(filter).equals(simpleCG.main));
		assertEquals(main.getChildren().size(), 0);
		assertEquals(main.getParent(), null);

		// Filter out the 2nd DBExec but not the first
		filter = new InclusiveTimeFilter(0.35);
		main = simpleCG.main.filter(filter);
		assertEquals(main.getParent(), null);
		assertEquals(main.getChildren().size(), 2);
		
		Call test = (Call)main.getChildren().get(0);
		assertEquals(test.getName(), "test");
		assertEquals(test.getTime(), 180, 0);
		assertEquals(test.getChildren().size(), 1);
		assertEquals(test.getChildren().get(0), test.getChildren().get(0));
		
		Call DBExec = (Call)test.getChildren().get(0);
		assertEquals(DBExec.getName(), "DBExec");
		assertEquals(DBExec.getTime(), 120, 0);
		assertEquals(DBExec.getChildren().size(), 0);

		assertEquals(DBExec.getParent(), test);
		assertEquals(DBExec.getParent().getParent(), main);
		assertEquals(DBExec.getParent().getParent().getParent(), null);

		/*
		boolean exception = false;
		try
		{
			// Can't get the parent of the root-level filtered Call
			simpleCG.test.filter(filter).getParent();
		}
		catch (RuntimeException x)
		{
			exception = true;
		}
		assertTrue(exception);
		*/
	}
}
