package orbit.parsers;

import orbit.model.*;
import orbit.parsers.ConstructCallsAlgorithm;
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
	RCC update;
	RCC concat1;
	RCC concat2;
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

	private void setupGraphWithAmbiguity()
	{
		setupBasicGraph();

		// Child of 'exec'
		update = new RCC(new StackTrace(new String[]{ "update", "exec", "test" }), 10, 10, key++);

		concat1 = new RCC(new StackTrace(new String[]{ "concat", "insert", "exec" }), 100, 30, key++);
		concat2 = new RCC(new StackTrace(new String[]{ "concat", "update", "exec" }), 25, 5, key++);

		bufferAppend = new RCC(new StackTrace(new String[]{ "bufferAppend", "modify", "concat", }), 100, 25, key++);

		rccList.add(update);
		rccList.add(concat1);
		rccList.add(concat2);
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

	public void testAmbiguity()
	{
		setupGraphWithAmbiguity();

		ConstructCallsAlgorithm algorithm = new ConstructCallsAlgorithm(rccList.size());

		HashMap rccListByCallee = new HashMap();

		// Depth of 2
		mapRCCLeaves(rccListByCallee, rccList, 2);

		ArrayList rootRCCList = new ArrayList();
		rootRCCList.addAll(rccList);
		
		algorithm.execute(rootRCCList, rccListByCallee, 2);
		List callIDs = algorithm.getCallIDs();

		assertEquals(callIDs.size(), rccList.size() + 1);
		assertEquals(insert, ((CallID)callIDs.get(concat1.getKey())).getParentRCC());
		assertEquals(update, ((CallID)callIDs.get(concat2.getKey())).getParentRCC());
		assertNull(bufferAppend + " should have no parent", ((CallID)callIDs.get(bufferAppend.getKey())).getParentRCC());

		rootRCCList.clear();
		rootRCCList.add(bufferAppend);
		rccListByCallee.clear();
		// Depth of 1
		mapRCCLeaves(rccListByCallee, rccList, 1);

		algorithm.execute(rootRCCList, rccListByCallee, 1);
		callIDs = algorithm.getCallIDs();

		// System.out.println("testAmbiguity CallIDs : " + callIDs);
		
		assertEquals(rccList.size() + 3, callIDs.size());
		// Should have 2 proxy CallIDs, thus the callIDs[bufferAppend.key] should be null
		assertNull(callIDs.get(bufferAppend.getKey()));
		assertEquals(insert, ((CallID)callIDs.get(concat1.getKey())).getParentRCC());
		assertEquals(update, ((CallID)callIDs.get(concat2.getKey())).getParentRCC());
		assertNotNull(callIDs.get(rccList.size() + 1));
		assertNotNull(callIDs.get(rccList.size() + 2));
		assertEquals("modify -> bufferAppend [ 8 -> p11 ]", ((CallID)callIDs.get(rccList.size() + 1)).toString());
		assertEquals("modify -> bufferAppend [ 9 -> p12 ]", ((CallID)callIDs.get(rccList.size() + 2)).toString());
	}

	private void mapRCCLeaves(Map rccListByCallee, ArrayList rccList, int depth)
	{
		mapRCCLeaves(rccListByCallee, (RCC[])rccList.toArray(new RCC[0]), depth);
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
