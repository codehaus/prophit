package orbit.gui;

import orbit.model.Call;

import java.awt.geom.Rectangle2D;
import java.util.Iterator;

public class CallLayoutAlgorithm
{
	private final CallAdapter        root;
	private final RectangleLayout    layout;
	private final int                maxDepth;
	private final Rectangle2D.Double rootRectangle;

	private Callback callback = null;
	
	public CallLayoutAlgorithm(CallAdapter root, TimeMeasure measure, int depth, Rectangle2D.Double rootRectangle)
	{
		this.root = root;
		this.layout = new RectangleLayout(measure);
		this.maxDepth = depth;
		this.rootRectangle = rootRectangle;
	}

	public void setCallback(Callback callback)
	{
		this.callback = callback;
	}

	public void execute()
	{
		if ( callback == null )
			throw new NullPointerException("Callback is null in CallLayoutAlgorithm");

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
		if ( callback.beginCall(call, rectangle, depth) )
		{
			layoutChildren(call, rectangle, depth + 1);
			callback.endCall(call);
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
		public boolean beginCall(CallAdapter call, Rectangle2D.Double rectangle, int depth);

		public void endCall(CallAdapter call);
	}
}
