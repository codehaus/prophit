package orbit.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

// For redirecting System.properties so that Launcher Processes can
// each get their own version wihile still referring to the one in
// System
//
public class PropertiesRedirector extends Properties implements Redirector
{
	private ThreadRedirector redir;

	public PropertiesRedirector(Properties defaultProperties)
	{
		redir = new ThreadRedirector(defaultProperties);
	}

	public synchronized Object setProperty(String param1, String param2)
	{
		return getProperties().setProperty(param1, param2);
	}

	public String getProperty(String param1, String param2)
	{
		return getProperties().getProperty(param1, param2);
	}

	public String getProperty(String param1)
	{
		return getProperties().getProperty(param1);
	}

	public synchronized void load(InputStream param1) throws IOException
	{
		getProperties().load(param1);
	}

	public void list(PrintWriter param1)
	{
		getProperties().list(param1);
	}

	public void list(PrintStream param1)
	{
		getProperties().list(param1);
	}

	public synchronized void save(OutputStream param1, String param2)
	{
		getProperties().save(param1, param2);
	}


	public synchronized void store(OutputStream param1, String param2) throws IOException
	{
		getProperties().store(param1, param2);
	}

	public Enumeration propertyNames()
	{
		return getProperties().propertyNames();
	}

	public synchronized int hashCode()
	{
		return getProperties().hashCode();
	}

	public synchronized Object put(Object param1, Object param2)
	{
		return getProperties().put(param1, param2);
	}

	public synchronized boolean equals(Object param1)
	{
		return getProperties().equals(param1);
	}

	public synchronized Object clone()
	{
		return getProperties().clone();
	}

	public synchronized String toString()
	{
		return getProperties().toString();
	}

	public synchronized Object get(Object param1)
	{
		return getProperties().get(param1);
	}

	public Collection values()
	{
		return getProperties().values();
	}

	public int size()
	{
		return getProperties().size();
	}

	public synchronized boolean contains(Object param1)
	{
		return getProperties().contains(param1);
	}

	public synchronized void clear()
	{
		getProperties().clear();
	}

	public synchronized Object remove(Object param1)
	{
		return getProperties().remove(param1);
	}

	public synchronized Enumeration keys()
	{
		return getProperties().keys();
	}

	public Set keySet()
	{
		return getProperties().keySet();
	}

	public Set entrySet()
	{
		return getProperties().entrySet();
	}

	public boolean isEmpty()
	{
		return getProperties().isEmpty();
	}

	public synchronized Enumeration elements()
	{
		return getProperties().elements();
	}

	public boolean containsValue(Object param1)
	{
		return getProperties().containsValue(param1);
	}

	public synchronized boolean containsKey(Object param1)
	{
		return getProperties().containsKey(param1);
	}

	public synchronized void putAll(Map param1)
	{
		getProperties().putAll(param1);
	}

	
	public Properties getProperties()
	{
		return (Properties) redir.getResource();
	}

	public void addMap(ThreadGroup param1, Object param2)
	{
		redir.addMap(param1, param2);
	}

	public void removeMap(ThreadGroup param1)
	{
		redir.removeMap(param1);
	}

	public Object getResource()
	{
		return redir.getResource();
	}

	public Object getDefaultResource()
	{
		return redir.getDefaultResource();
	}

	public boolean isGroupCurrentResourceOwner(ThreadGroup group)
	{
		return redir.isGroupCurrentResourceOwner(group);
	}
}
