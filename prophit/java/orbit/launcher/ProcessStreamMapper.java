package orbit.launcher;

import java.io.InputStream;
import java.io.OutputStream;

public class ProcessStreamMapper
{
	private StreamPipe stdoutPipe;
	private StreamPipe stderrPipe;
	private StreamPipe stdinPipe;
	private OutputStream internalStdout;
	private OutputStream internalStderr;
	private InputStream internalStdin;
	private InputStream externalStdout;
	private InputStream externalStderr;
	private OutputStream externalStdin;

	public ProcessStreamMapper ()
	{
	}

	public void setInternalStreams(OutputStream stdout, OutputStream stderr, InputStream stdin)
	{
		internalStdout = stdout;
		internalStderr = stderr;
		internalStdin = stdin;
	}

	public void setExternalStreams(InputStream stdout, InputStream stderr, OutputStream stdin)
	{
		externalStdout = stdout;
		externalStderr = stderr;
		externalStdin = stdin;
	}

	public void start()
	{
		stdoutPipe = new StreamPipe(externalStdout, internalStdout, true, false);
		stdoutPipe.start();
		stderrPipe = new StreamPipe(externalStderr, internalStderr, true, false);
		stderrPipe.start();
		stdinPipe = new StreamPipe(internalStdin, externalStdin, false, false);
		stdinPipe.start();
	}

	public void exit()
	{
		if (stdoutPipe != null)
			stdoutPipe.exit();
		if (stderrPipe != null)
			stderrPipe.exit();
		if (stdinPipe != null)
			stdinPipe.exit();
	}

	public void kill()
	{
		if (stdoutPipe != null)
			stdoutPipe.kill();
		if (stderrPipe != null)
			stderrPipe.kill();
		if (stdinPipe != null)
			stdinPipe.kill();
	}
}
