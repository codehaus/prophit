package test;

import orbit.model.CallGraph;
import orbit.parsers.Loader;
import orbit.parsers.LoaderFactory;

import java.io.File;

public class SimpleCallGraph
{
	public static CallGraph load()
	{
		try
		{
			Loader loader = LoaderFactory.instance().createLoader(new File(System.getProperty("basedir") + "/data/simple.prof"));
			loader.parse();
			return loader.solve();
		}
		catch (Exception x)
		{
			x.printStackTrace();
			throw new RuntimeException("Unable to load simple.prof : " + x);
		}
	}
}
