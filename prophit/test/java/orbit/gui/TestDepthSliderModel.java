package orbit.gui;

import util.SimpleCallGraph;

import junit.framework.TestCase;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

/**
 * Test the DepthSliderModel by constructing a BlockDiagramModel and DepthSliderModel and manually
 * changing the RENDER_CALL_PROPERTY and LEVEL_OF_DETAIL_PROPERTY on the BlockDiagramModel.
 */
public class TestDepthSliderModel
	extends TestCase
{
	public TestDepthSliderModel(String name)
	{
		super(name);
	}

	/**
	 * Construct a BlockDiagramModel with LevelOfDetail = VeryFine
	 * Assert the computed maxCallDepth
	 * Change the LevelOfDetail
	 * Assert the computed maxCallDepth
	 */
	public void testBasicOperation()
	{
		SimpleCallGraph scg = new SimpleCallGraph();

		BlockDiagramModel model = new BlockDiagramModel(scg.cg);
		model.setLevelOfDetail(LevelOfDetail.VeryFine);
		
		DepthSliderModel  dsModel = new DepthSliderModel();
		DepthSliderListener listener = new DepthSliderListener();
		dsModel.addListener(listener);
		
		dsModel.setModel(model);

		assertEquals(1, listener.size());
		assertEquals(5, listener.getDepth());

		// Should be no change event if the LevelOfDetail is set to the same value
		model.setLevelOfDetail(LevelOfDetail.VeryFine);
		assertEquals(1, listener.size());
		assertEquals(5, listener.getDepth());

		// Filter out the 2nd DBExec but not the first
		model.setLevelOfDetail(new LevelOfDetail("Custom", 0.35));
		assertEquals(2, listener.size());
		assertEquals(3,listener.getDepth());
	}

	static class DepthSliderListener
		implements PropertyChangeListener
	{
		ArrayList values = new ArrayList();

		public int size() { return values.size(); }
		public int getDepth() { return ((Integer)values.get(0)).intValue(); }
		
		public void propertyChange(PropertyChangeEvent evt)
		{
			if ( DepthSliderModel.MAX_DEPTH_PROPERTY.equals(evt.getPropertyName()) )
			{
				Integer value = (Integer)evt.getNewValue();
				values.add(value);
			}
		}		
	}
}
