package orbit.model;

import orbit.parsers.Loader;
import orbit.parsers.LoaderFactory;

import org.apache.log4j.Category;

import java.io.File;

import junit.framework.TestCase;

public class TestRCC
	extends TestCase
{
	public static Category LOG = Category.getInstance(TestRCC.class);
	public TestRCC(String name)
	{
		super(name);
	}

	public void testGetRCCs() throws Exception
	{
		try
		{
			Loader loader = LoaderFactory.instance().createLoader(new File(System.getProperty("basedir") + "/data/hello.hprof.txt"));
			CallGraph cg = loader.solve();
			
			RCC rccs[] = RCC.getRCCs(cg.getCallIDs());
		   int maxRCC = 0;
			for (int i = 0; i < rccs.length; i++)
			{
				if (maxRCC < rccs[i].getKey()) maxRCC = rccs[i].getKey();
			}
			LOG.info("MaxRCC : " + maxRCC + " numRCCs : " + rccs.length);
			assertTrue("MaxRCC < = numRCCs", maxRCC <= rccs.length );

			/*for (int i = 0; i < rccs.length; i++)
			{ 
				if  (rccs[i].getTime() == -1 && rccs[i].getExclusiveTime() == -1) 
				{ 
					 System.out.println("RCC[" + i + "]: " + rccs[i]);
				} 
				}*/
			//System.out.println("RCC COUNT: " +rccs.length);
											 
		} catch (Exception x)
		{
			x.printStackTrace();
			throw new RuntimeException("RCC's don't match");
		}
	}
}
