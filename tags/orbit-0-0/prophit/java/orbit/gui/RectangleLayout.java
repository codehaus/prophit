package orbit.gui;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * This class is meant to be used as a flyweight.
 */
public class RectangleLayout
{
	private final TimeMeasure        measure;
	
	private CallAdapter        call;
	private Rectangle2D.Double parentExtent;
	private Rectangle2D.Double remainderExtent;

	public RectangleLayout(TimeMeasure measure)
	{
		this.measure = measure;
	}
	
	public void initialize(CallAdapter call, Rectangle2D.Double parentExtent, Rectangle2D.Double remainderExtent)
	{
		this.call = call;
		this.parentExtent = parentExtent;
		this.remainderExtent = remainderExtent;
		
		// System.out.println("Computing " + call + " with parentExtent = " + parentExtent + ", remainderExtent = " + remainderExtent);
	}

	/**
	 * Get the rectangle governing the extent of area occupied by the call. This includes
	 * the area in which the image will be drawn, as well as surrounding 'territory' which will not
	 * be filled in but is owned by this Call. This extra area represents the time which was
	 * spent in the parent method itself, exclusive of its callees.
	 */
	public Rectangle2D.Double getExtent()
	{
		// The amount of area rendered for the Call should be equal to its fraction
		//   of the parent time
		// It should occupy the fraction of the extent equal to its fraction of all the time
		//   spent in children of its parent

		double fractionOfChildren = call.getFractionOfParentChildTimes(measure);

		double areaRatio = ( remainderExtent.height / parentExtent.height ) * ( remainderExtent.width / parentExtent.width );

		// System.out.println(call + " is " + fractionOfChildren + " of " + call.getParent());
		// System.out.println("areaRatio is " + areaRatio);
		
		// Upper-left of call extent in coordinates of parent extent
		Point2D.Double upperLeft = new Point2D.Double(remainderExtent.x, remainderExtent.y + remainderExtent.height);
		double enclosedWidth, enclosedHeight;
		// The rectangle should take be left-to-right if the
		//   available space is wider than it is long (or square)
		if ( remainderExtent.width >= remainderExtent.height )
		{
			enclosedWidth = remainderExtent.width * fractionOfChildren / areaRatio;
			enclosedHeight = remainderExtent.height;
		}
		// Otherwise, top-to-bottom
		else
		{
			enclosedWidth = remainderExtent.width;
			enclosedHeight = remainderExtent.height * fractionOfChildren / areaRatio;
		}
		return new Rectangle2D.Double(upperLeft.x, upperLeft.y - enclosedHeight,
									  enclosedWidth, enclosedHeight);
	}

	/**
	 * Get the rectangle which is left over when the extent is removed from the previous remainder extent.
	 */
	public Rectangle2D.Double getRemainderExtent(Rectangle2D.Double extent)
	{
		// Return the area to the right of the remainderExtent
		if ( remainderExtent.width >= remainderExtent.height )
		{
			return new Rectangle2D.Double(remainderExtent.x + extent.width, remainderExtent.y,
										  remainderExtent.width - extent.width, remainderExtent.height);
		}
		// Return the area to the bottom of the remainderExtent
		else
		{
			return new Rectangle2D.Double(remainderExtent.x, remainderExtent.y,
										  remainderExtent.width, remainderExtent.height - extent.height);
		}
	}

	/**
	 * Get the rectangle which should actually be rendered.
	 */
	public Rectangle2D.Double getRectangle(Rectangle2D.Double extent)
	{
		double fractionOfParent = call.getFractionOfParentTime(measure);
		double fractionOfChildren = call.getFractionOfParentChildTimes(measure);

		/*
		if ( fractionOfParent != 0 &&
			 ( fractionOfParent / fractionOfChildren ) > 1 )
		{
			System.out.println(call);
			System.out.println(call.getParent());
			System.out.println("\t" + fractionOfParent);
			System.out.println("\t" + fractionOfChildren);
			System.out.println("\t" + ( fractionOfParent / fractionOfChildren));
		}
		*/

		double ratio = 0;
		if ( fractionOfChildren != 0 )
			ratio = fractionOfParent / fractionOfChildren;
		if ( ratio > 1 )
			ratio = 1;
		
		double dimensionAdjustment = 1 - Math.sqrt(ratio);
		double widthAdjustment = extent.width * dimensionAdjustment;
		double heightAdjustment = extent.height * dimensionAdjustment;
		return new Rectangle2D.Double(extent.x + widthAdjustment / 2, extent.y + heightAdjustment / 2,
									  extent.width - widthAdjustment, extent.height - heightAdjustment);
	}
}
