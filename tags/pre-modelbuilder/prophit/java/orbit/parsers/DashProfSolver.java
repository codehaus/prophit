package orbit.parsers;

import orbit.model.CallID;
import orbit.model.CallGraph;
import orbit.ampl.SocketConnection;
import orbit.util.Util;

import org.apache.log4j.Category;

import java.io.*;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

public class DashProfSolver
	implements Solver
{
	public static Category LOG = Category.getInstance(DashProfSolver.class);

	private final static String VERSION = "1.0.0";
	
	private final SocketConnection.Factory factory;
	private double[] fractions = null;

	public DashProfSolver(File profileFile)
	{
		ConnectionProperties props = new ConnectionProperties();
		
		factory = new SocketConnection.Factory(props.getHostName(), props.getPort());
	}

	// TODO: make protected & move test in test.Test into this package
	public double[] getFractions()
	{
		return fractions;
	}
	
	public CallGraph solve(List callIDs)
	{
		DashProfSolverData data = new DashProfSolverData(callIDs);
																 
		orbit.ampl.Solver solver = new orbit.ampl.Solver(factory);
		long solveStart = System.currentTimeMillis();
		String results = solver.execute(data.getUserName(), data.getModel(), data.getData(), data.getCommands());
		long solveEnd = System.currentTimeMillis();

		LOG.info("Solved in " + ( solveEnd - solveStart ) + " ms");
		
		fractions = data.parse(results);
		
		return new CallGraph(callIDs, fractions);
	}

	public CallGraph readFromCache(List callIDs, Reader r) throws IOException
	{
		double[] fractions = new double[callIDs.size()];
		for ( int i = 0; i < fractions.length; ++i )
			fractions[i] = 1.0;

		BufferedReader reader = new BufferedReader(r);
		String line;
		line = reader.readLine();
		if ( !("DashProfSolver Version " + VERSION).equals(line) )
		{
			return null;
		}
		// Eat the 'f [*] :=' line
		line = reader.readLine();
		while ( ( line = reader.readLine() ) != null )
		{
			StringTokenizer tok = new StringTokenizer(line, " \t");
			while ( tok.hasMoreTokens() )
			{
				String proxyCallKeyStr = tok.nextToken();
				proxyCallKeyStr = proxyCallKeyStr.substring(1, proxyCallKeyStr.length());
				if ( !"".equals(proxyCallKeyStr) && !";".equals(proxyCallKeyStr) )
				{
					int proxyCallKey = Integer.parseInt(proxyCallKeyStr);
					double f = Double.parseDouble(tok.nextToken());
					fractions[proxyCallKey] = f;
				}
			}
		}
		return new CallGraph(callIDs, fractions);
	}

	public void writeToCache(Writer w)
	{
		PrintWriter writer = new PrintWriter(w);
		writer.println("DashProfSolver Version " + VERSION);
		writer.println("f [*] :=");
		for ( int i = 0; i < fractions.length; ++i )
		{
			if ( fractions[i] != 1.0 )
			{
				writer.print("p");writer.print(i);writer.print(" ");writer.println(fractions[i]);
			}
		}
		writer.println(";");
	}

	static class ConnectionProperties
	{
		private final Properties props;
		
		public ConnectionProperties()
		{
			InputStream is = getClass().getClassLoader().getResourceAsStream("ampl/connection.properties");
			props = new Properties();
			try
			{
				props.load(is);
				is.close();
			}
			catch (IOException x)
			{
				throw Util.handle(getClass(), x);
			}
		}
		
		public String getHostName()
		{
			return props.getProperty("host.name");
		}

		public int getPort()
		{
			return Integer.parseInt(props.getProperty("port"));
		}
		
		public boolean getDebug()
		{
			return "true".equals(props.getProperty("debug"));
		}
	}
}
