package orbit.util;

import org.apache.log4j.Category;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.util.Properties;

public class Log
{
	static
	{
		try
		{
			Properties props = new Properties();
			props.load(Log.class.getClassLoader().getResourceAsStream("log.properties"));
			PropertyConfigurator.configure(props);
		}
		catch (IOException x)
		{
			x.printStackTrace();
		}
	}
	
	public static void debug(Category log, Object message)
	{
		log.debug(message);
	}

	public static void debug(Category log, Object msg1, Object msg2)
	{
		if ( log.isDebugEnabled() )
			log.debug("" + msg1 + msg2);
	}

	public static void debug(Category log, Object msg1, int msg2)
	{
		if ( log.isDebugEnabled() )
			log.debug("" + msg1 + msg2);
	}

	public static void debug(Category log, Object msg1, double msg2)
	{
		if ( log.isDebugEnabled() )
			log.debug("" + msg1 + msg2);
	}

	public static void debug(Category log, Object msg1, Object msg2, Object msg3)
	{
		if ( log.isDebugEnabled() )
			log.debug("" + msg1 + msg2 + msg3);
	}

	public static void debug(Category log, Object msg1, int msg2, Object msg3)
	{
		if ( log.isDebugEnabled() )
			log.debug("" + msg1 + msg2 + msg3);
	}

	public static void debug(Category log, Object msg1, Object msg2, Object msg3, Object msg4)
	{
		if ( log.isDebugEnabled() )
			log.debug("" + msg1 + msg2 + msg3 + msg4);
	}

	public static void debug(Category log, Object msg1, Object msg2, Object msg3, int msg4)
	{
		if ( log.isDebugEnabled() )
			log.debug("" + msg1 + msg2 + msg3 + msg4);
	}

	public static void debug(Category log, Object msg1, Object msg2, Object msg3, long msg4)
	{
		if ( log.isDebugEnabled() )
			log.debug("" + msg1 + msg2 + msg3 + msg4);
	}
}
