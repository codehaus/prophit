package orbit.launcher;

import java.io.IOException;
import java.io.InputStream;

// For redirecting java input streams
//
public class InputStreamRedirector extends InputStream implements Redirector
{
	private ThreadRedirector redir;

	public InputStreamRedirector (InputStream defaultStream)
	{
		redir = new ThreadRedirector(defaultStream);
	}

	public synchronized void reset() throws IOException
	{
		getStream().reset();
	}

	public int read(byte[] param1, int param2, int param3) throws IOException
	{
		return getStream().read(param1, param2, param3);
	}

	public int read(byte[] param1) throws IOException
	{
		return getStream().read(param1);
	}

	public int read() throws IOException
	{
		return getStream().read();
	}

	public long skip(long param1) throws IOException
	{
		return getStream().skip(param1);
	}

	public int available() throws IOException
	{
		return getStream().available();
	}

	public void close() throws IOException
	{
		getStream().close();
	}

	public synchronized void mark(int param1)
	{
		getStream().mark(param1);
	}

	public boolean markSupported()
	{
		return getStream().markSupported();
	}

	public InputStream getStream()
	{
		return (InputStream) getResource();
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
