package test;

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
}
