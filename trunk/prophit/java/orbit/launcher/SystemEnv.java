package orbit.launcher;

import orbit.util.Log;

import org.apache.log4j.Category;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

// This class abstracts away the process environment (environment
// variables) for the system.  It exec's a native system command and
// parses its stdout in order to produce a name-> value mapping of the
// environment vars for that system.  The command it executes must
// produce a listing of the form "envname=envvalue" on each line.  if
// the default commands aren't working for you, you can try and set
// the system property cs.env.command to a native command that
// produces the correct output.
//
public class SystemEnv implements Map
{
	private static final Category LOG = Category.getInstance(SystemEnv.class);
	
	private static Map systemEnv = null;
	private Map env = null;

	private static boolean isWindows =
		System.getProperty("os.name").toLowerCase().indexOf("windows") != -1;

	public SystemEnv(boolean initialize)
	{
		env = getPlatformMap();
		if (initialize)
		{
			initialize();
			if (systemEnv != null) 
				env.putAll(systemEnv);
		}
	}

	public SystemEnv(Map rhs)
	{
		env = getPlatformMap();
		env.putAll(rhs);		
	}
	
	private static Map getPlatformMap()
	{
		// Under windows, env variables are not case sensitive
		if (isWindows)
			return new TreeMap(String.CASE_INSENSITIVE_ORDER);
		else
			return new TreeMap();
	}

	private static void initialize()
	{
		synchronized(SystemEnv.class)
		{
			String cmd = "";
			try
			{
				if (systemEnv == null) 
				{
					Map newEnv = getPlatformMap();
					// TODO: preference
					if (isWindows) 
						cmd = "cmd /c set";
					else
						cmd = "/usr/bin/env";
					Process process = Runtime.getRuntime().exec(cmd);
					BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(process.getInputStream())));
					for (String line = reader.readLine(); line != null; line = reader.readLine())
					{
						line = line.trim();
						int delimIdx = line.indexOf('=');
						if (delimIdx == -1)
							continue;
						String key = line.substring(0, delimIdx);
						String value = line.substring(delimIdx + 1);
						newEnv.put(key, value);
					}
					systemEnv = newEnv;
				}
			}
			catch (IOException ex)
			{
				LOG.error("IOException while creating System environment: " + ex);
				LOG.error("Check that the system shell used is available: " + cmd);
			}
			catch (Throwable ex)
			{
				LOG.error("Unknown problem while creating System environment: " + ex);
			}
		}
	}

	public String[] toEnvArray()
	{
		String[] result = new String[env.size()];
		int i = 0;
		for (Iterator iter = env.entrySet().iterator(); iter.hasNext(); i++)
		{
			Map.Entry each = (Map.Entry) iter.next();
			String key = each.getKey().toString();
			String value = each.getValue().toString();
			result[i] = key + "=" + value;
		}
		return result;
	}

	public String toString()
	{
		StringBuffer result = new StringBuffer(1000);
		result.append("{");
		for (Iterator iter = env.entrySet().iterator(); iter.hasNext(); )
		{
			Map.Entry each = (Map.Entry) iter.next();
			String key = each.getKey().toString();
			String value = each.getValue().toString();
			result.append(key);
			result.append("=");
			result.append(value);
			if (iter.hasNext())
				result.append(", ");
		}
		result.append("}");
		return result.toString();
	}

	public int hashCode()
	{
		return env.hashCode();
	}

	public Object put(Object param1, Object param2)
	{
		return env.put(param1, param2);
	}

	public boolean equals(Object param1)
	{
		return env.equals(param1);
	}

	public Object get(Object param1)
	{
		return env.get(param1);
	}

	public Collection values()
	{
		return env.values();
	}

	public int size()
	{
	 return env.size();
	}

	public void clear()
	{
		env.clear();
	}

	public Object remove(Object param1)
	{
		return env.remove(param1);
	}

	public Set keySet()
	{
		return env.keySet();
	}

	public Set entrySet()
	{
		return env.entrySet();
	}

	public boolean isEmpty()
	{
		return env.isEmpty();
	}

	public boolean containsValue(Object param1)
	{
		return env.containsValue(param1);
	}

	public boolean containsKey(Object param1)
	{
		return env.containsKey(param1);
	}

	public void putAll(Map param1)
	{
		env.putAll(param1);
	}

}
