package orbit.parsers;

import java.io.File;

public class LoaderFactory
{
	private LoaderFactory() { }
	
	public LoaderFactory instance()
	{
		return new LoaderFactory();
	}
	
	public Loader createLoader(File file) throws ParseException
	{
		Parser parser = ParserFactory.instance().createParser(file);
		Solver solver;
		if ( parser instanceof DashProfParser )
		{
			solver = new DashProfSolver(file);
		}
		else
		{
			// solver = new HProfSolver();
			throw new IllegalArgumentException("No solver for parser " + parser);
		}
		return new Loader(parser, solver, file);
	}	
}
