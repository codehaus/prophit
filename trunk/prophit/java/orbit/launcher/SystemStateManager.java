package orbit.launcher;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Properties;

// This is a Singleton class responsible for setting up and managing
// the static system state that we need to redirect for the life of
// this process
//
public class SystemStateManager
{
	// Default resources for redirector - these streams can't be
	// preempted by someone doing a System.setOut/Err/In()
	public static final OutputStream stdout = new BufferedOutputStream(new FileOutputStream(FileDescriptor.out));
	public static final OutputStream stderr = new BufferedOutputStream(new FileOutputStream(FileDescriptor.err));
	public static final InputStream stdin = new BufferedInputStream(new FileInputStream(FileDescriptor.in));
	public static final Properties props = getDefaultProps();

	private OutputStreamRedirector stdoutRedir;
	private OutputStreamRedirector stderrRedir;
	private InputStreamRedirector stdinRedir;
	private PropertiesRedirector sysPropRedir;

	private static SystemStateManager instance = new SystemStateManager();

	private static Properties getDefaultProps()
	{
		// breaks use of launcher in ant script due to ant.bsh setting
		// of sys props, once we fix that, then this can be reverted to
		// be 'proper'
		/*
		//  Ensures we get the REAL system props no matter what other code has done to it
		Properties p = System.getProperties();
		System.setProperties(null);
		Properties p2 = System.getProperties();
		System.setProperties(p);
		return p2;
		*/
		Properties p = System.getProperties();
		if (p instanceof Redirector)
			p = (Properties) ((Redirector) p).getDefaultResource();
		return p;
	}

	private SystemStateManager ()
	{
		stdoutRedir = new OutputStreamRedirector(stdout);
		System.setOut(new PrintStream(stdoutRedir, true));
		stderrRedir = new OutputStreamRedirector(stderr);
		System.setErr(new PrintStream(stderrRedir, true));
		stdinRedir = new InputStreamRedirector(stdin);
		System.setIn(stdinRedir);
		sysPropRedir = new PropertiesRedirector(props);
		System.setProperties(sysPropRedir);
	}

	public static SystemStateManager getInstance()
	{
		return instance;
	}

	public void addStreamRedirector(ThreadGroup threadGroup, OutputStream stdout,
											  OutputStream stderr, InputStream stdin)
	{
		if (stdout == stdoutRedir)
			stdout = (OutputStream) stdoutRedir.getDefaultResource();
		stdoutRedir.addMap(threadGroup, stdout);
		if (stderr == stderrRedir)
			stderr = (OutputStream) stderrRedir.getDefaultResource();
		stderrRedir.addMap(threadGroup, stderr);
		if (stdin == stdinRedir)
			stdin = (InputStream) stdinRedir.getDefaultResource();
		stdinRedir.addMap(threadGroup, stdin);
	}

	public void removeStreamRedirector(ThreadGroup threadGroup)
	{
        stdoutRedir.removeMap(threadGroup);
        stderrRedir.removeMap(threadGroup);
        stdinRedir.removeMap(threadGroup);
	}

	public boolean isGroupOwnerOfCurrentStreams(ThreadGroup group)
	{
		// streams are only ever set together, so we only need to check
		// one of the redirectors
		return stdoutRedir.isGroupCurrentResourceOwner(group);
	}

	public void addPropertiesRedirector(ThreadGroup threadGroup, Properties sysProp)
	{
		if (sysProp == sysPropRedir)
			sysProp = (Properties) sysPropRedir.getDefaultResource();
		sysPropRedir.addMap(threadGroup, sysProp);
	}

	public void removePropertiesRedirector(ThreadGroup threadGroup)
	{
		sysPropRedir.removeMap(threadGroup);
	}

	public boolean isGroupOwnerOfCurrentProperties(ThreadGroup group)
	{
		return sysPropRedir.isGroupCurrentResourceOwner(group);
	}
}
