package orbit.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamPipe extends Thread
{
	private static int POLL_INTERVAL = 100;
	private static int BUF_LENGTH = 4096;
	private static int newline = 10;

	private InputStream inStream;
	private OutputStream outStream;
	private byte[] buf;
	private boolean active = true;
	private boolean doingExit = false;
	private boolean respectNewline;
	private boolean blocking;

	/**
	 * Creates a new <code>StreamPipe</code> instance.  When started,
	 * this class wil pipe data from inStream to outStream untill
	 * stopped or the streams are closed.  The respectNewline flag
	 * causes the pipe to try and send data along newline boundaries.
	 * The blocking flag causes blocking reads to be performed on
	 * inStream, otherwise polling reads are done.
	 *
	 * @param inStream an <code>InputStream</code> value
	 * @param outStream an <code>OutputStream</code> value
	 * @param respectNewline a <code>boolean</code> value
	 * @param blocking a <code>boolean</code> value
	 */
	public StreamPipe (InputStream inStream, OutputStream outStream, boolean respectNewline, boolean blocking)
	{
		this.inStream = inStream;
		this.outStream = outStream;
		this.respectNewline = respectNewline;
		this.blocking = blocking;
		buf = new byte[BUF_LENGTH];
	}

	public void run()
	{
		if (respectNewline) 
		{
			newlinePipe();
		}
		else 
		{
			plainPipe();
		}
	}

	private void plainPipe()
	{
		try
		{
			while (active || (doingExit && inStream.available() > 0))
			{
				if (inStream.available() > 0 || blocking)
				{
					int readCount = inStream.read(buf);
					outStream.write(buf, 0, readCount);
					outStream.flush();
				}
				else if (active)
				{
					try { sleep(POLL_INTERVAL); } catch (InterruptedException ex) {}
				}
			}
		}
		catch (IOException ex)
		{
			// Swallow them
		}
	}
	
	private void newlinePipe()
	{
		// Write a single line at a time instead of the entire buffer -
		// this results in less interleaving between out/err streams for
		// processes dumping a lot.  Do multiple reads to catch tardy
		// newlines, but if we still don't find a newline, output anyway
		// for those processes that aren't writing a newline for that
		// particular output (i.e. interactive prompts)
		try
		{
			while (active || (doingExit && inStream.available() > 0))
			{
				int i = 0;
				// Would like to do a blocking read here, but can't
				// because if we do this for java processes we have
				// executed, they end up blocking for a while after the
				// java process has died
				int b = -1;
				if (inStream.available() > 0 || blocking)
					b = inStream.read();
				else if (active)
					try { sleep(POLL_INTERVAL); } catch (InterruptedException ex) {}
				if (b != -1) 
				{
					// Keep adding bytes to our buffer until we encounter a
					// newline or run out of data, or exhaust our buffer
					while ((b != newline) && i < buf.length - 1)
					{
						buf[i++] = (byte) b;
						// Check to see if we ran out of data before
						// receiving a newline
						if ( ! (inStream.available() > 0))
						{
							// Try again after a slight delay in case we have
							// a tardy newline
							if (active)
								try { sleep(1); } catch (InterruptedException ex) {}
							if ( ! (inStream.available() > 0))
							{
								b = -1;
								break;
							}
						}
						// read next byte
						b = inStream.read();
					}
					// We reach here because we have either encountered a
					// newline, or run out of data in the stream.  If we
					// ran out of data, don't add any more bytes to output,
					// otherwise add the newline in, then write all the
					// data to output and start process over.
					if (b != -1) 
						buf[i++] = (byte) b;
					outStream.write(buf, 0, i);
					outStream.flush();
				}
			}
		}
		catch (IOException ex) 
		{
			// Swallow them
		}
	}

	public void exit()
	{
		doingExit = true;
		kill();
	}

	public void kill()
	{
		active = false;
		this.interrupt();
		Thread.yield();
	}
}
