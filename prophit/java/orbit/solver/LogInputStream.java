package orbit.solver;

import java.io.*;

public class LogInputStream
	extends InputStream
{
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
			log.write(i);
		return i;
	}
}
