package orbit.parsers;

import orbit.model.*;

import java.io.File;
import java.io.FileReader;
import java.util.List;

import junit.framework.TestCase;

public class TestHProfParser
	extends TestCase
{
	public TestHProfParser(String name)
	{
		super(name);
	}

	public void testParseSamples() throws Exception
	{
		File file = new File(System.getProperty("basedir") + "/test/data/hello.hprof.txt");
		
		HProfParser parser = new HProfParser(new FileReader(file));
		ModelBuilder builder = ModelBuilderFactory.newModelBuilder();
		parser.execute(builder);

		List callIDs = builder.getCallIDs();

		// System.out.println(callIDs);
		System.out.println("TestHProfParser needs more work");
	}

	/**
	 * Just tests that the hsqldb.hprof.txt file can be loaded without exceptions. Pretty lame.
	 */
	public void testParseSamples2() throws Exception
	{
		File file = new File(System.getProperty("basedir") + "/data/hsqldb.hprof.txt");
		
		HProfParser parser = new HProfParser(new FileReader(file));
		ModelBuilder builder = ModelBuilderFactory.newModelBuilder();
		parser.execute(builder);
	}

	public void testParseTimes() throws Exception
	{
		File file = new File(System.getProperty("basedir") + "/data/hello.times.txt");
		
		HProfParser parser = new HProfParser(new FileReader(file));
		ModelBuilder builder = ModelBuilderFactory.newModelBuilder();
		parser.execute(builder);

		List callIDs = builder.getCallIDs();

		// System.out.println(callIDs);
		System.out.println("TestHProfParser needs more work");
	}

	/**
	 * Loads the simple.prof profile using the DashProfParser
	 * Then applies the HProfSolver to the CallIDs in this profile.
	 * <p>
	 * The HProfSolver assumes it is supplied with exclusive times, and computes
	 * the inclusive times. This is different behavior than the DashProfParser, to which
	 * the data file supplies inclusive times.
	 * <p>
	 * This test assures that the inclusive times are computed properly.
	 */
	public void testSolve() throws Exception
	{
		File profileFile = new File(System.getProperty("basedir") + "/data/simple.prof");
		DashProfParser parser = new DashProfParser(new FileReader(profileFile));
		ModelBuilder builder = ModelBuilderFactory.newModelBuilder();
		// Lie to the builder and tell it the times are Exclusive even though
		//   the parser will tell it they are inclusive
		// It will print a warning when initialize() is invoked again by the parser
		builder.initialize(TimeData.Exclusive);
		parser.execute(builder);

		List callIDs = builder.getCallIDs();
		assertEquals(14, callIDs.size());

		HProfSolver solver = new HProfSolver();
		CallGraph cg = solver.solve(callIDs);
		// System.out.println(cg.toString(-1));

		Call main = (Call)cg.getRoot();
		assertEquals("main", main.getName());
		assertEquals(1, main.getCallCount());
		assertEquals(945.0, main.getTime(), 0.0001);

		Call test = (Call)main.getChildren().get(0);
		assertEquals("test", test.getName());
		assertEquals(390.0, test.getTime(), 0.0001);

		Call init = (Call)main.getChildren().get(1);
		assertEquals("init", init.getName());
		assertEquals(255.0, init.getTime(), 0.0001);
		
		Call DBExec1 = (Call)test.getChildren().get(0);
		assertEquals("DBExec", DBExec1.getName());
		assertEquals(210.0, DBExec1.getTime(), 0.0001);
		Call insert1 = (Call)DBExec1.getChildren().get(0);
		assertEquals("insert", insert1.getName());
		assertEquals(30.0, insert1.getTime(), 0.0001);
		Call append1 = (Call)insert1.getChildren().get(0);
		assertEquals("append", append1.getName());
		assertEquals(10.0, append1.getTime(), 0.0001);
	}

	static CallID callID(List callIDs, int index)
	{
		return (CallID)callIDs.get(index);
	}
}
