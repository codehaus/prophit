package orbit.parsers;

import orbit.model.*;
import orbit.parsers.TestConstructCallsAlgorithm;
import java.util.*;
import junit.framework.TestCase;

public class TestConstructCallsAlgorithm
	extends TestCase
{
	RCC root;
	RCC init;
	RCC test;
	RCC setup;
	RCC exec;
	// Child of exec
	RCC insert;
	// Deep child of insert
	RCC bufferAppend;
	ArrayList rccList;

	int key = 1;

	public TestConstructCallsAlgorithm(String test)
	{
		super(test);
	}

	private void setupBasicGraph()
	{
		root = new RCC(new StackTrace(new String[]{ "main" }), 1, 100, key++);

		init = new RCC(new StackTrace(new String[]{ "init", "main" }), 1, 50, key++);
		test = new RCC(new StackTrace(new String[]{ "test", "main" }), 1, 50, key++);

		setup = new RCC(new StackTrace(new String[]{ "setup", "init", "main" }), 1, 50, key++);
		exec = new RCC(new StackTrace(new String[]{ "exec", "test", "main" }), 1, 50, key++);

		// Child of 'exec'
		insert = new RCC(new StackTrace(new String[]{ "insert", "exec", "test" }), 10, 40, key++);

		rccList = new ArrayList();
		rccList.add(root);
		rccList.add(init);
		rccList.add(test);
		rccList.add(setup);
		rccList.add(exec);
		rccList.add(insert);
	}

	private void setupGraphWithGaps()
	{
		setupBasicGraph();

		bufferAppend = new RCC(new StackTrace(new String[]{ "bufferAppend", "concat", "insert", }), 100, 25, key++);
		rccList.add(bufferAppend);
	}

	public void testBasicGraph() throws Exception
	{
		setupBasicGraph();
		
		ConstructCallsAlgorithm algorithm = new ConstructCallsAlgorithm(rccList.size());

		HashMap rccListByCallee = new HashMap();

		// Depth of 2
		mapRCCLeaves(rccListByCallee, new RCC[]{ init, test, setup, exec, insert }, 2);

		ArrayList rootRCCList = new ArrayList();
		rootRCCList.addAll(rccList);
		
		algorithm.execute(rootRCCList, rccListByCallee, 2);
		List callIDs = algorithm.getCallIDs();

		// Shouldn't be any proxy calls
		assertEquals(rccList.size() + 1, callIDs.size());
		
		assertNull(root + " should have no parent", ((CallID)callIDs.get(root.getKey())).getParentRCC());
		assertNull(init + " should have no parent", ((CallID)callIDs.get(init.getKey())).getParentRCC());
		assertNull(test + " should have no parent", ((CallID)callIDs.get(test.getKey())).getParentRCC());
		assertEquals(insert + " should have parent = " + exec, exec, ((CallID)callIDs.get(insert.getKey())).getParentRCC());
		assertEquals(setup + " should have parent = " + init, init, ((CallID)callIDs.get(setup.getKey())).getParentRCC());

		rccListByCallee.clear();

		rootRCCList.clear();
		rootRCCList.add(root);
		rootRCCList.add(init);
		rootRCCList.add(test);
		mapRCCLeaves(rccListByCallee, new RCC[]{ root }, 1);

		algorithm.execute(rootRCCList, rccListByCallee, 1);
		callIDs = algorithm.getCallIDs();

		assertEquals(rccList.size() + 1, callIDs.size());
		assertNull(root + " should have no parent", ((CallID)callIDs.get(root.getKey())).getParentRCC());
		assertEquals(init + " should have parent = " + root, root, ((CallID)callIDs.get(init.getKey())).getParentRCC());
		assertEquals(test + " should have parent = " + root, root, ((CallID)callIDs.get(test.getKey())).getParentRCC());
		assertEquals(insert + " should have parent = " + exec, exec, ((CallID)callIDs.get(insert.getKey())).getParentRCC());
	}

	public void testGaps() throws Exception
	{
		setupGraphWithGaps();
		
		ConstructCallsAlgorithm algorithm = new ConstructCallsAlgorithm(rccList.size());

		HashMap rccListByCallee = new HashMap();

		// Depth of 2
		mapRCCLeaves(rccListByCallee, new RCC[]{ insert }, 2);

		ArrayList rootRCCList = new ArrayList();
		rootRCCList.addAll(rccList);
		
		algorithm.execute(rootRCCList, rccListByCallee, 2);
		List callIDs = algorithm.getCallIDs();

		assertNull(bufferAppend + " should have no parent", ((CallID)callIDs.get(bufferAppend.getKey())).getParentRCC());

		// Depth of 1
		rccListByCallee.clear();
		mapRCCLeaves(rccListByCallee, new RCC[]{ insert }, 1);
		
		rootRCCList.clear();
		rootRCCList.addAll(rccList);
		
		algorithm.execute(rootRCCList, rccListByCallee, 1);
		callIDs = algorithm.getCallIDs();

		assertEquals(insert, ((CallID)callIDs.get(bufferAppend.getKey())).getParentRCC());
	}

	private void mapRCCLeaves(Map rccListByCallee, RCC[] rccs, int depth)
	{
		for ( int i = 0; i < rccs.length; ++i )
		{
			RCC rcc = rccs[i];
			StackTrace calleeStack = rcc.getLeafStack(depth);
			if ( calleeStack != null )
			{
				ArrayList subList = (ArrayList)rccListByCallee.get(calleeStack);
				if ( subList == null )
				{
					subList = new ArrayList();
					rccListByCallee.put(calleeStack, subList);
				}
				subList.add(rcc);
			}
		}
	}
}
