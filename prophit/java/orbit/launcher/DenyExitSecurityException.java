package orbit.launcher;

public class DenyExitSecurityException extends SecurityException
{
	private int exitCode = 0;
	public DenyExitSecurityException (String msg, int exitCode)
	{
		super(msg);
		this.exitCode = exitCode;
	}

	public int getExitCode()
	{
		return exitCode;
	}
}
