package orbit.util;

import org.apache.log4j.Category;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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

	public static void error(Category log, Throwable t)
	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		t.printStackTrace(pw);
		log.error(sw.toString());
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

	public static void debug(Category log, Object msg1, Object msg2, Object msg3, Object msg4, double msg5)
	{
		if ( log.isDebugEnabled() )
			log.debug("" + msg1 + msg2 + msg3 + msg4 + msg5);
	}

	public static void debug(Category log, Object msg1, double msg2, Object msg3, double msg4, Object msg5,
							 double msg6, Object msg7, double msg8, Object msg9, double msg10)
	{
		if ( log.isDebugEnabled() )
			log.debug("" + msg1 + msg2 + msg3 + msg4 + msg5 + msg6 + msg7 + msg8 + msg9 + msg10);
	}
}
