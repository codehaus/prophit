package test;

import util.*;

import orbit.gui.*;
import orbit.model.*;
import orbit.parsers.*;
import orbit.ampl.*;
import orbit.util.*;

import junit.framework.TestSuite;

import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.*;

public class Test
{
	public static void main(String[] args) throws Throwable
	{
		try
		{
			if ( args.length > 0 && "-system".equals(args[0]) )
			{
				testConnection();
				testSimpleProfileSolver();
				testHsqlDBProfileSolver();
			}
		}
		catch (Throwable t)
		{
			t.printStackTrace();
			throw t;
		}
	}

	public static void testConnection()
	{
		SocketConnection.Factory factory = new SocketConnection.Factory("neos.mcs.anl.gov", 3333);
		Connection c = factory.newConnection();

		VerifyCommand command = new VerifyCommand("joe");
		command.execute(c.getWriter(), new BufferedReader(c.getReader()));

		assertion("NEOS".equals(command.getServerID()), "Expected " + command.getServerID() + " = 'NEOS'");
		
		c.close();
	}
	
	public static void testSimpleProfileSolver() throws Exception
	{
		File file = new File(System.getProperty("basedir") + "/data/simple.prof");
		DashProfParser parser = new DashProfParser(new FileReader(file));
		ModelBuilder builder = ModelBuilderFactory.newModelBuilder();
		parser.execute(builder);
		List callIDs = builder.getCallIDs();
		
		DashProfSolverData data = new DashProfSolverData(callIDs);
		assertion(data.getModel().toString().startsWith("[741]option solver kestrel;"),
				  "Expected " + data.getModel() + " to start with [741]option solver kestrel;");
		assertion(data.getCommands().toString().startsWith("[171]# Assign initial values for I"),
				  "Expected " + data.getCommands() + " to start with [171]# Assign initial values for I");
		String expectedData = "[224]data;\n" +
			"set rcc = 6 4 5 7;\n" + 
			"set I = p10 p11 p12 p13;";
		assertion(data.getData().toString().startsWith(expectedData), "Expected " + data.getData() + " to start with " + expectedData);

		System.setProperty("solver.user.name", "JAVA_USER");
		DashProfSolver solver = new DashProfSolver(file);
		solver.solve(callIDs);
		double[] fractions = solver.getFractions();

		assertion(fractions.length == 14, "Expected " + fractions.length + " = 14");
		assertion(TestUtil.equal(fractions[0], 1), "Expected " + fractions[0] + " = 1");
		assertion(TestUtil.equal(fractions[1], 1), "Expected " + fractions[1] + " = 1");
		// TODO: I think I changed the data file & broke these tests
		assertion(TestUtil.equal(fractions[10], 0.333333), "Expected " + fractions[10] + " = 1/3");
		assertion(TestUtil.equal(fractions[11], 0.666667), "Expected " + fractions[10] + " = 2/3");
		/*
		for ( int i = 0; i < fractions.length; ++i )
		{
			System.out.println(i + " = " + fractions[i]);
		}
		*/
	}

	public static void testHsqlDBProfileSolver() throws Exception
	{
		long parseStart = System.currentTimeMillis();

		File file = new File(System.getProperty("basedir") + "/data/hsqldb.prof");
		DashProfParser parser = new DashProfParser(new FileReader(file));
		ModelBuilder builder = ModelBuilderFactory.newModelBuilder();
		parser.execute(builder);

		long parseEnd = System.currentTimeMillis();
		System.out.println("Parsed hsqldb.prof in " + ( parseEnd - parseStart ) + " ms");
		
		List callIDs = builder.getCallIDs();

		System.setProperty("solver.user.name", "JAVA_USER");
		
		long solveStart = System.currentTimeMillis();
		
		DashProfSolver solver = new DashProfSolver(file);
		solver.solve(callIDs);

		long solveEnd = System.currentTimeMillis();
		System.out.println("Solved hsqldb.prof in " + ( solveEnd - solveStart ) + " ms");
	}
	
	public static void assertion(boolean test, String message)
	{
		if ( !test )
			throw new RuntimeException("Assertion failed : " + message);
	}
}
