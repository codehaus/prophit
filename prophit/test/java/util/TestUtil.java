package util;

import java.awt.geom.Rectangle2D;

import junit.framework.AssertionFailedError;

public class TestUtil
{
	public static double TOLERANCE = 1e-5;

	public static boolean equal(double first, Object second)
	{
		return equal(first, ((Number)second).doubleValue());
	}
	
	public static boolean equal(double first, double second)
	{
		return Math.abs(second - first ) < TOLERANCE;
	}
	
	public static void assertRectangle(Rectangle2D.Double first, Rectangle2D.Double second)
	{
		if ( !equal(first.x, second.x) ||
			 !equal(first.y, second.y) ||
			 !equal(first.width, second.width) ||
			 !equal(first.height, second.height) )
		{
			throw new AssertionFailedError("Expected " + first + " = " + second);
		}
	}

	public static double area(Rectangle2D.Double rectangle)
	{
		return rectangle.height * rectangle.width;
	}
}
