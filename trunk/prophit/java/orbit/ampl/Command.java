package orbit.ampl;

import orbit.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;

public abstract class Command
{
	private final String userName;

	public Command(String userName)
	{
		this.userName = userName;
	}
	
	public abstract void execute(Writer writer, BufferedReader reader);

	public void writeHeader(Writer writer, String command)
	{
		println(writer, userName);
		println(writer, command);
	}

	protected void flush(Writer writer)
	{
		try
		{
			writer.flush();
		}
		catch (IOException x)
		{
			throw handle(x);
		}			
	}

	protected void print(Writer writer, String str)
	{
		try
		{
			writer.write(str);
		}
		catch (IOException x)
		{
			throw handle(x);
		}			
	}

	protected void println(Writer writer, String str)
	{
		try
		{
			writer.write(str);writer.write('\n');
		}
		catch (IOException x)
		{
			throw handle(x);
		}			
	}

	protected String readLine(BufferedReader reader)
	{
		try
		{
			return reader.readLine();
		}
		catch (IOException x)
		{
 			throw handle(x);
		}			
	}
	
	protected String readSizedResult(BufferedReader reader)
	{
		try
		{
			int size = Integer.parseInt(reader.readLine());
			return read(reader, size);
		}
		catch (IOException x)
		{
			throw handle(x);
		}			
	}

	/**
	 * BufferedReader should be configured to read US-ASCII (bytes)
	 */
	protected String read(BufferedReader reader, int size)
	{
		try
		{
			char[] chars = new char[size];
			int pos = 0, read = 0;
			while ( ( read = reader.read(chars, pos, size - pos) ) != -1 && pos != size)
			{
				pos += read;
			}			
			if ( pos != size )
				warn("Unable to read " + size + " characters (only got " + pos + ")");
			return new String(chars);
		}
		catch (IOException x)
		{
			throw handle(x);
		}			
	}

	protected String readFully(BufferedReader reader)
	{
		try
		{
			char chars[] = new char[8192];
			StringBuffer sb = new StringBuffer();
			int read = 0;
			while ( ( read = reader.read(chars) ) != -1 )
			{
				sb.append(chars, 0, read);
			}
			return sb.toString();
		}
		catch (IOException x)
		{
			throw handle(x);
		}			
	}

	protected RuntimeException handle(IOException x)
	{
		return Util.handleAsNetwork(getClass(), x);
	}
	
	protected void warn(String message)
	{
		System.err.println("Warning : " + message);
	}
}
