package orbit.solver;

public class Solver
{
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
			
		BeginJobCommand beginJob = new BeginJobCommand(userName, modelDatum, dataDatum, commandsDatum);
		execute(beginJob);
		String jobNumber = beginJob.getJobNumber();

		GetResultsCommand getResults = new GetResultsCommand(userName, jobNumber);
		execute(getResults);
		String results = getResults.getResults();
		return results;
	}

	protected void execute(Command command)
	{
		Connection c = factory.newConnection();
		command.execute(c.getWriter(), c.getReader());
		c.close();
	}
}
