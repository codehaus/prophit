package orbit.parsers;

import java.util.List;

public interface Parser
{
	public boolean isFileFormatRecognized();

	public List getCallIDs();
	
	public void execute() throws ParseException;
}
