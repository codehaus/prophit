package orbit.parsers;

import orbit.model.CallGraph;
import orbit.model.CallID;
import orbit.model.CallFractionSolver;
import orbit.model.CallFractionSolverData;
import orbit.solver.SocketConnection;

import java.io.*;
import java.util.*;

public abstract class AbstractParser
	implements Parser
{
	protected final LineNumberReader reader;
	private int token = 0;

	protected static void main(String[] args, AbstractParser parser) throws Exception
	{
		Collection callIDs = parser.getCallIDs();
		Collection proxyCallIDs = parser.getProxyCallIDs();
		if ( "-debug".equals(args[1]) )
		{
			System.out.println(parser.getCallIDs());
		}
		else if ( "-solve".equals(args[1]) )
		{
			System.setProperty("solver.user.name", "JAVA_USER");
			CallFractionSolverData data = new CallFractionSolverData(callIDs, proxyCallIDs);
			CallFractionSolver solver = new CallFractionSolver(data);
			SocketConnection.Factory factory = new SocketConnection.Factory("neos.mcs.anl.gov", 3333);
			factory.setDebug(true, "solver");
			double[] fractions = solver.execute(factory);

			solver.writeToFile(new FileWriter(args[0] + ".fractions"), fractions);
		}
		else if ( "-print".equals(args[1]) )
		{
			int depth = 6;
			if ( args.length > 2 )
			{
				depth = Integer.parseInt(args[2]);
			}

			CallID[] callIDArray = (CallID[])parser.getCallIDs().toArray(new CallID[0]);

			CallFractionSolverData data = new CallFractionSolverData(callIDs, proxyCallIDs);
			CallFractionSolver solver = new CallFractionSolver(data);
			double[] fractions = solver.readFromFile(new FileReader(args[0] + ".graph"), callIDs.size());
			
			CallGraph cg = new CallGraph(callIDArray, fractions);
			System.out.println(cg.toString(depth));
		}
	}
	
	/**
	 * @return true if the parser believes it can read the file.
	 */
	public abstract boolean isFileFormatRecognized();

	public abstract List getCallIDs();
	
	public abstract List getProxyCallIDs();

	public abstract void execute() throws ParseException;

	/**
	 * Default implementation does nothing.
	 */
	public void postProcess(double[] fractions)
	{
	}

	protected List getProxyCallIDs(Collection callIDs)
	{
		ArrayList proxyCalls = new ArrayList(callIDs.size());
		for ( Iterator i = callIDs.iterator(); i.hasNext(); )
		{
			CallID callID = (CallID)i.next();
			if ( callID != null && callID.isProxy() )
				proxyCalls.add(callID);
		}
		return proxyCalls;
	}
	
	protected AbstractParser(LineNumberReader reader)
	{
		this.reader = reader;
	}
	
	protected void assertEqual(String expected, String actual) throws ParseException
	{
		if ( !expected.equals(actual) )
		{
			throw new ParseException("Expected '" + expected + "' at token " + token + ", line " + lineNumber() + ". Got '" + actual + "'");
		}
	}
	
	protected String nextToken(StringTokenizer tok, boolean must) throws ParseException
	{
		if ( !tok.hasMoreTokens() )
		{
			if ( must )
				throw new ParseException("Expected token " + token + " at line " + lineNumber());
			else
				return null;
		}
		String token = tok.nextToken();
		// Parse '<unknown caller>'
		if ( "<unknown".equals(token) )
		{
			return token + " " + nextToken(tok, true);
		}
		else
		{
			return token;
		}
	}

	protected String nextLine(boolean must) throws ParseException, IOException
	{
		String line = reader.readLine();
		if ( line == null && must )
		{
			throw new ParseException("Unexpected end of file at line " + lineNumber());
		}
		token = 0;
		return line;
	}

	protected int lineNumber()
	{
		return reader.getLineNumber();
	}
}
