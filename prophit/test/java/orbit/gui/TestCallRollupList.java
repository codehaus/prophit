package orbit.gui;

import util.SimpleCallGraph;
import util.TestUtil;

import orbit.model.*;
import orbit.gui.CallRollupList;
import orbit.util.Log;

import org.apache.log4j.Category;

import junit.framework.TestCase;

// Tests CallRollupList
public class TestCallRollupList
	extends TestCase
{
	static Category LOG = Category.getInstance(TestCallRollupList.class);
	
	CallGraph simpleCG;

	Call main;
	Call test;
	Call init;

	Call DBExec1;
	Call DBExec2;

	Call insert1;
	Call update1;
	Call insert2;
	Call update2;

	Call append11;
	Call append12;
		
	Call append21;
	Call append22;
	
	public TestCallRollupList(String name)
	{
		super(name);
	}

	public void setUp()
	{
		simpleCG = SimpleCallGraph.load();

		main = (Call)simpleCG.getChildren().get(0);
		test = (Call)main.getChildren().get(0);
		init = (Call)main.getChildren().get(1);

		DBExec1 = (Call)test.getChildren().get(0);
		DBExec2 = (Call)init.getChildren().get(0);

		insert1 = (Call)DBExec1.getChildren().get(0);
		update1 = (Call)DBExec1.getChildren().get(1);
		insert2 = (Call)DBExec2.getChildren().get(0);
		update2 = (Call)DBExec2.getChildren().get(1);

		append11 = (Call)insert1.getChildren().get(0);
		append12 = (Call)update1.getChildren().get(0);
		
		append21 = (Call)insert2.getChildren().get(0);
		append22 = (Call)update2.getChildren().get(0);
	}

	// Tests the entries in the CallGraph
	public void testSimpleCallGraph()
	{
		Log.debug(LOG, simpleCG.toString(-1));
		
		assertEquals(test.getName(), "test");
		assertEquals(init.getName(), "init");

		assertEquals(append11.getName(), "append");
		assertEquals(append12.getName(), "append");

		assert(TestUtil.equal(append11.getTime(), 10));
		assert(TestUtil.equal(append12.getTime(), 20));

		assert(TestUtil.equal(append21.getTime(), 5));
		assert(TestUtil.equal(append22.getTime(), 10));
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
		assert(exception);

		// Simulate adding some of the unique calls to the CallRollupList
		rollup.addCall(main);
		rollup.addCall(test);
		rollup.addCall(init);

		rollup.sort();

		assertEquals(rollup.size(), 3);
		assertEquals(rollup.getCallName(0), "main");
		assertEquals(rollup.getCallName(1), "test");
		assertEquals(rollup.getCallName(2), "init");

		assert(TestUtil.equal(rollup.getTime(0), 300));
		assert(TestUtil.equal(rollup.getTime(1), 180));
		assert(TestUtil.equal(rollup.getTime(2), 120));
	}

	// Tests rolling up the times from multiple calls with the same name
	public void testMultiRollUp()
	{
		CallRollupList rollup = new CallRollupList();

		rollup.addCall(main);
		rollup.addCall(test);

		rollup.addCall(insert1);
		rollup.addCall(insert2);
		rollup.addCall(update1);
		rollup.addCall(update2);

		rollup.sort();

		assertEquals(rollup.size(), 4);
		assertEquals(rollup.getCallName(0), "main");
		assertEquals(rollup.getCallName(1), "test");
		assertEquals(rollup.getCallName(2), "update");
		assertEquals(rollup.getCallName(3), "insert");

		assert(TestUtil.equal(rollup.getTime(0), 300));
		assert(TestUtil.equal(rollup.getTime(1), 180));
		assert(TestUtil.equal(rollup.getTime(2), 60));
		assert(TestUtil.equal(rollup.getTime(3), 30));
	}

	// Tests constructing a render-able CallDetails from the simple.prof model using
	//   the root of the call-graph as the diagram root
	public void testCallDetailsFromRoot()
	{
		CallDetails details;

		// Get details for the 'main' method
		// Should be 0 callers, and 2 callees (test and init)
		details = new CallDetails(main, main);
		assertEquals(details.getCallersModel().getRowCount(), 0);
		
		assertEquals(2, details.getCalleesModel().getRowCount());
		assertEquals("test", details.getCalleesModel().getValueAt(0, 0));
		assertEquals("180 (60%)", details.getCalleesModel().getValueAt(0, 1));
		assertEquals("init", details.getCalleesModel().getValueAt(1, 0));
		assertEquals("120 (40%)", details.getCalleesModel().getValueAt(1, 1));
					 
		// Get details for the 'DBExec' method
		// Should be 2 callers (init and test), and 2 callees (insert and update)
		details = new CallDetails(main, DBExec1);
		assertEquals(2, details.getCallersModel().getRowCount());
		assertEquals("test", details.getCallersModel().getValueAt(0, 0));
		assertEquals("180 (60%)", details.getCallersModel().getValueAt(0, 1));
		assertEquals("init", details.getCallersModel().getValueAt(1, 0));
		assertEquals("120 (40%)", details.getCallersModel().getValueAt(1, 1));

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
		details = new CallDetails(init, DBExec1);
		assertEquals(1, details.getCallersModel().getRowCount());
		assertEquals("init", details.getCallersModel().getValueAt(0, 0));
		assertEquals("120 (100%)", details.getCallersModel().getValueAt(0, 1));

		// 'update' should come first because it takes more time
		assertEquals(2, details.getCalleesModel().getRowCount());
		assertEquals("update", details.getCalleesModel().getValueAt(0, 0));
		assertEquals("20 (66.67%)", details.getCalleesModel().getValueAt(0, 1));
		assertEquals("insert", details.getCalleesModel().getValueAt(1, 0));
		assertEquals("10 (33.33%)", details.getCalleesModel().getValueAt(1, 1));
	}
}
