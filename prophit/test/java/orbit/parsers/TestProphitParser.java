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
	public TestProphitParser(String name)
	{
		super(name);
	}
	
	public void testXMLSampleParser() throws Exception
	{
		try 
		{
			//System.out.println("HEY");
			
			Parser parser = new ProphitParserLoader(new FileReader(System.getProperty("basedir") + "/test/data/simple-profile-std.xml")); 
			
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
	
	public void testParserFactory() throws Exception
	{
		try 
		{
			File file = new File(System.getProperty("basedir") + "/test/data/simple-profile-std.xml");
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
	
	public void testLoaderFactory() throws Exception
	{
		try 
		{
			File file = new File(System.getProperty("basedir") + "/test/data/simple-profile-std.xml");
			Loader loader = LoaderFactory.instance().createLoader(file);
			
			assertTrue("loader is a ProphitParserLoader", loader.getClass() == ProphitParserLoader.class);
		} 
		catch (Exception x) 
		{
			System.out.println(x.getMessage());
			throw new Exception(x.getMessage());
		}
	}
	
	public void testLoadingWithFactory() throws Exception
	{
		try 
		{
			File file = new File(System.getProperty("basedir") + "/test/data/simple-profile-std.xml");
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
	
	public void testWritingSimple() throws Exception
	{
		try
		{
			File file = new File(System.getProperty("basedir") + "/test/data/simple-profile-std.xml");
			Loader loader = LoaderFactory.instance().createLoader(file);
			
			assertTrue("loader is a ProphitParserLoader", loader.getClass() == ProphitParserLoader.class);
			loader.parse();
			CallGraph cg = loader.solve();
			assertTrue("CallGraph is not null.", cg != null);
			File out = new File(System.getProperty("basedir") + "/test/data/blah-output.xml");
			ProphitWriter writer = new ProphitWriter( cg, out );
			writer.write();
		}
		catch (Exception x)
		{
			System.out.println(x.getMessage());
			throw new Exception(x.getMessage());
		}
	}

	public void testRoundTripSimple() throws Exception
	{
		try 
		{
			
			Loader loader = LoaderFactory.instance().createLoader(new File(System.getProperty("basedir") + "/data/simple.prof"));
			loader.parse();
			CallGraph simple = loader.solve();
			CallID[] calls = simple.getCallIDs();
			for (int i = 0; i < calls.length; i++)
			{
				if (calls[i] != null) System.out.println(calls[i].toString());
			}
			System.out.println("finished loading simple");
			
			ProphitWriter writer = new ProphitWriter( simple, new File(System.getProperty("basedir") + "/data/testoutput.xml"));
			writer.write();

			System.out.println("finished writing");

			ProphitParserLoader p = new ProphitParserLoader( new FileReader(new File(System.getProperty("basedir") + "/data/testoutput.xml")));
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
			ProphitParserLoader p1 = new ProphitParserLoader( new FileReader(new File(System.getProperty("basedir") + "/data/testoutput.xml")));
			CallGraph c1 = p1.solve();
			ProphitWriter writer = new ProphitWriter(c1, new File(System.getProperty("basedir") + "/data/testoutput2.xml"));
			writer.write();
			ProphitParserLoader p2 = new ProphitParserLoader( new FileReader(new File(System.getProperty("basedir") + "/data/testoutput2.xml")));
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
			ProphitParserLoader p1 = new ProphitParserLoader(new FileReader(new File(System.getProperty("basedir") + "/test/data/simple-profile-std.xml")));
			CallGraph cg = p1.solve();
			ProphitWriter w = new ProphitWriter(cg, new File(System.getProperty("basedir") + "/test/data/simple-profile-std-copy.xml"));
			w.write();
		} catch (Exception x)
		{
			x.printStackTrace();
			throw new RuntimeException("unable to load");
		}
	}
}



