package orbit.parsers;

import java.util.List;

public interface Parser
{
	public boolean isFileFormatRecognized();

	public List getCallIDs();
	
	public List getProxyCallIDs();

	public void execute() throws ParseException;

	public void postProcess(double[] fractions);
}
