package orbit.ampl;

import java.io.*;

public class LogInputStream
	extends InputStream
{
	private StringBuffer sb = new StringBuffer();
	private final InputStream  input;
	private final OutputStream log;

	public LogInputStream(InputStream input, OutputStream log)
	{
		this.input = input;
		this.log = log;
	}

	public int read() throws IOException
	{
		int i = input.read();
		if ( i != -1 )
		{
			/*
			char c = new String(new byte[]{ (byte)i }).charAt(0);
			if ( c == '\n')
			{
				System.out.println(sb.toString());
				sb = new StringBuffer();
			}
			else
			{
				sb.append(c);
			}
			*/
			log.write(i);
		}
		return i;
	}
	
	/* Implement the rest of InputStream */
	public long skip(long l) throws IOException { return input.skip(l); }
    public int available() throws IOException   { return input.available(); }
    public void close() throws IOException      { input.close(); }
    public synchronized void mark(int i)        { input.mark(i); }
    public synchronized void reset() throws IOException { input.reset(); }
    public boolean markSupported()                      { return input.markSupported(); }
}
