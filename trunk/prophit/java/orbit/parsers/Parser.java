package orbit.parsers;

import java.util.Collection;

public interface Parser
{
	public boolean isFileFormatRecognized();

	public Collection getCallIDs();
	
	public Collection getProxyCallIDs();

	public void execute() throws ParseException;
}
