package orbit.util;

public class Util
{
	public static void handleTrace(Class origin, Throwable x)
	{
		x.printStackTrace();
	}

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

	public static NetworkException handleAsNetwork(Class origin, Throwable x)
	{
		x.printStackTrace();
		return new NetworkException(x.getClass().getName() + " in " + origin.getName() + " : " + x.getMessage());
	}
}
