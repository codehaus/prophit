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
		Call main = (Call)simpleCG.cg.getChildren().get(0);
		Call test = (Call)main.getChildren().get(0);
		Call DBExec1 = (Call)test.getChildren().get(0);

		assertEquals(main, simpleCG.main);
		assertEquals(test, simpleCG.test);
		assertEquals(DBExec1, simpleCG.DBExec1);
		assertTrue(!DBExec1.equals(simpleCG.DBExec2));

		DBExec1 = (Call)simpleCG.test.getChildren().get(0);
		assertEquals(DBExec1, simpleCG.DBExec1);
	}
}
