package orbit.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DualStreamPipe extends Thread
{
	private static int POLL_INTERVAL = 100;
	private static int BUF_LENGTH = 1024;

	private InputStream inStream1;
	private OutputStream outStream1;
	private InputStream inStream2;
	private OutputStream outStream2;
	private byte[] buf;
	private boolean active = true;

	public DualStreamPipe (InputStream inStream1, OutputStream outStream1, InputStream inStream2, OutputStream outStream2)
	{
		this.inStream1 = inStream1;
		this.outStream1 = outStream1;
		this.inStream2 = inStream2;
		this.outStream2 = outStream2;
		buf = new byte[BUF_LENGTH];
	}

	public void run()
	{
		try
		{
			while (active)
			{
				if (inStream1.available() > 0)
				{
					int readCount = inStream1.read(buf);
					outStream1.write(buf, 0, readCount);
					outStream1.flush();
				}
				else if (inStream2.available() > 0)
				{
					int readCount = inStream2.read(buf);
					outStream2.write(buf, 0, readCount);
					outStream2.flush();
				}
				else
				{
					sleep(POLL_INTERVAL);
				}
			}
		}
		catch (IOException ex)
		{
			// Swallow them
		}
		catch (InterruptedException ex)
		{
			// Swallow them
		}
	}

	public void kill()
	{
		active = false;
	}
}
