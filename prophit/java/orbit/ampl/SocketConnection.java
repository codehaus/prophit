package orbit.ampl;

import orbit.util.Util;

import java.io.*;
import java.net.Socket;

public class SocketConnection
	implements Connection
{
	private static int COUNTER = 0;
	
	private final Socket socket;
	
	private Writer         writer    = null;
	private BufferedReader reader    = null;
	private String         debugFile = null;
	
	private SocketConnection(Socket socket)
	{
		this.socket = socket;
	}

	public void setDebugFile(String debugFile)
	{
		this.debugFile = debugFile;
	}

	public Writer getWriter()
	{
		open();
		return writer;
	}

	public BufferedReader getReader()
	{
		open();
		return reader;
	}

	public void close()
	{
		try
		{
			socket.close();
		}
		catch (IOException x)
		{
			throw Util.handle(getClass(), x);
		}
	}

	private void open()
	{
		if ( writer != null && reader != null )
			return;

		try
		{
			if ( debugFile != null )
			{
				reader = new BufferedReader(new InputStreamReader(new LogInputStream(socket.getInputStream(), new FileOutputStream(debugFile + "." + ( COUNTER++ ) + ".input")), "US-ASCII"));
				writer = new OutputStreamWriter(new TeeOutputStream(socket.getOutputStream(), new FileOutputStream(debugFile + "." + ( COUNTER++ ) + ".output")), "US-ASCII");
			}
			else
			{
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "US-ASCII"));
				writer = new OutputStreamWriter(socket.getOutputStream(), "US-ASCII");
			}
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
		private boolean debug = false;
		private String baseDebugFile = "solver";
			
		public Factory(String host, int port)
		{
			this.host = host;
			this.port = port;
		}

		public void setDebug(boolean b, String baseFile)
		{
			this.debug = b;
			if ( baseFile != null )
				this.baseDebugFile = baseFile;
		}
		
		public Connection newConnection()
		{
			try
			{
				SocketConnection c = new SocketConnection(new Socket(host, port));
				if ( debug )
				{
					c.setDebugFile(baseDebugFile);
				}
				return c;
			}
			catch (Exception x)
			{
				throw Util.handleAsNetwork(getClass(), x);
			}
		}
	}		
}
