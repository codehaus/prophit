package orbit.parsers;

import orbit.model.CallGraph;
import orbit.util.Util;

import java.io.*;
import java.util.List;

/**
 * Coordinates a Parser and a Solver to load a profile from a file.
 * The Parser and Solver should already be constructed appropriately.
 */
public class Loader
{
	private final Parser parser;
	private final Solver solver;
	private final File   file;

	private String error = null;
	private String warning = null;
	
	public Loader(Parser parser, Solver solver, File file)
	{
		this.parser = parser;
		this.solver = solver;
		this.file = file;
	}

	/**
	 * If the {@link #execute} method returns null, this method may return an error message which
	 * describes why the profile could not be loaded.
	 */
	public String getError()
	{
		return error;
	}
	
	/**
	 * If any recoverable errors occur while the profile is being loaded, this method will return them.
	 */
	public String getWarning()
	{
		return warning;
	}
	
	public CallGraph execute()
	{
		try
		{
			parser.execute();
			List callIDs = parser.getCallIDs();

			File fractionsFile = new File(file.getAbsolutePath() + ".graph");
			CallGraph cg = null;
			if ( fractionsFile.exists() ||
				 fractionsFile.lastModified() < file.lastModified() )
			{
				FileReader reader = new FileReader(fractionsFile);
				cg = solver.readFromCache(callIDs, reader);
				reader.close();
			}
			if ( cg == null )
			{
				cg = solver.solve(callIDs);
				try
				{
					FileWriter writer = new FileWriter(fractionsFile);
					solver.writeToCache(writer);
					writer.close();
				}
				catch (IOException x)
				{
					Util.handleTrace(getClass(), x);
					warning = "Error writing solved results to cache file '" + fractionsFile + "' : " + x.getMessage();
				}
			}
			return cg;
		}
		catch (Exception x)
		{
			Util.handleTrace(getClass(), x);
			error = "Error parsing profile from file '" + file + "' : " + x.getMessage();
			return null;
		}
	}
}
