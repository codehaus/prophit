package orbit.util;

public class Util
{
	public static RuntimeException handle(Class origin, Throwable x)
	{
		x.printStackTrace();
		return new RuntimeException(x.getClass().getName() + " in " + origin.getName() + " : " + x.getMessage());
	}

	public static ConfigurationException handleAsConfiguration(Class origin, Throwable x)
	{
		x.printStackTrace();
		return new ConfigurationException(x.getClass().getName() + " in " + origin.getName() + " : " + x.getMessage());
	}
}