package orbit.gui.tower;

import util.*;

import junit.framework.TestCase;

import java.awt.geom.Rectangle2D;

public class TestComputeCallLocation
	extends TestCase
{
	private BasicTestCalls calls;
	
	public TestComputeCallLocation(String name)
	{
		super(name);
	}

	public void setUp()
	{
		calls = new BasicTestCalls();
	}
	
	public void testComputeRootCallLocation()
	{
		ComputeCallLocation computeLocation = new ComputeCallLocation(calls.root, calls.root);
		computeLocation.execute();

		Rectangle2D.Double rootArea = new Rectangle2D.Double(0, 0, 1.0, 1.0);

		assertEquals(rootArea, computeLocation.getRectangle());
		assertEquals(computeLocation.getRenderDepth(), 0);
	}	

	public void testComputeChildCallLocation()
	{
		ComputeCallLocation computeLocation = new ComputeCallLocation(calls.dbOpen, calls.root);
		computeLocation.execute();

		assertEquals(computeLocation.getRenderDepth(), 2);
		double expectedArea = 0.4;
		double actualArea = TestUtil.area(computeLocation.getRectangle());
		assertTrue("expectedArea " + expectedArea + " != actualArea " + actualArea, TestUtil.equal(expectedArea, actualArea));
	}	

	public void testComputeMainLocation()
	{
		SimpleCallGraph simpleCG = new SimpleCallGraph();
		
		ComputeCallLocation computeLocation = new ComputeCallLocation(simpleCG.main, simpleCG.main);
		computeLocation.execute();

		assertEquals(computeLocation.getRenderDepth(), 0);
		double expectedArea = 1.0;
		double actualArea = TestUtil.area(computeLocation.getRectangle());
		assertTrue("expectedArea " + expectedArea + " != actualArea " + actualArea, TestUtil.equal(expectedArea, actualArea));
	}	

	public void testDBExec1Location()
	{
		SimpleCallGraph simpleCG = new SimpleCallGraph();
		
		ComputeCallLocation computeLocation = new ComputeCallLocation(simpleCG.DBExec1, simpleCG.main);
		computeLocation.execute();

		assertEquals(computeLocation.getRenderDepth(), 2);
		double expectedArea = 120 / 300.0;
		double actualArea = TestUtil.area(computeLocation.getRectangle());
		assertTrue("expectedArea " + expectedArea + " != actualArea " + actualArea, TestUtil.equal(expectedArea, actualArea));
	}	
}
