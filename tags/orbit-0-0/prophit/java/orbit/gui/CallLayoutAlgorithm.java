package orbit.gui;

import orbit.model.Call;

import java.awt.geom.Rectangle2D;
import java.util.Iterator;

public class CallLayoutAlgorithm
{
	private final CallAdapter root;
	private final Callback    callback;
	private final RectangleLayout layout;
	private int   maxDepth = -1;
	private Rectangle2D.Double rootRectangle = new Rectangle2D.Double(0, 0, 1, 1);
	
	public CallLayoutAlgorithm(CallAdapter root, TimeMeasure measure, Callback callback)
	{
		this.root = root;
		this.layout = new RectangleLayout(measure);
		this.callback = callback;
	}

	public void setMaxDepth(int depth)
	{
		this.maxDepth = depth;
	}

	public void setRootRectangle(Rectangle2D.Double rootRectangle)
	{
		this.rootRectangle = rootRectangle;
	}

	public void execute()
	{
		layoutCall(root, rootRectangle, rootRectangle, 0);
	}

	/**
	 * Layout the call and return the remainder rectangle (which can be null, signifying that the
	 * call was not rendered at all).
	 */
	private Rectangle2D.Double layoutCall(CallAdapter call, Rectangle2D.Double parentRectangle,
						  Rectangle2D.Double renderRectangle, int depth)
	{
		if ( depth > maxDepth )
			return null;

		layout.initialize(call, parentRectangle, renderRectangle);
		Rectangle2D.Double extent = layout.getExtent();
		Rectangle2D.Double rectangle = layout.getRectangle(extent);
		Rectangle2D.Double remainder = layout.getRemainderExtent(extent);
		if ( callback.nextCall(call, rectangle, depth) )
		{
			layoutChildren(call, rectangle, depth + 1);
		}
		return remainder;
	}

	private void layoutChildren(CallAdapter call, Rectangle2D.Double parentRectangle, int depth)
	{
		Rectangle2D.Double nextRenderRectangle = parentRectangle;
		for ( Iterator i = call.getChildren().iterator(); i.hasNext(); )
		{
			Call child = (Call)i.next();
			Rectangle2D.Double nextRemainder = layoutCall(new CallAdapter(child), parentRectangle,
														  nextRenderRectangle, depth);
			if ( nextRemainder != null )
				nextRenderRectangle = nextRemainder;
		}
	}
		
	public interface Callback
	{
		public boolean nextCall(CallAdapter call, Rectangle2D.Double rectangle, int depth);
	}
}
