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
}



