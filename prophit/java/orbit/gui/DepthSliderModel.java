package orbit.gui;

import orbit.model.Call;
import orbit.model.MaxDepthVisitor;
import orbit.util.Log;

import org.apache.log4j.Category;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Contains model information for the slider control which the user uses to determine
 * how much of the call graph to display.
 * Its principal function is to determine the maximum stack depth of the current diagram.
 */
class DepthSliderModel
{
	public static final Category LOG = Category.getInstance(DepthSliderModel.class);
	public static final String   MAX_DEPTH_PROPERTY = "maxDepth";

	/** Spend at most 1/4 second looking for the maximum stack depth */
	private static final long MAX_COMPUTE_TIME = 250;

	private int maxDepth = -1;
	private BlockDiagramModel model = null;
	
	private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
	
	public synchronized void addListener(PropertyChangeListener listener)
	{
		changeSupport.addPropertyChangeListener(listener);
	}
	
	public synchronized void removeListener(PropertyChangeListener listener)
	{
		changeSupport.removePropertyChangeListener(listener);
	}

	public void setModel(BlockDiagramModel model)
	{
		this.model = model;
		model.addListener(new PropertyChangeListener()
			{
				public void propertyChange(PropertyChangeEvent evt)
				{
					if ( BlockDiagramModel.RENDER_CALL_PROPERTY.equals(evt.getPropertyName()) )
					{
						computeMaxDepth(MAX_COMPUTE_TIME);
					}
				}
			});

		maxDepth = -1;
		computeMaxDepth(MAX_COMPUTE_TIME);
	}

	/**
	 * @param computeTime maximum amount of time to spend looking for the max depth.
	 * Use -1 to look forever
	 */
	private void computeMaxDepth(long computeTime)
	{
		Call root = model.getRootRenderState().getRenderCall();
		Log.debug(LOG, "Computing maxDepth for root ", root, ", computeTime = ", computeTime);
		int newMaxDepth;
		MaxDepthVisitor visitor = new MaxDepthVisitor(computeTime);
		root.depthFirstTraverse(visitor);
		if ( visitor.getMaxDepth() != -1 )
		{
			newMaxDepth = visitor.getMaxDepth();
			Log.debug(LOG, "\tFound max depth ", visitor.getMaxDepth());
		}
		else
		{
			newMaxDepth = model.getRootCall().getMaxDepth() - root.getDepth();
			Log.debug(LOG, "\tTimed out. Using depth estimate ", newMaxDepth);
		}
		setMaxDepth(newMaxDepth);
	}

	private void setMaxDepth(int newMaxDepth)
	{
		Log.debug(LOG, "maxDepth = ", newMaxDepth);
		if ( newMaxDepth != maxDepth )
		{
			int oldMaxDepth = maxDepth;
			maxDepth = newMaxDepth;
			changeSupport.firePropertyChange(MAX_DEPTH_PROPERTY, oldMaxDepth, newMaxDepth);
		}
	}	
}
	
