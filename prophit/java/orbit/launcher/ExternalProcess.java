package orbit.launcher;

import java.io.File;

public class ExternalProcess extends BaseProcess
{
	String[] cmdArray;

	public ExternalProcess(String[] cmdArray, BaseProcess parent)
	{
		super(parent);
		if (cmdArray == null || cmdArray.length == 0) 
		{
			throw new IllegalArgumentException("A command must be supplied to run an external process");
		}
		this.cmdArray = cmdArray;
	}

	public ExternalProcess (String[] cmdArray)
	{
		this(cmdArray, null);
	}

	public String getName()
	{
		return new File(cmdArray[0]).getName();
	}

	public String toString()
	{
		return "ExternalProcess-" + getPID();
	}

	public void doRun()
	{
		BaseProcess process = new AttachedExternalProcess(cmdArray, this);
		process.start();
		process.join();
	}

}
