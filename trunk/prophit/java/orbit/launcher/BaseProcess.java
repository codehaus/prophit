package orbit.launcher;

import orbit.util.Log;

import org.apache.log4j.Category;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public abstract class BaseProcess implements LauncherProcess, ProcessListener, Runnable
{
	private static final Category LOG = Category.getInstance(BaseProcess.class);

	public static final String THREADGROUP_NAME_PREFIX = "Process-";
	// TODO: preference
	public static final int DEFAULT_KILLHOOK_TIMEOUT = 20000;

	private static int nextPid = 0;

	private List processListeners = Collections.synchronizedList(new ArrayList());
	private List childProcesses = Collections.synchronizedList(new ArrayList());
	private Runnable killHook; 

	protected BaseProcess parentProcess;
	private PropertiesOverride props;
	private int retcode = 0;
	private int pid = 0;
	private Thread thread;
	private ThreadGroup theThreadGroup;
	private Map scriptContext = Collections.synchronizedMap(new HashMap());
	private String workingDir;
	private SystemEnv sysenv;
	private boolean verbose = false;

	private Object exitLock = new Object();
	private boolean shuttingDown = false;

	// The streams the process will read/write from/to
	protected OutputStream rawStdout;
	protected OutputStream rawStderr;
	protected InputStream rawStdin;
	private BufferedWriter stdout;
	private BufferedWriter stderr;
	private BufferedReader stdin;
	private String encoding;

	public BaseProcess(BaseProcess parent)
	{
		synchronized(BaseProcess.class)
		{
			pid = nextPid++;
		}

		theThreadGroup = new ThreadGroup(THREADGROUP_NAME_PREFIX + Integer.toString(pid));
		thread = new Thread(theThreadGroup, this, theThreadGroup.getName());

		// Always inherit the parent's current dir, or launchers current
		// dir if no parent
		setWorkingDirectory(System.getProperty("user.dir"));
		
		// Setup default streams
		// TODO: preference
		this.encoding = "UTF-8";
		setStreams(SystemStateManager.stdout, SystemStateManager.stderr, SystemStateManager.stdin);
			
		// Give process its own copy of System.properties - this will be
		// a copy of the parent processes (or default System.props)
		SystemStateManager.getInstance().addPropertiesRedirector(theThreadGroup, (Properties) System.getProperties().clone());

		setParentProcess(parent);
	}
	
	// Makes the child inherit the correct values from a parent
	public void setParentProcess(BaseProcess parent)
	{
		this.parentProcess = parent;
		if (parent != null)
		{
			props = (PropertiesOverride) parent.getProps().clone();
			parent.addChild(this);
			setStreams(parent.rawStdout, parent.rawStderr, parent.rawStdin);
			setWorkingDirectory(parent.getWorkingDirectory());
			setEnvironment(new SystemEnv(parent.getEnvironment()));
		}
		else
		{
			// To help out usage of ExternalProcess from outside of launcher
			props = new PropertiesOverride(new Properties(), System.getProperties());
		}
		if (sysenv == null)
			sysenv = new SystemEnv(true);
	}
	
	public Map getContext()
	{
		return scriptContext;
	}

	public ThreadGroup getThreadGroup()
	{
		return theThreadGroup;
	}

	public int getPID()
	{
		return pid;
	}

	public abstract String getName();

	public String toString()
	{
		return "BaseProcess-" + pid;
	}

	public void setKillHook(Runnable killHook)
	{
		this.killHook = killHook;
	}

	public Runnable getKillHook()
	{
		return killHook;
	}

	public void addProcessListener(ProcessListener listener)
	{
		processListeners.add(listener);
	}

	public void removeProcessListener(ProcessListener listener)
	{
		processListeners.remove(listener);
	}

	public void receiveStart(LauncherProcess process)
	{
	}
	
	public void receiveExit(LauncherProcess process)
	{
		int retcode = process.getRetcode();

		if (verbose)
		{
			if (retcode == 0)
				LOG.info("Process [" + process.toString() + "] completed normally");
			else
				LOG.error("Process [" + process.toString() + "] terminated with error code: " + retcode);
		}
		
		// Processes error code is last childs error code only if
		// process didn't have an error of its own
		if (getRetcode() == 0)
			setRetcode(retcode);

		removeChild(process);
	}

	protected void addChild(LauncherProcess child)
	{
		childProcesses.add(child);
		child.addProcessListener(this);
	}

	protected void removeChild(LauncherProcess child)
	{
		childProcesses.remove(child);
	}

	public int getRetcode()
	{
		return retcode;
	}

	public void setRetcode(int retcode)
	{
		this.retcode = retcode;
	}

	public boolean isAlive()
	{
		return !shuttingDown && thread.isAlive();
	}

	public void join()
	{
		try
		{
			thread.join();
		}
		catch (InterruptedException ex) {}
	}

	public void start()
	{
		for (Iterator iter = processListeners.iterator(); iter.hasNext(); )
		{
			ProcessListener each = (ProcessListener) iter.next();
			each.receiveStart(this);
		}
		thread.start();
	}

	public abstract void doRun();

	public final void run()
	{
		if (workingDir != null)
			System.setProperty("user.dir", workingDir);
		try
		{
			doRun();
		}
		catch (ThreadDeath t)
		{
			Log.debug(LOG, "The process " + toString() + " was killed.");
		}
		catch (Throwable t)
		{
			LOG.error("The process " + toString() + " threw an unexpected exception: " + t.getMessage());
			Log.error(LOG, t);
		}
		while (childProcesses.size() > 0)
		{
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException ex)
			{
				Log.debug(LOG, "InterruptedException while waiting for child processes to exit: ", ex.getMessage());
				Log.error(LOG, ex);
			}
		}
		exit(true);
	}

	private boolean acquireExitLock()
	{
		// Prevent IllegalMonitorStateException
		if (shuttingDown)
			return false;
		synchronized (exitLock)
		{
			if (shuttingDown)
				return false;
			shuttingDown = true;
			return true;
		}
	}

	public final void kill()
	{
		if (isAlive())
		{
			retcode = 1;

			try
			{
				if (killHook != null)
				{
					Thread killHookThread = new Thread(killHook);
					killHookThread.start();
					killHookThread.join(DEFAULT_KILLHOOK_TIMEOUT);
					if (killHookThread.isAlive())
						LOG.error("Timeout while waiting for kill hook to kill process, resuming kill sequence");
				}
			}
			catch (Throwable ex)
			{
				LOG.error("Unexpected exception while running killHook, resuming kill sequence: " + ex);
			}

			List copy = new ArrayList();
			copy.addAll(childProcesses);
			for (Iterator iter = copy.iterator(); iter.hasNext();)
			{
				LauncherProcess process = (LauncherProcess) iter.next();
				process.kill();
			}

			// Give the child processes a chance to die, then call exit() to cleanup
			try { Thread.sleep(500); } catch(InterruptedException ex) {}

			exit(false);
		}
	}
	
	private void exit(final boolean inRun)
	{
		// This needs to be here in case a process was killed, and the
		// act of killing it caused it to exit gracefully.  Exit gets
		// called in both cases, but we only ever want it to get called
		// once per process.
		if (! acquireExitLock())
			return;

		SystemStateManager.getInstance().removeStreamRedirector(getThreadGroup());
		while(processListeners.size() > 0)
		{
			ProcessListener each = (ProcessListener) processListeners.remove(processListeners.size() - 1);
			try
			{
				each.receiveExit(this);
			}
			catch (Throwable t)
			{
				LOG.error("A process listener threw an unexpected exception: " + t.getMessage());
				Log.error(LOG, t);
			}
		}
			
		SystemStateManager.getInstance().removePropertiesRedirector(theThreadGroup);
			
		//	If we are cleaning up as a result of the current thread
		// exiting, need to destroy the threadgroup async so that
		// this thread can exit first, otherwise, tg will throw
		// exception as its thread is still running.  We do this on
		// kills too to make for cleaner code.
		Runnable runnable = new Runnable() {
			public void run()
			{
				try
				{
					// First kill all threads, except if we are in exit() due to run() exiting cleanly,
					// we don't want to kill the thread that represents this process because we don't
					// want to call Thread.stop() unless absolutely necessary
					Thread[] activeThreads = new Thread[theThreadGroup.activeCount()];
					theThreadGroup.enumerate(activeThreads);
					for (int i = 0; i < activeThreads.length; i++)
					{
						Thread thread = activeThreads[i];
						boolean nokill = inRun && thread == BaseProcess.this.thread;
						if (thread != null && thread.isAlive() && !nokill)
						{
							Log.debug(LOG, "Killing thread: " + thread.toString());
							thread.interrupt();
							thread.stop();
						}
					}
					
					// All threads are in the process of exiting, so we wait for them all to finish
					// before cleaning up the ThreadGroups (otherwise if the thread is still running
					// when we clean up the thread group, we get an IllegalStateException, and the
					// thread group does not get cleaned up) 
					for (int i = 0; i < activeThreads.length; i++)
					{
						Thread thread = activeThreads[i];
						if (thread != null && thread.isAlive())
						{
							// Timeout so that the cleanup thread goes away eventually even if there are
							// badly behaved threads which swallow ThreadDeath.  If a timeout occurs, worst
							// case is that the threadgroups don't end up getting cleaned up.
							try
							{
								thread.join(10000);
							}
							catch (InterruptedException e)
							{
								Log.debug(LOG, "Interruption while waiting for thread to die in destroy ThreadGroup thread");
							}
							if (thread.isAlive())
								Log.debug(LOG, "Unable to kill thread - it may be swallowing ThreadDeath: ", thread);
						}
					}

					// Now that all contained threads are dead, cleanup all the threadgroups
					ThreadGroup[] threadGroups = new ThreadGroup[theThreadGroup.activeGroupCount() + 1];
					theThreadGroup.enumerate(threadGroups);
					threadGroups[threadGroups.length - 1] = theThreadGroup;
					for (int i = 0; i < threadGroups.length; i++)
					{
						ThreadGroup group = threadGroups[i];
						if (group != null && ! group.isDestroyed())
						{
							try
							{
								group.destroy();
							}
							catch (IllegalThreadStateException ex)
							{
								Log.debug(LOG, "IllegalThreadStateException in destroy ThreadGroup thread: ", theThreadGroup);
								Log.error(LOG, ex);
							}
						}
					}
				}
				catch (Exception ex)
				{
					Log.debug(LOG, "Unexpected Exception in destroy ThreadGroup thread: ", theThreadGroup);
					Log.error(LOG, ex);
				}
			}
		};		
		Thread destroyThread = new Thread(ThreadUtil.getMainThreadGroup(), runnable, toString() + "-DestroyThreadGroupThread");
		destroyThread.start();

	}

	public void println(String msg)
	{
		try
		{
			stdout.write(msg);
			stdout.newLine();
			stdout.flush();
		}
		catch (IOException ex)
		{
			Log.debug(LOG, "IOException while writing to stdout", ex.getMessage());
			Log.error(LOG, ex);
		}
	}

	public void error(String msg)
	{
		try
		{
			stderr.write(msg);
			stderr.newLine();
			stderr.flush();
		}
		catch (IOException ex)
		{
			Log.debug(LOG, "IOException while writing to stderr", ex.getMessage());
			Log.error(LOG, ex);
		}
	}

	public String readline()
	{
		String result = null;
		try
		{
			result = stdin.readLine();
		}
		catch (IOException ex)
		{
			Log.debug(LOG, "IOException while reading from stdin", ex);
		}
		return result;
	}

	public void setStreams(OutputStream stdout, OutputStream stderr, InputStream stdin)
	{
		rawStdout = stdout;
		rawStderr = stderr;
		rawStdin = stdin;

		SystemStateManager.getInstance().addStreamRedirector(getThreadGroup(),
																			  stdout, stderr, stdin);

		try
		{
			this.stdout = new BufferedWriter(new OutputStreamWriter(stdout, encoding));
			this.stderr = new BufferedWriter(new OutputStreamWriter(stderr, encoding));
			this.stdin = new BufferedReader(new InputStreamReader(stdin, encoding));
		}
		catch (UnsupportedEncodingException ex)
		{
			LOG.error("Invalid encoding: " + encoding + ", using system default");
			encoding = new InputStreamReader(System.in).getEncoding();
			this.stdout = new BufferedWriter(new OutputStreamWriter(stdout));
			this.stderr = new BufferedWriter(new OutputStreamWriter(stderr));
			this.stdin = new BufferedReader(new InputStreamReader(stdin));
		}
	}

	public OutputStream getOutputStream()
	{
		return rawStdout;
	}

	public OutputStream getErrorStream()
	{
		return rawStderr;
	}

	public InputStream getInputStream()
	{
		return rawStdin;
	}

	public void setWorkingDirectory(String dir)
	{
		if (dir == null || dir.trim().length() == 0)
			throw new IllegalArgumentException("Cannot set the working directory for a process to null");

		String newDir = null;
		File directory = new File(dir);

		if (! directory.isAbsolute())
		{
			if (workingDir == null || workingDir.trim().length() == 0)
				directory = new File(System.getProperty("user.dir"), dir);
			else
				directory = new File(workingDir, dir);
		}
		try
		{
			newDir = directory.getCanonicalPath();
		}
		catch (IOException ex)
		{
			LOG.error("Unhandled IOException while setting working dir" + ex);
		}

		if (isAlive())
		{
			// if the process is running, and this method is called, we
			// need to ensure that it is called from a thread within this
			// process' threadgroup so that the redirector sets the
			// correct System.properties, otherwise java code that runs
			// in the process that uses user.dir to determine cwd, will
			// not work correctly, so we issue a warning.
			if (SystemStateManager.getInstance().isGroupOwnerOfCurrentProperties(theThreadGroup))
			{
				workingDir = newDir;
				System.setProperty("user.dir", workingDir);
			}
			else
			{
				LOG.error("WARNING: Setting the working directory on a running process has no effect");
			}
		}
		else
		{
			workingDir = newDir;
		}
	}

	public String getWorkingDirectory()
	{
		return workingDir;
	}

	public void setEnvironment(SystemEnv env)
	{
		this.sysenv = env;
	}

	public SystemEnv getEnvironment()
	{
		return sysenv;
	}

	public boolean isVerbose()
	{
		return verbose;
	}

	public void setVerbose(boolean verbose)
	{
		this.verbose = verbose;
	}

	public Properties getProperties()
	{
		return props;
	}
	
	public PropertiesOverride getProps()
	{
		return props;
	}

	public void setProps(PropertiesOverride properties)
	{
		this.props = properties;
	}

}
