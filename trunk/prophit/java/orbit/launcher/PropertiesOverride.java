package orbit.launcher;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Simple class to create a property set that has both a set of defaults, 
 * and a set of overrides which cannot be replaced.  This makes it easy to
 * use System.properties as an absolute override no matter what properties
 * get passed in programatically when creating new launcher processes.
 */
public class PropertiesOverride extends Properties
{
	private Properties overrides;
	private Properties defaults;

	/**
	 * Creates the Properties object
	 * @param defaults The default properties to use
	 * @param overrides The override properties to use
	 */
	public PropertiesOverride(Properties defaults, Properties overrides)
	{
		this.overrides = overrides;
		if (defaults != null)
		{
			putAll(defaults);
			this.defaults = defaults;
		}
		if (overrides != null)
		{
			this.overrides = overrides;
			for (Enumeration e = overrides.propertyNames(); e.hasMoreElements(); )
			{
				String key = (String) e.nextElement();
				super.put(key, overrides.get(key));
			}
		}
	}

	/**
	 * Allows one to set an override property directly
	 * @param key The property's key
	 * @param value The property's value
	 * @return String The old value of key, null if none
	 */
	public String setOverride(String key, String value)
	{
		super.put(key, value);
		return (String) overrides.put(key, value);
	}
	
	/**
	 * Checks if key is an overriden property
	 * @param key
	 * @return boolean
	 */
	public boolean isOverride(String key)
	{
		return overrides != null && overrides.containsKey(key);
	}
	
	/**
	 * Checks if key is an defaulted property
	 * @param key
	 * @return boolean
	 */
	public boolean isDefault(String key)
	{
		boolean result = false;
		if (defaults != null)
		{
			String def = defaults.getProperty(key);
			String prop = getProperty(key);
			result = def == prop;
		}
		return result;
	}
	
	/**
	 * @see java.util.Dictionary#put(Object, Object)
	 */
	public Object put(Object key, Object value)
	{
		if (overrides == null || !overrides.containsKey(key))
			return super.put(key, value);
		else
			return null;
	}
	
	/** This method replaces each properties value with variables
	 * references, ${varname}, expanded by a lookup in the Properties.
	 *	If the variable does not exist in the map, it is left unexpanded
	 *	in the result string.
	 */
	public void expandVariables()
	{
		for(Iterator iter = entrySet().iterator(); iter.hasNext(); )
		{
			Map.Entry each = (Map.Entry) iter.next();
			String key = (String) each.getKey();
			String value = (String) each.getValue();
			if (each.getValue() != null)
			{
				String text = replaceVars(value);
				if (defaults != null && defaults.getProperty(key) == value)
					defaults.put(key, text);
				super.put(key, text); 
			}
		}
	}

	private String replaceVars(String src)
	{
		int end = -1;
		int start = src.indexOf("${");

		if (start == -1)
			return src;

		StringBuffer result = new StringBuffer();
		while (start != -1)
		{
			result.append(src.substring(end + 1, start));

			end = src.indexOf("}", start + 2);
			if (end == -1)
				throw new RuntimeException("Incomplete variable reference in property file: " + src);

			String varName = src.substring(start + 2, end);
			String varValue = getProperty(varName);

			if (varValue == null)
				varValue = src.substring(start, end + 1);
			result.append(varValue);

			start = src.indexOf("${", end + 1);
		}
		if (end + 1 <= src.length())
			result.append(src.substring(end + 1));

		return result.toString();
	}

	public boolean getBool(String key)
	{
		return Boolean.valueOf(getProperty(key)).booleanValue();
	}

	public int getInt(String key)
	{
		int result = -1;
		String val = getProperty(key);
		if (val != null)
		{
			result = Integer.parseInt(val);
		}
		return result;
	}

	public Properties getDefaults()
	{
		return defaults;
	}

}
