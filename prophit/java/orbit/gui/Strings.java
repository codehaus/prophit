package orbit.gui;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;

public class Strings
{
	private static Properties props;
	
	static
	{
		try {
			InputStream is = Strings.class.getClassLoader().getResourceAsStream("uitext.properties");
			props = new Properties();
			props.load(is);
		}
		catch (IOException x) {
			x.printStackTrace();
			throw new RuntimeException("Unable to open resource uitext.properties : " + x);
		}
	}

	public static String getUILabel(Class cls, String key)
	{
		String className = cls.getName();
		className = className.substring(className.lastIndexOf(".") + 1, className.length());
		return getProperty(className + "." + key, true);
	}

	public static String getMessage(Class cls, String key, Object arguments)
	{
		return getMessage(cls, key, new Object[]{ arguments });
	}

	public static String getMessage(Class cls, String key, Object[] arguments)
	{
		String className = cls.getName();
		className = className.substring(className.lastIndexOf(".") + 1, className.length());
		String formatStr = getProperty(className + "." + key, true);
		return MessageFormat.format(formatStr, arguments);
	}

	public static String getProperty(String key, boolean must)
	{
		String property = props.getProperty(key);
		if ( property == null && must )
			throw new RuntimeException("No property found in uitext.properties for key " + key);
		return property;
	}
}
