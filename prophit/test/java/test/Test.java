package test;

import util.*;

import orbit.gui.*;
import orbit.model.*;
import orbit.parsers.*;
import orbit.solver.*;
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
			testBasic();
			testHProfParserHello();
			testDashProfParserSimple();
			testSolver();
			
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
		command.execute(c.getWriter(), c.getReader());

		assertion("NEOS".equals(command.getServerID()), "Expected " + command.getServerID() + " = 'NEOS'");
		
		c.close();
	}
	
	public static void testSolver()
	{
		String[] responses =
		{
			"4\nNEOS",
			"-1\n" +
			"170797\n" +
			"Welcome to NEOS!\n" +
			"\n" +
			"<CLEAR_SCREEN>\n" +
			"Parsing:\n" +
			"\n" +
			"432 bytes written to donlp2.com (AMPL commands)\n" +
			"2747 bytes written to donlp2.mod (AMPL model)\n" +
			"405 bytes written to donlp2.dat (AMPL data)\n" +
			"<CLEAR_SCREEN>\n" +
			"Scheduling:\n" +
			"\n" +
			"You are job #170797.\n",
			"Job complete"
		};

		String userName = "hornbeast";
		String commands = "model bertsek.mod;\n" +
			"data bertsek.dat;\n" +
			"\n" +
			"set InitPoints;\n" +
			"param iptx {InitPoints};\n" +
			"param ipty {InitPoints};\n" +
			"param iptz {InitPoints};\n" +
			"data;\n";
		String model = "set Nodes  circular;	# nodes in the network\n" +
			"set Types;		# types of arcs\n" +
			"\n" +
			"param demand {Nodes};	# flow demand, i -> i+3\n" +
			"param c {Nodes,Types};	# delay coefficients for links in the network\n";
		String data = "data;\n" +
			"\n" +
			"set Types :=\n" +
			"ihy	# inside highway\n" +
			"ion	# inside on-ramp\n" +
			"ioff	# inside off-ramp\n" +
			"iby	# inside bypass\n";
		
		TestConnection.Factory factory = new TestConnection.Factory(responses);

		orbit.solver.Solver solver = new orbit.solver.Solver(factory);
		
		String result = solver.execute(userName, new StringDatum(model), new StringDatum(data), new StringDatum(commands));

		/*
		System.out.println(factory.getConnection(0).getWrittenText());
		System.out.println(factory.getConnection(1).getWrittenText());
		System.out.println(factory.getConnection(2).getWrittenText());
		System.out.println(result);
		*/

		assertion(factory.getConnection(0).getWrittenText().startsWith("hornbeast\nverify\n"),
				  "Expected factory.getConnection(0).getWrittenText() startsWith hornbeast\\nverify\\n");
		assertion(factory.getConnection(1).getWrittenText().startsWith("hornbeast\nbegin job 497\nTYPE NCO\nSOLVER DONLP2\n\nBEGIN.COM[135]"),
				  "Expected factory.getConnection(1).getWrittenText() startsWith hornbeast\\nbegin job 497\\nTYPE NCO\\nSOLVER DONLP2\\n\\nBEGIN.COM[135]");
		assertion(factory.getConnection(2).getWrittenText().startsWith("hornbeast\nget results 170797"),
				  "Expected factory.getConnection(2).getWrittenText() startsWith hornbeast\\nget results 170797");
		
		assertion("Job complete".equals(result), "Expected " + result + " = 'Job complete'");
	}

	public static void testHProfParserHello() throws Exception
	{
		Parser parser = ParserFactory.instance().createParser(new File(System.getProperty("basedir") + "/data/hello.hprof.txt"));
		parser.execute();
	}

	public static void testDashProfParserSimple() throws Exception
	{
		Parser parser = ParserFactory.instance().createParser(new File(System.getProperty("basedir") + "/data/simple.prof"));
		parser.execute();
		List callIDs = parser.getCallIDs();
		List proxyCallIDs = CallID.getProxyCallIDs(callIDs);

		assertion(callIDs.size() == 14, callIDs + ".size should be 14");
		assertion(proxyCallIDs.size() == 4, proxyCallIDs + ".size should be 4");

		CallID[] callIDArray = (CallID[])callIDs.toArray(new CallID[0]);
		double[] fractions = new double[callIDs.size()];
		for ( int i = 0; i < fractions.length; ++i )
			fractions[i] = 1;
		fractions[10] =  0.333333;
		fractions[11] =  0.666667;
		fractions[12] =  0.333333;
		fractions[13] =  0.666667;
		CallGraph cg = new CallGraph(callIDArray, fractions);

		class SimpleVisitor
			implements CallGraph.Visitor
		{
			int count = 0;

			int getCount() { return count; }
			
			public boolean visit(CallID callID, IntStack callStack)
			{
				++count;
				return true;
			}			
		}

		SimpleVisitor visitor = new SimpleVisitor();
		cg.depthFirstTraverse(visitor);
		assertion(visitor.getCount() == 13, "Expected to be visited 13 times");
	}		
	
	public static void testSimpleProfileSolver() throws Exception
	{
		File file = new File(System.getProperty("basedir") + "/data/simple.prof");
		DashProfParser parser = new DashProfParser(new FileReader(file));
		parser.execute();
		List callIDs = parser.getCallIDs();
		
		CallFractionSolverData data = new CallFractionSolverData(callIDs);
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
		parser.execute();

		long parseEnd = System.currentTimeMillis();
		System.out.println("Parsed hsqldb.prof in " + ( parseEnd - parseStart ) + " ms");
		
		List callIDs = parser.getCallIDs();

		System.setProperty("solver.user.name", "JAVA_USER");
		
		long solveStart = System.currentTimeMillis();
		
		DashProfSolver solver = new DashProfSolver(file);
		solver.solve(callIDs);

		long solveEnd = System.currentTimeMillis();
		System.out.println("Solved hsqldb.prof in " + ( solveEnd - solveStart ) + " ms");
	}
	
	public static void testBasic()
	{
		BasicTestCalls calls = new BasicTestCalls();

		CallAdapter root = new CallAdapter(calls.root);

		CallAdapter main = new CallAdapter(calls.main);
		CallAdapter events = new CallAdapter(calls.events);

		CallAdapter dbOpen = new CallAdapter(calls.dbOpen);
		CallAdapter dbClose = new CallAdapter(calls.dbClose);
		CallAdapter dbInit = new CallAdapter(calls.dbInit);

		assertion(main.getExclusiveTime(TimeMeasure.TotalTime) == 20, "main.timeInSelf should be 20, is " + main.getExclusiveTime(TimeMeasure.TotalTime));

		assertion(dbOpen.getDepth() == 2, "dbOpen.depth should be 2");
		assertion(dbOpen.getExclusiveTime(TimeMeasure.TotalTime) == 40, "dbOpen.timeInSelf should be 40");
		assertion(main.getExclusiveTime(TimeMeasure.TotalTime) == 20, "main.timeInSelf should be 20");
		assertion(main.getTimeInChildren(TimeMeasure.TotalTime) == 50, "main.timeInChildren should be 50");
		assertion(main.getInclusiveTimeFractionOfParentInclusiveTime(TimeMeasure.TotalTime) == 0.7, "main.fractionOfParentTime should be 0.7");
		assertion(dbOpen.getInclusiveFractionOfParentChildTimes(TimeMeasure.TotalTime) == 0.8, "dbOpen.fractionOfChildTimes should be 0.8");
		assertion(dbOpen.getInclusiveTimeFractionOfParentInclusiveTime(TimeMeasure.TotalTime) == 4 / 7.0, "dbOpen.fractionOfChildTimes should be " + ( 4 / 7.0 ));

		Rectangle2D.Double fullExtent = new Rectangle2D.Double(0, 0, 1.0, 1.0);
		Rectangle2D.Double leftHalf = new Rectangle2D.Double(0, 0, 0.5, 1.0);
		
		RectangleLayout layout = new RectangleLayout(TimeMeasure.TotalTime);

		layout.initialize(null, root, fullExtent, fullExtent);
		assertRectangle(layout.getExtent(), new Rectangle2D.Double(0, 0, 1, 1));
		
		layout.initialize(new CallAdapter(main.getParent()), main, fullExtent, fullExtent);
		assertRectangle(layout.getExtent(), new Rectangle2D.Double(0, 0, 0.7, 1));
		assertRectangle(layout.getRemainderExtent(layout.getExtent()), new Rectangle2D.Double(0.7, 0, 0.3, 1));

		layout.initialize(new CallAdapter(dbOpen.getParent()), dbOpen, fullExtent, fullExtent);
		assertRectangle(layout.getExtent(), new Rectangle2D.Double(0, 0, 0.8, 1));
		assertRectangle(layout.getRemainderExtent(layout.getExtent()), new Rectangle2D.Double(0.8, 0, 0.2, 1));

		// Should occupy the top 50% of the remaining triangle
		Rectangle2D.Double remainder = layout.getRemainderExtent(layout.getExtent());
		layout.initialize(new CallAdapter(dbClose.getParent()), dbClose, fullExtent, remainder);
		assertRectangle(layout.getExtent(), new Rectangle2D.Double(0.8, 0.5, 0.2, 0.5));
		assertRectangle(layout.getRemainderExtent(layout.getExtent()), new Rectangle2D.Double(0.8, 0, 0.2, 0.5));
		// Rendered area should the same fraction of the Extent as the fractionOfParent / fractionOfChild
		assertion(TestUtil.equal(TestUtil.area(layout.getRectangle(layout.getExtent())) / TestUtil.area(layout.getExtent()), 4 / 7.0 / 0.8), "Incorrect area ratio");
		
		System.out.println(layout.getRectangle(layout.getExtent()));
	}

	static void assertRectangle(Rectangle2D.Double first, Rectangle2D.Double second)
	{
		if ( !TestUtil.equal(first.x, second.x) ||
			 !TestUtil.equal(first.y, second.y) ||
			 !TestUtil.equal(first.width, second.width) ||
			 !TestUtil.equal(first.height, second.height) )
		{
			assertion(false, "Expected " + first + " = " + second);
		}
	}

	public static void assertion(boolean test, String message)
	{
		if ( !test )
			throw new RuntimeException("Assertion failed : " + message);
	}
}