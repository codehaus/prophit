package orbit.ampl;

import orbit.util.Util;
import orbit.util.Log;

import org.apache.log4j.Category;

import java.io.*;
import java.net.Socket;

public class SocketConnection
	implements Connection
{
	public static Category LOG = Category.getInstance(SocketConnection.class);

	private static int COUNTER = 0;
	private static String US_ASCII = "US-ASCII";
	
	private final Socket socket;
	
	private Writer                writer   = null;
	private Reader                reader   = null;
	private ByteArrayOutputStream inputOS  = null;
	private ByteArrayOutputStream outputOS = null;
	
	private SocketConnection(Socket socket)
	{
		this.socket = socket;
	}

	public Writer getWriter()
	{
		open();
		return writer;
	}

	public Reader getReader()
	{
		open();
		return reader;
	}

	public void close()
	{
		try
		{
			socket.close();
			if ( LOG.isDebugEnabled() )
			{
				String input = new String(inputOS.toByteArray(), US_ASCII);
				String output = new String(outputOS.toByteArray(), US_ASCII);
				int index = COUNTER++;
				LOG.debug("Input [" + index + "]");
				LOG.debug(input);
				LOG.debug("Output [" + index + "]");
				LOG.debug(output);
			}
		}
		catch (IOException x)
		{
			throw Util.handle(getClass(), x);
		}
	}

	private synchronized void open()
	{
		if ( writer != null && reader != null )
			return;

		try
		{
			inputOS  = new ByteArrayOutputStream();
			outputOS = new ByteArrayOutputStream();
			reader = new InputStreamReader(new LogInputStream(socket.getInputStream(), inputOS), US_ASCII);
			writer = new OutputStreamWriter(new TeeOutputStream(socket.getOutputStream(), outputOS), US_ASCII);
		}
		catch (IOException x)
		{
			throw Util.handle(getClass(), x);
		}
	}

	public static class Factory
		implements ConnectionFactory
	{
		private final String host;
		private final int port;
		private String baseDebugFile = "solver";
			
		public Factory(String host, int port)
		{
			this.host = host;
			this.port = port;
		}

		public Connection newConnection()
		{
			try
			{
				SocketConnection c = new SocketConnection(new Socket(host, port));
				return c;
			}
			catch (Exception x)
			{
				throw Util.handleAsNetwork(getClass(), x);
			}
		}
	}		
}
