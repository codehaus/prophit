package orbit.parsers;

import java.io.File;

public class LoaderFactory
{
	private LoaderFactory() { }
	
	public static LoaderFactory instance()
	{
		return new LoaderFactory();
	}
	
	public Loader createLoader(File file) throws ParseException
	{
		Parser parser = ParserFactory.instance().createParser(file);
		Solver solver;
		if ( parser instanceof DashProfParser )
		{
			// solver = new DashProfSolver(file);
			solver = new LPSolver();
		}
		else
		{
			solver = new HProfSolver();
		}
		return new Loader(parser, solver, file);
	}	
}
