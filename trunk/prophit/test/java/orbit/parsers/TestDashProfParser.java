package orbit.parsers;

import orbit.gui.CallAdapter;
import orbit.gui.RectangleLayout;
import orbit.gui.TimeMeasure;
import orbit.model.*;
import orbit.util.IntStack;
import util.BasicTestCalls;
import util.TestUtil;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.List;

import junit.framework.TestCase;

public class TestDashProfParser
	extends TestCase
{
	public TestDashProfParser(String name)
	{
		super(name);
	}

	/**
	 * Tests that the BasicTestCalls utility class loads correctly.
	 * Also tests the RectangleLayout class on the BasicTestCalls data.
	 */
	public void testBasicTestCalls()
	{
		BasicTestCalls calls = new BasicTestCalls();

		CallAdapter root = new CallAdapter(calls.root);

		CallAdapter main = new CallAdapter(calls.main);
		CallAdapter events = new CallAdapter(calls.events);

		CallAdapter dbOpen = new CallAdapter(calls.dbOpen);
		CallAdapter dbClose = new CallAdapter(calls.dbClose);
		CallAdapter dbInit = new CallAdapter(calls.dbInit);

		assertTrue("main.timeInSelf should be 20, is " + main.getExclusiveTime(TimeMeasure.TotalTime),
				   main.getExclusiveTime(TimeMeasure.TotalTime) == 20);

		assertTrue("dbOpen.depth should be 2",
				   dbOpen.getDepth() == 2);
		assertTrue("dbOpen.timeInSelf should be 40",
				   dbOpen.getExclusiveTime(TimeMeasure.TotalTime) == 40);
		assertTrue("main.timeInSelf should be 20",
				   main.getExclusiveTime(TimeMeasure.TotalTime) == 20);
		assertTrue("main.timeInChildren should be 50",
				   main.getTimeInChildren(TimeMeasure.TotalTime) == 50);
		assertTrue("main.fractionOfParentTime should be 0.7",
				   main.getInclusiveTimeFractionOfParentInclusiveTime(TimeMeasure.TotalTime) == 0.7);
		assertTrue("dbOpen.fractionOfChildTimes should be 0.8",
				   dbOpen.getInclusiveFractionOfParentChildTimes(TimeMeasure.TotalTime) == 0.8);
		assertTrue("dbOpen.fractionOfChildTimes should be " + ( 4 / 7.0 ),
				   dbOpen.getInclusiveTimeFractionOfParentInclusiveTime(TimeMeasure.TotalTime) == 4 / 7.0);

		Rectangle2D.Double fullExtent = new Rectangle2D.Double(0, 0, 1.0, 1.0);
		Rectangle2D.Double leftHalf = new Rectangle2D.Double(0, 0, 0.5, 1.0);
		
		RectangleLayout layout = new RectangleLayout(TimeMeasure.TotalTime);

		layout.initialize(null, root, fullExtent, fullExtent);
		TestUtil.assertRectangle(layout.getExtent(), new Rectangle2D.Double(0, 0, 1, 1));
		
		layout.initialize(new CallAdapter(main.getParent()), main, fullExtent, fullExtent);
		TestUtil.assertRectangle(layout.getExtent(), new Rectangle2D.Double(0, 0, 0.7, 1));
		TestUtil.assertRectangle(layout.getRemainderExtent(layout.getExtent()), new Rectangle2D.Double(0.7, 0, 0.3, 1));

		layout.initialize(new CallAdapter(dbOpen.getParent()), dbOpen, fullExtent, fullExtent);
		TestUtil.assertRectangle(layout.getExtent(), new Rectangle2D.Double(0, 0, 0.8, 1));
		TestUtil.assertRectangle(layout.getRemainderExtent(layout.getExtent()), new Rectangle2D.Double(0.8, 0, 0.2, 1));

		// Should occupy the top 50% of the remaining triangle
		Rectangle2D.Double remainder = layout.getRemainderExtent(layout.getExtent());
		layout.initialize(new CallAdapter(dbClose.getParent()), dbClose, fullExtent, remainder);
		TestUtil.assertRectangle(layout.getExtent(), new Rectangle2D.Double(0.8, 0.5, 0.2, 0.5));
		TestUtil.assertRectangle(layout.getRemainderExtent(layout.getExtent()), new Rectangle2D.Double(0.8, 0, 0.2, 0.5));
		// Rendered area should the same fraction of the Extent as the fractionOfParent / fractionOfChild
		assertEquals("Incorrect area ratio",
					 4 / 7.0 / 0.8,
					 TestUtil.area(layout.getRectangle(layout.getExtent())) / TestUtil.area(layout.getExtent()),
					 TestUtil.TOLERANCE);
		
		System.out.println(layout.getRectangle(layout.getExtent()));
	}

	/**
	 * Tests the -prof parser for data/simple.prof
	 */
	public void testDashProfParserSimple() throws Exception
	{
		Parser parser = ParserFactory.instance().createParser(new File(System.getProperty("basedir") + "/data/simple.prof"));
		ModelBuilder builder = ModelBuilderFactory.newModelBuilder();
		parser.execute(builder);
		List callIDs = builder.getCallIDs();
		List proxyCallIDs = CallID.getProxyCallIDs(callIDs);

		assertTrue(callIDs + ".size should be 14",
				   callIDs.size() == 14);
		assertTrue(proxyCallIDs + ".size should be 4",
				   proxyCallIDs.size() == 4);

		CallID[] callIDArray = (CallID[])callIDs.toArray(new CallID[0]);
		double[] fractions = new double[callIDs.size()];
		for ( int i = 0; i < fractions.length; ++i )
			fractions[i] = 1;
		fractions[10] =  0.333333;
		fractions[11] =  0.666667;
		fractions[12] =  0.333333;
		fractions[13] =  0.666667;
		CallGraph cg = new CallGraph(callIDArray, fractions);

		class SimpleVisitor
			implements Call.Visitor
		{
			int count = 0;

			int getCount() { return count; }
			
			public boolean visit(CallID callID, IntStack callStack)
			{
				++count;
				return true;
			}			
		}

		SimpleVisitor visitor = new SimpleVisitor();
		cg.getRoot().depthFirstTraverse(visitor);
		assertTrue("Expected to be visited 13 times", visitor.getCount() == 13);
	}
	
	/**
	 * Tests the -prof parser for data/hello.prof
	 */
	public void testDashProfParserHello() throws Exception
	{
		Loader loader = LoaderFactory.instance().createLoader(new File(System.getProperty("basedir") + "/data/hello.prof"));
		loader.parse();
		CallGraph cg = loader.solve();
	} 
}
