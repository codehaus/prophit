package orbit.parsers;

import util.*;

//import orbit.gui.CallAdapter;
//import orbit.gui.TimeMeasure;
//import orbit.gui.RectangleLayout;
import orbit.model.*;
import orbit.parsers.*;
import orbit.writer.*;
import orbit.util.*;

import junit.framework.TestCase;

import java.awt.geom.Rectangle2D;
import java.util.*;
import java.io.*;

public class TestProphitParser
	extends TestCase
{

	String profilesPath;  // for the profiles subdir
	String dataPath;      // for the toplevel data dir
	String outputPath;    // where all the data files written by the test itself go

	public TestProphitParser(String name)
	{
		super(name);
		this.profilesPath = System.getProperty("basedir") + "/test/data/profiles";
		this.dataPath = System.getProperty("basedir") + "/data";
		this.outputPath = System.getProperty("basedir") + "/build/output";
	}
	
	/** 
	 * testXMLSampleParser
	 * this test loads a manually created xml file to test a simple parsing 
	 * example. 
	 */
	public void testXMLSampleParser() throws Exception
	{
		Parser parser = new ProphitParserLoader(new FileReader(this.profilesPath + "/simple-profile-std.xml")); 
		
		assertTrue("File Format is recognized.", parser.isFileFormatRecognized());
		ModelBuilder builder = ModelBuilderFactory.newModelBuilder();
		parser.execute(builder);
	}
	
	/**
	 * testParserFactory
	 * This test tells us that we are able to parse a manually created xml profile
	 * file.  It does not tell us much otherwise about the correctness of the
	 * parsing, just that nothing went wrong enough to throw an exception.
	 * this test uses the parserfactory, so it tests that mechanism for detecting
	 * the correct parser. 
	 */
	public void testParserFactory() throws Exception
	{
		File file = new File(this.profilesPath + "/simple-profile-std.xml");
		Parser parser = ParserFactory.instance().createParser(file);
		assertTrue("class is a ProphitParserLoader.", parser.getClass() == ProphitParserLoader.class);
		ModelBuilder builder = ModelBuilderFactory.newModelBuilder();
		parser.execute(builder);
	}
	
	/**
	 * testLoaderFactory
	 * in this case, test our ability to find the correct class for LOADING
	 * the manufactured xml profile data file. 
	 * we don't even need to load the file for this test, just make sure the 
	 * correct loader was created.
	 */
	public void testLoaderFactory() throws Exception
	{
		File file = new File(this.profilesPath + "/simple-profile-std.xml");
		Loader loader = LoaderFactory.instance().createLoader(file);
		
		assertTrue("loader is a ProphitParserLoader", loader.getClass() == ProphitParserLoader.class);
	}
	
	/** 
	 * testLoadingWithFactory
	 * Now, we load the xml file, and then actually parse it.  
	 * the parse should complete successfully (no errors thrown).
	 */
	public void testLoadingWithFactory() throws Exception
	{
		File file = new File(this.profilesPath + "/simple-profile-std.xml");
		Loader loader = LoaderFactory.instance().createLoader(file);
		
		assertTrue("loader is a ProphitParserLoader", loader.getClass() == ProphitParserLoader.class);
		loader.parse();
	}
	
	/** 
	 * testWritingSimple
	 * this test writes out the callgraph to an output file. 
	 * it isn't guaranteed to be EXACTLY the same as the input file, 
	 * but it should be equivalent when loaded (tested separately). 
	 */
	public void testWritingSimple() throws Exception
	{
		File file = new File(this.profilesPath + "/simple-profile-std.xml");
		Loader loader = LoaderFactory.instance().createLoader(file);
		
		assertTrue("loader is a ProphitParserLoader", loader.getClass() == ProphitParserLoader.class);
		loader.parse();
		CallGraph cg = loader.solve();
		assertTrue("CallGraph is not null.", cg != null);
		File out = new File(this.outputPath + "/simple-profile-std.xml");
		ProphitWriter writer = new ProphitWriter( cg, out );
		writer.write();
	}

	public void testRoundTripSimpleProf() throws Exception
	{
		Loader loader = LoaderFactory.instance().createLoader(new File(this.dataPath + "/simple.prof"));
		loader.parse();
		CallGraph simple = loader.solve();
		CallID[] calls = simple.getCallIDs();
		for (int i = 0; i < calls.length; i++)
		{
			if (calls[i] != null) System.out.println(calls[i].toString());
		}
			
		ProphitWriter writer = new ProphitWriter( simple, new File(this.outputPath + "/simple.prof.xml"));
		writer.write();

		ProphitParserLoader p = new ProphitParserLoader( new FileReader(new File(this.outputPath + "/simple.prof.xml")));
		CallGraph newsimple = p.solve();

		SimpleCallGraph scg = new SimpleCallGraph();
		assertTrue("ROOT is the same", scg.main.equals(newsimple.getRoot()));
		assertTrue("Children are the same", scg.test.equals(newsimple.getRoot().getChildren().get(0)));
		assertTrue("Children are the same", scg.init.equals(newsimple.getRoot().getChildren().get(1)));

		assertEquals(simple.toString(simple.getMaxDepth()), newsimple.toString(newsimple.getMaxDepth()));
	}

	public void testRoundTripXML() throws Exception
	{
		doTestRoundTrip(this.dataPath, "testoutput.xml");
	}

	public void testRoundTripSampleXML() throws Exception
	{
		doTestRoundTrip(this.profilesPath, "simple-profile-std.xml");
	}

	public void testRoundTripHprofHello() throws Exception
	{
		doTestRoundTrip(this.dataPath, "hello.hprof.txt");
	}

	private void doTestRoundTrip(String inputDir, String inputFileName) throws Exception
	{
		String inputFilePath = inputDir + "/" + inputFileName;
		String outputFilePath = this.outputPath + "/" + inputFileName + ( inputFileName.endsWith(".xml") ? "" : ".xml" );

		Loader loader = LoaderFactory.instance().createLoader(new File(inputFilePath));
		loader.parse();
		CallGraph c1 = loader.solve();
		
		ProphitWriter writer = new ProphitWriter(c1, new File(outputFilePath));
		writer.write();

		ProphitParserLoader p2 = new ProphitParserLoader( new FileReader(new File(outputFilePath)));
		CallGraph c2 = p2.solve();
		
		assertEquals(c1.toString(c1.getMaxDepth()), c2.toString(c2.getMaxDepth()));
	}
}



