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
		Solver solver = null;
		if ( parser instanceof DashProfParser )
		{
			solver = new LPSolver();
		}
		else if (parser instanceof HProfParser )
		{
			solver = new HProfSolver();
		}
		if ( parser instanceof Loader ) 
		{
		    return (Loader)parser;
		} 
		else 
		{
		    return new ParseAndSolveLoader(parser, solver, file);
		}	
	}
}
