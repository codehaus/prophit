package orbit.ampl;

import java.io.Writer;
import java.io.BufferedReader;

public interface Connection
{
	public Writer getWriter();

	public BufferedReader getReader();

	public void close();
}
