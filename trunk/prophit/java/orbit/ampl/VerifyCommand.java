package orbit.ampl;

import java.io.BufferedReader;
import java.io.Writer;

public class VerifyCommand
	extends Command
{
	private String serverID = null;

	public VerifyCommand(String userName)
	{
		super(userName);
	}

	public String getServerID()
	{
		return serverID;
	}
	
	public void execute(Writer writer, BufferedReader reader)
	{
		writeHeader(writer, "verify");
		flush(writer);
		
		serverID = readSizedResult(reader);
	}
}
