package orbit.gui;

import orbit.util.Log;

import org.apache.log4j.Category;

import util.SimpleCallGraph;
import util.TestUtil;
import junit.framework.TestCase;

// Tests CallRollupList
public class TestCallRollupList
	extends TestCase
{
	static Category LOG = Category.getInstance(TestCallRollupList.class);

	private SimpleCallGraph simpleCG;
	
	public TestCallRollupList(String name)
	{
		super(name);
	}

	public void setUp()
	{
		simpleCG = new SimpleCallGraph();
	}

	// Tests the entries in the CallGraph
	public void testSimpleCallGraph()
	{
		Log.debug(LOG, simpleCG.cg.toString(-1));
		
		assertEquals(simpleCG.test.getName(), "test");
		assertEquals(simpleCG.init.getName(), "init");

		assertEquals(simpleCG.append11.getName(), "append");
		assertEquals(simpleCG.append12.getName(), "append");

		assertTrue(TestUtil.equal(simpleCG.append11.getTime(), 10));
		assertTrue(TestUtil.equal(simpleCG.append12.getTime(), 20));

		assertTrue(TestUtil.equal(simpleCG.append21.getTime(), 5));
		assertTrue(TestUtil.equal(simpleCG.append22.getTime(), 10));
	}

	// Tests basic rollup of distinctly named calls
	public void testSimpleRollUp()
	{
		CallRollupList rollup = new CallRollupList();

		boolean exception = false;
		try {
			rollup.size();
		}
		catch (NullPointerException x) {
			exception = true;
		}
		assertTrue(exception);

		// Simulate adding some of the unique calls to the CallRollupList
		rollup.addCallee(simpleCG.main);
		rollup.addCallee(simpleCG.test);
		rollup.addCallee(simpleCG.init);

		rollup.sort();

		assertEquals(rollup.size(), 3);
		assertEquals(rollup.getCallName(0), "main");
		assertEquals(rollup.getCallName(1), "test");
		assertEquals(rollup.getCallName(2), "init");

		assertTrue(TestUtil.equal(rollup.getTime(0), 300));
		assertTrue(TestUtil.equal(rollup.getTime(1), 180));
		assertTrue(TestUtil.equal(rollup.getTime(2), 120));
	}

	// Tests rolling up the times from multiple calls with the same name
	public void testMultiRollUp()
	{
		CallRollupList rollup = new CallRollupList();

		rollup.addCallee(simpleCG.main);
		rollup.addCallee(simpleCG.test);

		rollup.addCallee(simpleCG.insert1);
		rollup.addCallee(simpleCG.insert2);
		rollup.addCallee(simpleCG.update1);
		rollup.addCallee(simpleCG.update2);

		rollup.sort();

		assertEquals(rollup.size(), 4);
		assertEquals(rollup.getCallName(0), "main");
		assertEquals(rollup.getCallName(1), "test");
		assertEquals(rollup.getCallName(2), "update");
		assertEquals(rollup.getCallName(3), "insert");

		assertTrue(TestUtil.equal(rollup.getTime(0), 300));
		assertTrue(TestUtil.equal(rollup.getTime(1), 180));
		assertTrue(TestUtil.equal(rollup.getTime(2), 60));
		assertTrue(TestUtil.equal(rollup.getTime(3), 30));
	}

	// Tests constructing a render-able CallDetails from the simple.prof model using
	//   the root of the call-graph as the diagram root
	public void testCallDetailsFromRoot()
	{
		CallDetails details;

		// Get details for the 'main' method
		// Should be 0 callers, and 2 callees (test and init)
		details = new CallDetails(simpleCG.main, simpleCG.main);
		assertEquals(details.getCallersModel().getRowCount(), 0);
		
		assertEquals(2, details.getCalleesModel().getRowCount());
		assertEquals("test", details.getCalleesModel().getValueAt(0, 0));
		assertEquals("180 (60%)", details.getCalleesModel().getValueAt(0, 1));
		assertEquals("init", details.getCalleesModel().getValueAt(1, 0));
		assertEquals("120 (40%)", details.getCalleesModel().getValueAt(1, 1));
					 
		// Get details for the 'DBExec' method
		// Should be 2 callers (init and test), and 2 callees (insert and update)
		details = new CallDetails(simpleCG.main, simpleCG.DBExec1);
		assertEquals(2, details.getCallersModel().getRowCount());
		assertEquals("test", details.getCallersModel().getValueAt(0, 0));
		assertEquals("120 (57.14%)", details.getCallersModel().getValueAt(0, 1));
		assertEquals("init", details.getCallersModel().getValueAt(1, 0));
		assertEquals("90 (42.86%)", details.getCallersModel().getValueAt(1, 1));

		// 'update' should come first because it takes more time
		assertEquals(2, details.getCalleesModel().getRowCount());
		assertEquals("update", details.getCalleesModel().getValueAt(0, 0));
		assertEquals("60 (66.67%)", details.getCalleesModel().getValueAt(0, 1));
		assertEquals("insert", details.getCalleesModel().getValueAt(1, 0));
		assertEquals("30 (33.33%)", details.getCalleesModel().getValueAt(1, 1));
	}
	
	// Tests constructing a render-able CallDetails from the simple.prof model using
	//   the 'init' call as the diagram root
	public void testCallDetailsFromPartialTree()
	{
		CallDetails details;

		// Get details for the 'DBExec' method
		// Should be 1 caller (init), and 2 callees (insert and update)
		details = new CallDetails(simpleCG.init, simpleCG.DBExec1);
		assertEquals(1, details.getCallersModel().getRowCount());
		assertEquals("init", details.getCallersModel().getValueAt(0, 0));
		assertEquals("90 (100%)", details.getCallersModel().getValueAt(0, 1));

		// 'update' should come first because it takes more time
		assertEquals(2, details.getCalleesModel().getRowCount());
		assertEquals("update", details.getCalleesModel().getValueAt(0, 0));
		assertEquals("20 (66.67%)", details.getCalleesModel().getValueAt(0, 1));
		assertEquals("insert", details.getCalleesModel().getValueAt(1, 0));
		assertEquals("10 (33.33%)", details.getCalleesModel().getValueAt(1, 1));
	}
}
