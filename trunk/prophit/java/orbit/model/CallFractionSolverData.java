package orbit.model;

import orbit.ampl.Datum;
import orbit.ampl.StringDatum;
import orbit.util.ConfigurationException;
import orbit.util.Util;

import java.io.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

// This class really doesn't belong in this package. The 'solver' stuff should be moved into a different package
//   called something like 'qpsolver', then this class should go into a 'solver' package or perhaps into 'loader'
public class CallFractionSolverData
{
	private static final String modelResourceName = "/ampl/profile.mod";
	private static final String commandsResourceName = "/ampl/profile.run";

	private final List callIDs;
	private final List proxyCallIDs;
	private boolean debug = false;
	
	public CallFractionSolverData(List callIDs)
	{
		this.callIDs = callIDs;
		this.proxyCallIDs = CallID.getProxyCallIDs(callIDs);
	}

	public double[] parse(String results)
	{
		double[] callFractions = new double[callIDs.size()];
		for ( int i = 0; i < callFractions.length; ++i )
			callFractions[i] = 1.0;

		try
		{
			LineNumberReader reader = new LineNumberReader(new StringReader(results));
			String line;
			// Look for the beginning of the 'f' data
			while ( ( line = reader.readLine() ) != null && line.indexOf("f [*] :=") == -1 ) { }
			boolean done = false;
			while ( !done && ( line = reader.readLine() ) != null )
			{
				StringTokenizer tok = new StringTokenizer(line, " \t");
				while ( !done && tok.hasMoreTokens() )
				{
					/*
					 * Read the proxyCallKey and strip out the leading 'p'
					 * If it is a 
					 */
					String proxyCallKeyStr = tok.nextToken();
					if ( !";".equals(proxyCallKeyStr) )
					{
						proxyCallKeyStr = proxyCallKeyStr.substring(1, proxyCallKeyStr.length());
						int proxyCallKey = Integer.parseInt(proxyCallKeyStr);
						double f = Double.parseDouble(tok.nextToken());
						callFractions[proxyCallKey] = f;
						// System.out.println(proxyCallKey + " = " + f);
					}
					else
					{
						done = true;
					}
				}
			}
		}
		catch (IOException x)
		{
			throw Util.handle(getClass(), x);
		}
		return callFractions;
	}

	public String getUserName()
	{
		String userName = System.getProperty("solver.user.name");
		if ( userName == null )
			userName = System.getProperty("user.name");
		return userName;		
	}
	
	public Datum getModel()
	{
		return new StringDatum(readResource(modelResourceName));
	}

	public Datum getCommands()
	{
		return new StringDatum(readResource(commandsResourceName));
	}
	
	public Datum getData()
	{
		StringWriter str = new StringWriter();
		PrintWriter writer = new PrintWriter(str);

		writer.print("data;");writer.print('\n');
		writer.print("set rcc =");
		HashSet rccs = new HashSet();
		for ( Iterator i = proxyCallIDs.iterator(); i.hasNext(); )
		{
			CallID callID = (CallID)i.next();
			if ( rccs.add(callID.getRCC()) )
			{
				writer.print(" ");writer.print(callID.getRCC().getKey());
			}
			if ( callID.getParentRCC() != null &&
				 rccs.add(callID.getParentRCC()) )
			{
				writer.print(" ");writer.print(callID.getParentRCC().getKey());
			}
		}
		writer.print(";");writer.print('\n');

		if ( debug )
		{
			writer.print("# rcc = ");
			HashSet rccs2 = new HashSet();
			for ( Iterator i = proxyCallIDs.iterator(); i.hasNext(); )
			{
				CallID callID = (CallID)i.next();
				if ( rccs2.add(callID.getRCC()) )
				{
					writer.print(" ");writer.print(callID.getRCC().getLeafMethodName());
				}
				if ( callID.getParentRCC() != null &&
					 rccs2.add(callID.getParentRCC()) )
				{
					writer.print(" ");writer.print(callID.getParentRCC().getLeafMethodName());
				}
			}
			writer.print('\n');
		}
			
		writer.print("set I =");
		for ( Iterator i = proxyCallIDs.iterator(); i.hasNext(); )
		{
			CallID callID = (CallID)i.next();
			writer.print(" p");writer.print(callID.getKey());
		}
		writer.print(";");writer.print('\n');

		if ( debug )
		{
			writer.print("# I =");
			for ( Iterator i = proxyCallIDs.iterator(); i.hasNext(); )
			{
				CallID callID = (CallID)i.next();
				writer.print(" ");writer.print(callID.getRCC().getLeafMethodName());
			}
			writer.print('\n');
		}
			
		writer.print("param time := ");writer.print('\n');
		for ( Iterator i = rccs.iterator(); i.hasNext(); )
		{
			RCC rcc = (RCC)i.next();
			long time = rcc.getTime();
			for ( Iterator j = callIDs.iterator(); j.hasNext(); )
			{
				CallID child = (CallID)j.next();
				if ( child != null && !child.isProxy() && child.getParentRCCKey() == rcc.getKey() )
					time -= child.getRCC().getTime();
			}
			if ( time < 0 )
			{
				System.out.println("Child time adds up to " + -time + " more than parent time for RCC " + rcc);
				time = 0;
			}
			writer.print("\t");writer.print(rcc.getKey());writer.print("\t");writer.print(time);writer.print('\n');
		}
		writer.print(";");writer.print('\n');

		writer.print("param nCalls := ");writer.print('\n');
		for ( Iterator i = rccs.iterator(); i.hasNext(); )
		{
			RCC rcc = (RCC)i.next();
			writer.print("\t");writer.print(rcc.getKey());writer.print("\t");writer.print(rcc.getCallCount());writer.print('\n');
		}
		writer.print(";");writer.print('\n');
		
		writer.print("param parent := ");writer.print('\n');
		for ( Iterator i = proxyCallIDs.iterator(); i.hasNext(); )
		{
			CallID callID = (CallID)i.next();
			writer.print("\tp");writer.print(callID.getKey());writer.print("\t");writer.print(callID.getParentRCCKey());writer.print('\n');
		}
		writer.print(";");writer.print('\n');

		writer.print("param rccOf := ");writer.print('\n');
		for ( Iterator i = proxyCallIDs.iterator(); i.hasNext(); )
		{
			CallID callID = (CallID)i.next();
			writer.print("\tp");writer.print(callID.getKey());writer.print("\t");writer.print(callID.getRCC().getKey());writer.print('\n');
		}
		writer.print(";");writer.print('\n');

		writer.flush();
		return new StringDatum(str.toString());
	}

	private String readResource(String resourceName)
	{
		try
		{
			InputStream is = getClass().getResourceAsStream(resourceName);
			if ( is == null )
			{
				throw new ConfigurationException("Resource " + resourceName + " not found by " + getClass());
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			StringBuffer sb = new StringBuffer();
			String line;
			while ( ( line = reader.readLine() ) != null )
			{
				sb.append(line);
				sb.append("\n");
			}
			sb.append("\n");
			return sb.toString();
		}
		catch (IOException x)
		{
			throw Util.handleAsConfiguration(getClass(), x);
		}
	}
}
