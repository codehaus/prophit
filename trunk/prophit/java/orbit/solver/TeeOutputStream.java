package orbit.solver;

import java.io.*;

public class TeeOutputStream
	extends OutputStream
{
	private final OutputStream first;
	private final OutputStream second;

	public TeeOutputStream(OutputStream first, OutputStream second)
	{
		this.first = first;
		this.second = second;
	}

	public void write(int i) throws IOException
	{
		first.write(i);
		second.write(i);
	}
}
