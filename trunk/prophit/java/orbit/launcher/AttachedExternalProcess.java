package orbit.launcher;

import orbit.util.Log;

import org.apache.log4j.Category;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AttachedExternalProcess extends BaseProcess
{
	public static Category LOG = Category.getInstance(AttachedExternalProcess.class);

	class ProcessShutdownHook extends Thread
	{
		AttachedExternalProcess process;
		boolean doNothing = false;

		public ProcessShutdownHook(ThreadGroup tg, AttachedExternalProcess process)
		{
			super(tg, tg.getName() + "-ProcessShutdownHook");
			this.process = process;
		}

		public void setDoNothing()
		{
			doNothing = true;
		}

		public void run()
		{
			if (! doNothing)
				process.kill();
		}
	}

	private String[] processCmd;
	private Process process;
	private ProcessStreamMapper streamMapper;
	private ProcessShutdownHook shutdownHook;

	public AttachedExternalProcess (String[] cmdArray, BaseProcess parent)
	{
		super(parent);

		if (cmdArray == null || cmdArray.length == 0) 
		{
			throw new IllegalArgumentException("A command must be supplied to run an external process");
		}
		
		this.processCmd = cmdArray;

		super.setKillHook(new Runnable() {
				public void run()
				{
					doKill();
				}
			});
		
		streamMapper = new ProcessStreamMapper();
		streamMapper.setInternalStreams(rawStdout, rawStderr, rawStdin);
	}

	public AttachedExternalProcess (String[] cmdArray)
	{
		this(cmdArray, null);
	}

	public void setStreams(OutputStream stdout, OutputStream stderr, InputStream stdin)
	{
		super.setStreams(stdout, stderr, stdin);
		if (streamMapper != null) 
		{
			streamMapper.kill();
		}
		streamMapper = new ProcessStreamMapper();
		streamMapper.setInternalStreams(rawStdout, rawStderr, rawStdin);
	}

	public String toString()
	{
		return "AttachedExternalProcess-" + getPID();
	}

	public String getName()
	{
		return new File(processCmd[0]).getName();
	}

	public void doRun()
	{
		if (process != null)
		{
			throw new RuntimeException("Cannot restart a process");
		}
		try
		{
			StringBuffer cmdString = new StringBuffer();
			for (int i = 0; i < processCmd.length; i++)
			{
				cmdString.append(processCmd[i]);
				cmdString.append(" ");
			}
			LOG.info("Running process: " + cmdString.toString());
			
			File directory = new File(getWorkingDirectory());
			
			shutdownHook = new ProcessShutdownHook(getThreadGroup(), this);
			SystemEnv env = getEnvironment();
			String[] processEnv = null;
			if (env != null)
			{
				processEnv = env.toEnvArray();
			}

			process = Runtime.getRuntime().exec(processCmd, processEnv, directory);
			Runtime.getRuntime().addShutdownHook(shutdownHook);
			streamMapper.setExternalStreams(new BufferedInputStream(process.getInputStream()),
													  new BufferedInputStream(process.getErrorStream()),
													  new BufferedOutputStream(process.getOutputStream()));
			streamMapper.start();
			setRetcode(process.waitFor());
			cleanup();
		}
		catch (IOException ex)
		{
			LOG.error("Unexpected IOException while running external process: " + ex);
			setRetcode(-1);
		}
		catch (InterruptedException ex)
		{
			Log.debug(LOG, "Unexpected InterruptedException while running external process: ", ex);
			setRetcode(-1);
		}
	}
	
	private void doKill()
	{
		if (process != null) 
			process.destroy();
		join();
	}

	public void cleanup()
	{
		try
		{
			Runtime.getRuntime().removeShutdownHook(shutdownHook);
			// Have to remove the thread from the threadgroup or
			// threadgroup.destroy() barfs
			shutdownHook.setDoNothing();
			shutdownHook.start();
			shutdownHook.join();
		}
		catch (InterruptedException ex)
		{
			Log.debug(LOG, "Cleanup of shutdown hook interrupted");
		}
		catch (IllegalStateException ex)
		{
			// Swallow - only happens when we are cleaning up cuz of exit
		}
		streamMapper.exit();
	}
}
