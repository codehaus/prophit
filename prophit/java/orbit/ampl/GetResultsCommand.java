package orbit.ampl;

import java.io.BufferedReader;
import java.io.Writer;

public class GetResultsCommand
	extends Command
{
	private final String jobNumber;
	private String results = null;

	public GetResultsCommand(String userName, String jobNumber)
	{
		super(userName);

		this.jobNumber = jobNumber;
	}

	public String getResults()
	{
		return results;
	}
	
	public void execute(Writer writer, BufferedReader reader)
	{
		writeHeader(writer, "get results " + jobNumber);
		flush(writer);
		
		results = readFully(reader);
	}
}
