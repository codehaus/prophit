package orbit.model;

import orbit.solver.ConnectionFactory;
import orbit.solver.Solver;

import java.io.*;
import java.util.StringTokenizer;

// TODO: remove this class in favor of DashProfSolver
public class CallFractionSolver
{
	private final CallFractionSolverData data;

	public CallFractionSolver(CallFractionSolverData data)
	{
		this.data = data;
	}
	
	public double[] execute(ConnectionFactory factory)
	{
		Solver solver = new Solver(factory);
		long solveStart = System.currentTimeMillis();
		String results = solver.execute(data.getUserName(), data.getModel(), data.getData(), data.getCommands());
		long solveEnd = System.currentTimeMillis();
		System.out.println("Solved in " + ( solveEnd - solveStart ) + " ms");
		return data.parse(results);
	}

	public void writeToFile(Writer w, double[] fractions)
	{
		PrintWriter writer = new PrintWriter(w);
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

	public double[] readFromFile(Reader r, int numCalls) throws IOException
	{
		double[] fractions = new double[numCalls];
		for ( int i = 0; i < fractions.length; ++i )
			fractions[i] = 1.0;

		BufferedReader reader = new BufferedReader(r);
		reader.readLine();
		String line;
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
		return fractions;
	}
}
