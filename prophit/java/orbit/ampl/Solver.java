package orbit.ampl;

import orbit.util.Log;

import org.apache.log4j.Category;

import java.io.BufferedReader;

public class Solver
{
	public static Category LOG = Category.getInstance(Solver.class);

	private final ConnectionFactory factory;
	
	public Solver(ConnectionFactory factory)
	{
		this.factory = factory;
	}
	
	public String execute(String userName, Datum modelDatum, Datum dataDatum, Datum commandsDatum)
	{
		VerifyCommand verify = new VerifyCommand(userName);
		execute(verify);
		String serverID = verify.getServerID();
		Log.debug(LOG, "serverID : ", serverID);
			
		BeginJobCommand beginJob = new BeginJobCommand(userName, modelDatum, dataDatum, commandsDatum);
		execute(beginJob);
		String jobNumber = beginJob.getJobNumber();
		Log.debug(LOG, "jobNumber : ", jobNumber);

		GetResultsCommand getResults = new GetResultsCommand(userName, jobNumber);
		execute(getResults);
		String results = getResults.getResults();
		Log.debug(LOG, "results : ", results);
		return results;
	}

	protected void execute(Command command)
	{
		Connection c = factory.newConnection();
		try
		{
			command.execute(c.getWriter(), new BufferedReader(c.getReader()));
		}
		finally
		{
			c.close();
		}
	}
}
