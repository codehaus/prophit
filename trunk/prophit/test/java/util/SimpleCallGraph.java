package util;

import orbit.model.Call;
import orbit.model.CallGraph;
import orbit.parsers.Loader;
import orbit.parsers.LoaderFactory;

import java.io.File;

public class SimpleCallGraph
{
	public final CallGraph cg;
	public final Call main;
	public final Call test;
	public final Call init;
	public final Call DBExec1;
	public final Call DBExec2;
	public final Call insert1;
	public final Call update1;
	public final Call insert2;
	public final Call update2;
	public final Call append11;
	public final Call append12;
	public final Call append21;
	public final Call append22;
	
	public SimpleCallGraph()
	{
		cg = load();
		
		main = (Call)cg.getChildren().get(0);
		test = (Call)main.getChildren().get(0);
		init = (Call)main.getChildren().get(1);

		DBExec1 = (Call)test.getChildren().get(0);
		DBExec2 = (Call)init.getChildren().get(0);

		insert1 = (Call)DBExec1.getChildren().get(0);
		update1 = (Call)DBExec1.getChildren().get(1);
		insert2 = (Call)DBExec2.getChildren().get(0);
		update2 = (Call)DBExec2.getChildren().get(1);

		append11 = (Call)insert1.getChildren().get(0);
		append12 = (Call)update1.getChildren().get(0);
		
		append21 = (Call)insert2.getChildren().get(0);
		append22 = (Call)update2.getChildren().get(0);
	}
	
	private static CallGraph load()
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
