package test;

import orbit.solver.Connection;
import orbit.solver.ConnectionFactory;

import java.io.*;
import java.util.ArrayList;

public class TestConnection
	implements Connection
{
	private final StringWriter writer = new StringWriter();
	private final BufferedReader reader;

	public TestConnection(String response)
	{
		try
		{
			reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(response.getBytes()), "US-ASCII"));
		}
		catch (java.io.UnsupportedEncodingException x)
		{
			x.printStackTrace();
			throw new RuntimeException(x.toString());
		}
	}

	public String getWrittenText()
	{
		return writer.toString();
	}
	
	public Writer getWriter()
	{
		return writer;
	}

	public BufferedReader getReader()
	{
		return reader;
	}

	public void close()
	{
		try
		{
			writer.close();
			reader.close();
		}
		catch (java.io.IOException x)
		{
			x.printStackTrace();
			throw new RuntimeException(x.toString());
		}
	}
	
	public static class Factory
		implements ConnectionFactory
	{
		private final String[] responses;
		private final ArrayList connections = new ArrayList();
		private int index = 0;
		
		public Factory(String[] responses)
		{
			this.responses = responses;
		}

		public TestConnection getConnection(int index)
		{
			return (TestConnection)connections.get(index);
		}
		
		public Connection newConnection()
		{
			TestConnection c = new TestConnection(responses[index++]);
			connections.add(c);
			return c;
		}
	}
}
