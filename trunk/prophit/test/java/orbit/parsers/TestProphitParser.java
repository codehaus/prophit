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
	public TestProphitParser(String name)
	{
		super(name);
		this.profilesPath = System.getProperty("basedir") + "/test/data/profiles";
		this.dataPath = System.getProperty("basedir") + "/data";
	}
	
	/** 
	 * testXMLSampleParser
	 * this test loads a manually created xml file to test a simple parsing 
	 * example. 
	 */
	public void testXMLSampleParser() throws Exception
	{
		try 
		{
			//System.out.println("HEY");
			
			Parser parser = new ProphitParserLoader(new FileReader(this.profilesPath + "/simple-profile-std.xml")); 
			
			assertTrue("File Format is recognized.", parser.isFileFormatRecognized());
			ModelBuilder builder = ModelBuilderFactory.newModelBuilder();
			parser.execute(builder);
		}
		catch (Exception x)
		{
			System.out.println(x.getMessage());
			throw new Exception(x.getMessage());
		}
		
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
		try 
		{
			File file = new File(this.profilesPath + "/simple-profile-std.xml");
			Parser parser = ParserFactory.instance().createParser(file);
			//System.out.println(parser);
			assertTrue("class is a ProphitParserLoader.", parser.getClass() == ProphitParserLoader.class);
			ModelBuilder builder = ModelBuilderFactory.newModelBuilder();
			parser.execute(builder);
		}
		catch (Exception x) 
		{
			System.out.println(x.getMessage());
			throw new Exception(x.getMessage());
		}
		
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
		try 
		{
			File file = new File(this.profilesPath + "/simple-profile-std.xml");
			Loader loader = LoaderFactory.instance().createLoader(file);
			
			assertTrue("loader is a ProphitParserLoader", loader.getClass() == ProphitParserLoader.class);
		} 
		catch (Exception x) 
		{
			System.out.println(x.getMessage());
			throw new Exception(x.getMessage());
		}
	}
	
	/** 
	 * testLoadingWithFactory
	 * Now, we load the xml file, and then actually parse it.  
	 * the parse should complete successfully (no errors thrown).
	 */
	public void testLoadingWithFactory() throws Exception
	{
		try 
		{
			File file = new File(this.profilesPath + "/simple-profile-std.xml");
			Loader loader = LoaderFactory.instance().createLoader(file);
			
			assertTrue("loader is a ProphitParserLoader", loader.getClass() == ProphitParserLoader.class);
			loader.parse();
			
		} 
		catch (Exception x) 
		{
			System.out.println(x.getMessage());
			throw new Exception(x.getMessage());
		}
	}
	
	/** 
	 * testWritingSimple
	 * this test writes out the callgraph to an output file. 
	 * it isn't guaranteed to be EXACTLY the same as the input file, 
	 * but it should be equivalent when loaded (tested separately). 
	 */
	public void testWritingSimple() throws Exception
	{
		try
		{
			File file = new File(this.profilesPath + "/simple-profile-std.xml");
			Loader loader = LoaderFactory.instance().createLoader(file);
			
			assertTrue("loader is a ProphitParserLoader", loader.getClass() == ProphitParserLoader.class);
			loader.parse();
			CallGraph cg = loader.solve();
			assertTrue("CallGraph is not null.", cg != null);
			File out = new File(this.profilesPath + "/out-simple-profile.xml");
			ProphitWriter writer = new ProphitWriter( cg, out );
			writer.write();
		}
		catch (Exception x)
		{
			System.out.println(x.getMessage());
			throw new Exception(x.getMessage());
		}
	}

	public void testRoundTripSimpleProf() throws Exception
	{
		try 
		{
			
			Loader loader = LoaderFactory.instance().createLoader(new File(this.dataPath + "/simple.prof"));
			loader.parse();
			CallGraph simple = loader.solve();
			CallID[] calls = simple.getCallIDs();
			for (int i = 0; i < calls.length; i++)
			{
				if (calls[i] != null) System.out.println(calls[i].toString());
			}
			System.out.println("finished loading simple");
			
			ProphitWriter writer = new ProphitWriter( simple, new File(this.dataPath + "/testoutput.xml"));
			writer.write();

			System.out.println("finished writing");

			ProphitParserLoader p = new ProphitParserLoader( new FileReader(new File(this.dataPath + "/testoutput.xml")));
			CallGraph newsimple = p.solve();

			SimpleCallGraph scg = new SimpleCallGraph();
			assertTrue("ROOT is the same", scg.main.equals(newsimple.getRoot()));
			assertTrue("Children are the same", scg.test.equals(newsimple.getRoot().getChildren().get(0)));
			assertTrue("Children are the same", scg.init.equals(newsimple.getRoot().getChildren().get(1)));
			
			System.out.println("FIRST: " + simple.toString(simple.getMaxDepth()));
			System.out.println("");
			System.out.println("SECOND: " + newsimple.toString(newsimple.getMaxDepth()));

		}
		catch (Exception x)
		{
			//System.out.println(x.getMessage());
			x.printStackTrace();
			throw new RuntimeException("Unable to load simple.prof : " + x);
		}
	}

	public void testRoundTripXML() throws Exception
	{
		try 
		{
			ProphitParserLoader p1 = new ProphitParserLoader( new FileReader(new File(this.dataPath + "/testoutput.xml")));
			CallGraph c1 = p1.solve();
			ProphitWriter writer = new ProphitWriter(c1, new File(this.dataPath + "/testoutput2.xml"));
			writer.write();
			ProphitParserLoader p2 = new ProphitParserLoader( new FileReader(new File(this.dataPath + "/testoutput2.xml")));
			CallGraph c2 = p2.solve();
			System.out.println("FIRST: " + c1.toString(c1.getMaxDepth()));
			System.out.println(" ");
			System.out.println("SECOND: " + c2.toString(c2.getMaxDepth()));
		} catch (Exception x)
		{
			x.printStackTrace();
			throw new RuntimeException("Unable to load.");
		}
	}

	public void testRoundTripSampleXML() throws Exception
	{
		try 
		{
			ProphitParserLoader p1 = new ProphitParserLoader(new FileReader(new File(this.profilesPath + "/simple-profile-std.xml")));
			CallGraph cg = p1.solve();
			ProphitWriter w = new ProphitWriter(cg, new File(this.profilesPath + "/simple-profile-std-copy.xml"));
			w.write();
		} catch (Exception x)
		{
			x.printStackTrace();
			throw new RuntimeException("unable to load");
		}
	}

	public void testRoundTripHprofHello() throws Exception
	{
		try 
		{
			Loader hLoader = LoaderFactory.instance().createLoader(new File(this.dataPath + "/hello.hprof.txt"));
			CallGraph cg = hLoader.solve();
			//System.out.println(":::" + cg.toString(cg.getMaxDepth()));
			ProphitWriter w = new ProphitWriter(cg, new File(this.profilesPath + "/out-hello.hprof.xml"));
			System.out.println("----------------");
			w.write();
			System.out.println("----------------");
			System.out.println(cg.toString(cg.getMaxDepth()));

			Loader pLoader = LoaderFactory.instance().createLoader(new File(this.profilesPath + "/out-hello.hprof.xml"));
			CallGraph cgxml = pLoader.solve();
			System.out.println(cgxml.toString(cgxml.getMaxDepth()));

		} catch (Exception x)
		{
			x.printStackTrace();
			throw new RuntimeException("unable to load");
		}
	}
}



