package orbit.launcher;

import java.io.IOException;
import java.io.OutputStream;


// For redirecting java output streams so that in process java
// processes can still use System.out and have their output directed
// to the right place.
//
public class OutputStreamRedirector extends OutputStream implements Redirector
{
	private ThreadRedirector redir;

	public OutputStreamRedirector (OutputStream defaultStream)
	{
		redir = new ThreadRedirector(defaultStream);
	}

	public void flush() throws IOException
	{
		getStream().flush();
	}

	public void write(byte[] param1, int param2, int param3) throws IOException
	{
		getStream().write(param1, param2, param3);
	}

	public void write(byte[] param1) throws IOException
	{
		getStream().write(param1);
	}

	public void write(int param1) throws IOException
	{
		getStream().write(param1);
	}

	public void close() throws IOException
	{
		getStream().close();
	}

	public OutputStream getStream()
	{
		return (OutputStream) redir.getResource();
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
