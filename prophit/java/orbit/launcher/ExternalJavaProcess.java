package orbit.launcher;

import orbit.util.Log;

import org.apache.log4j.Category;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExternalJavaProcess extends BaseProcess
{
	private static final Category LOG = Category.getInstance(ExternalJavaProcess.class);

	private static boolean isHP = System.getProperty("os.name").toLowerCase().indexOf("hp-ux") != -1;
	private static String jvmVersion = System.getProperty("java.vm.version");

	private BaseProcess javaProcess;
	private String className;
	private List classArgs;
	private List classPath;
	private List jvmArgs;

	private boolean isDebug = false;
	private boolean isDebugSuspend = false;
	private String debugAddress = null;

	public ExternalJavaProcess(String className, List classArgs, List classPath, List jvmArgs, BaseProcess parent)
	{
		super(parent);
		this.className = className;
		this.classArgs = classArgs;
		this.classPath = classPath;
		this.jvmArgs = jvmArgs;
	}

	private void addDebugFlags(List processCmd)
	{
			String address = "";
			String transport = "";
			// If a debug address is supplied and its digits only, then
			// setup a socket address, otherwise make it a shmem addres.
			// If no address supplied, setup a random socket address
			if (debugAddress != null)
			{
				address = ",address=" + debugAddress;
				try
				{
					Integer.parseInt(debugAddress);
					transport = "transport=dt_socket";
				}
				catch (NumberFormatException e)
				{
					transport = "transport=dt_shmem";
				}
			}
			else
			{
				transport = "transport=dt_socket";
				address = "";
			}

			// We only want to use the -classic flag for debugging for
			// jvms < 1.3.1
			if (jvmVersion.compareTo("1.3.1") < 0)
			{
				processCmd.add("-classic");
			}

			processCmd.add("-Xdebug");
			processCmd.add("-Xnoagent");
			String runjdwp = "-Xrunjdwp:" + transport + address + ",server=y,suspend=";
			if (isDebugSuspend)
				runjdwp += "y";
			else
				runjdwp += "n";
			processCmd.add(runjdwp);
	}		

	public void doRun()
	{
		int retcode = 0;

		List processCmd = new ArrayList();

		// For external java processes, always use java and not javaw -
		// runjava.exe provides the root console needed under windows to
		// prevent those annoying console windows whenever a console app
		// is run
		String java_home = System.getProperty("java.home");
		processCmd.add(java_home + File.separator + "bin" + File.separator + "java");

		if (isDebug)
			addDebugFlags(processCmd);
			
		// If the user specifies the -classic flag, it needs to come
		// first
		if (jvmArgs.remove("-classic"))
			processCmd.add("-classic");

		// Other JVM args need to come after the jpda ones
		for (Iterator iter = jvmArgs.iterator(); iter.hasNext();)
		{
			String each = (String) iter.next();
			processCmd.add(each);
		}

		if (isHP)
			processCmd.add("-XdoCloseWithReadPending");

		StringBuffer classpath = new StringBuffer();
		for (Iterator iter = classPath.iterator(); iter.hasNext();)
		{
			String each = iter.next().toString();
			classpath.append(each);
			if (iter.hasNext())
				classpath.append(File.pathSeparator);
		}
		if (classPath.size() > 0)
		{
			processCmd.add("-classpath");
			processCmd.add(classpath.toString());
		}

		processCmd.add(className);

		for (Iterator iter = classArgs.iterator(); iter.hasNext();)
		{
			String each = (String) iter.next();
			processCmd.add(each);
		}

		String[] cmdArray = (String[]) processCmd.toArray(new String[processCmd.size()]);
		
		javaProcess = new ExternalProcess(cmdArray, this);
		javaProcess.setStreams(rawStdout, rawStderr, rawStdin);
		javaProcess.setWorkingDirectory(getWorkingDirectory());
		javaProcess.setEnvironment(getEnvironment());
		javaProcess.start();
		javaProcess.join();
		retcode = javaProcess.getRetcode();
	}

	public String getName()
	{
		return className;
	}

	public String toString()
	{
		return "ExternalJavaProcess-" + className + "-" + getPID();
	}

	public String getDebugAddress()
	{
		return debugAddress;
	}

	public boolean isDebug()
	{
		return isDebug;
	}

	public void setDebugAddress(String debugAddress)
	{
		this.debugAddress = debugAddress;
	}

	public void setIsDebug(boolean isDebug)
	{
		this.isDebug = isDebug;
	}

	public boolean isDebugSuspend()
	{
		return isDebugSuspend;
	}

	public void setIsDebugSuspend(boolean isDebugSuspend)
	{
		this.isDebugSuspend = isDebugSuspend;
	}

}
