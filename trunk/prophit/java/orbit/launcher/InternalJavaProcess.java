package orbit.launcher;

import orbit.util.Log;

import org.apache.log4j.Category;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class InternalJavaProcess extends BaseProcess
{
	private static final Category LOG = Category.getInstance(InternalJavaProcess.class);

	private String className;
	private String[] classArgs;
	private Properties sysProp;

	private ClassLoader classLoader;
	private Class mainClass;

	public InternalJavaProcess(String className, List classArgs, List classPath, List jvmArgs, BaseProcess parent)
	{
		super(parent);
		try
		{
			this.className = className;

			this.classArgs = new String[classArgs.size()];
			int i =0;
			for (Iterator iter = classArgs.iterator(); iter.hasNext(); i++)
			{
				String each = (String) iter.next();
				this.classArgs[i] = each;
			}

			// If we are running in the current JVM, we need to extract
			// the system properties from the jvmArgs, and pass them to
			// runJavaClass
			// No need to backup sysprops as we have our own copy thanks
			// to redirection in BaseProcess
			sysProp = System.getProperties();
			for (Iterator iter = jvmArgs.iterator(); iter.hasNext();)
			{
				String each = (String) iter.next();
				if (each.startsWith("-D"))
				{
					String key = null;
					String value = null;
					int delimIdx = each.indexOf("=");
					if (delimIdx == -1)
					{
						key = each.substring(2);
						value = "";
					}
					else
					{
						key = each.substring(2, delimIdx);
						value = each.substring(delimIdx + 1);
					}
					sysProp.setProperty(key, value);
				}
			}

			URL[] urls = new URL[classPath.size()];
			i = 0;
			for (Iterator iter = classPath.iterator(); iter.hasNext(); i++)
			{
				String each = (String) iter.next();
				URL url = new File(each).toURL();
				urls[i] = url;
			}
			// Make sure parent ClassLoader is the bootstrap ClassLoader
			// and not the system ClassLoader.  We have a problem with
			// the system ClassLoader if the class we are trying to run
			// exists in the system ClassLoader, but classes it depends
			// on don't
			classLoader = new URLClassLoader(urls, null);
			mainClass = Class.forName( className, true, classLoader );
		}
		catch(MalformedURLException ex)
		{
			LOG.error("Invalid classpath entry: " + ex);
		}
		catch(ClassNotFoundException ex)
		{
			LOG.error("Class not found: " + ex);
		}
	}

	public String toString()
	{
		return "InternalJavaProcess-" + className + "-" + getPID();
	}

	public String getName()
	{
		return className;
	}

	public void doRun()
	{
		try
		{
			Method mainMethod = mainClass.getMethod("main", new Class[] { classArgs.getClass() });
			mainMethod.invoke(null, new Object[] {classArgs});
		}
		catch(ThreadDeath ex) {} //Just in case user kills before we start invoke
		catch (InvocationTargetException ex)
		{
			Throwable t = ex.getTargetException();
			if (t instanceof ThreadDeath)
			{
			}
			else if (t instanceof DenyExitSecurityException)
			{
				setRetcode(((DenyExitSecurityException) t).getExitCode());
			}
			else
			{
				LOG.error("Unexpected error in java process: " + t);
			}
		}
		catch(Exception ex)
		{
			LOG.error("Unexpected error in java process: " + ex);
		}
	}

}
