package orbit.ampl;

import java.io.Writer;
import java.io.Reader;

public interface Connection
{
	public Writer getWriter();

	public Reader getReader();

	public void close();
}
