package orbit.gui;

import orbit.model.Call;
import orbit.util.Log;

import org.apache.log4j.Category;

import java.awt.geom.Rectangle2D;
import java.util.HashSet;

/**
 * This class computes the depth and render rectangle for a specified Call, starting from
 * a specified root Call. It can be used to re-construct the location of a specific Call in the
 * 3-D space of the block diagram.
 * <p>
 * Construct the ComputeCallLocation object, then call {@link #execute}.
 */
class ComputeCallLocation
{
	public static Category LOG = Category.getInstance(ComputeCallLocation.class);
	
	private final Call call;
	private final Call root;
	private final Rectangle2D.Double rootExtent;

	private Rectangle2D.Double rectangle = null;
	private int depth = -1;

	public ComputeCallLocation(Call call, Call root)
	{
		this(call, root, new Rectangle2D.Double(0, 0, 1, 1));
	}

	public ComputeCallLocation(Call call, Call root, Rectangle2D.Double rootExtent)
	{
		this.call = call;
		this.root = root;
		this.rootExtent = rootExtent;
	}

	/**
	 * Get the location of the render rectangle for the specified call.
	 * @see #ComputeCallLocation
	 */
	public Rectangle2D.Double getRectangle()
	{
		return rectangle;
	}

	/**
	 * Get the render depth of specified call. The render depth is the depth of the call in
	 * the block diagram, not the depth of the call in the call graph.
	 * @see #ComputeCallLocation
	 */
	public int getRenderDepth()
	{
		return depth;
	}

	/**
	 * Compute the rectangle and render depth.
	 */
	public void execute()
	{
		Log.debug(LOG, "ComputeCallLocation looking for call ", call);
		
		final HashSet callParents = new HashSet();

		// callParents.add(call);
		Call parent = call.getParent();
		while ( parent != null )
		{
			callParents.add(parent);
			parent = parent.getParent();
		}
		
		Log.debug(LOG, "Parent set is ", callParents);
		
		CallLayoutAlgorithm layout = new CallLayoutAlgorithm(new CallAdapter(root), TimeMeasure.TotalTime, -1, rootExtent);
		layout.setCallback(new CallLayoutAlgorithm.Callback()
			{
				/**
				 * This Callback handler allows the CallLayoutAlgorithm to continue as long as the specified
				 * call is in the call stack of the Call with which the ComputeCallLocation was constructed.
				 */
				public boolean beginCall(CallAdapter adapter, Rectangle2D.Double rectangle, int depth)
				{
					Call currentCall = adapter.getCall();
					if ( currentCall.equals(ComputeCallLocation.this.call) )
					{
						Log.debug(LOG, "Found call ", currentCall);
						ComputeCallLocation.this.rectangle = rectangle;
						ComputeCallLocation.this.depth = depth;
						return false;
					}
					else if ( callParents.contains(currentCall) )
					{
						Log.debug(LOG, "Following parent ", currentCall);
						return true;
					}
					else
					{
						Log.debug(LOG, "Not following unrelated call ", currentCall);
						return false;
					}
				}
				
				public void endCall(CallAdapter call) { }
			});
		layout.execute();
	}
}
