package orbit.ampl;

import util.*;
import orbit.ampl.*;

import junit.framework.TestCase;

import java.util.*;
import java.io.*;

public class TestSolver
	extends TestCase
{
	public TestSolver(String name)
	{
		super(name);
	}

	public void testSolver()
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

		Solver solver = new Solver(factory);
		
		String result = solver.execute(userName, new StringDatum(model), new StringDatum(data), new StringDatum(commands));

		/*
		System.out.println(factory.getConnection(0).getWrittenText());
		System.out.println(factory.getConnection(1).getWrittenText());
		System.out.println(factory.getConnection(2).getWrittenText());
		System.out.println(result);
		*/

		assertTrue("Expected factory.getConnection(0).getWrittenText() startsWith hornbeast\\nverify\\n",
				   factory.getConnection(0).getWrittenText().startsWith("hornbeast\nverify\n"));
		assertTrue("Expected factory.getConnection(1).getWrittenText() startsWith hornbeast\\nbegin job 497\\nTYPE NCO\\nSOLVER DONLP2\\n\\nBEGIN.COM[135]",
				   factory.getConnection(1).getWrittenText().startsWith("hornbeast\nbegin job 497\nTYPE NCO\nSOLVER DONLP2\n\nBEGIN.COM[135]"));
		assertTrue("Expected factory.getConnection(2).getWrittenText() startsWith hornbeast\\nget results 170797",
				   factory.getConnection(2).getWrittenText().startsWith("hornbeast\nget results 170797"));
		
		assertTrue("Expected " + result + " = 'Job complete'",
				   "Job complete".equals(result));
	}
}
