package orbit.parsers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

public class ParserFactory
{
	private ParserFactory()
	{
	}

	public static ParserFactory instance()
	{
		return new ParserFactory();
	}

	public Parser createParser(File file) throws ParseException
	{
		Reader reader;
		Parser parser;

		reader = openReader(file);
		parser = tryParser(new DashProfParser(reader));
		if ( parser != null )
			return parser;
		closeReader(reader);

		reader = openReader(file);
		parser = tryParser(new HProfParser(reader));
		if ( parser != null )
			return parser;
		closeReader(reader);

		reader = openReader(file);
		parser = tryParser(new ProphitParserLoader(reader));
		if (parser != null )
		    return parser;
		closeReader(reader);

		throw new ParseException("File " + file + " is not in a recognized format");
	}

	private Reader openReader(File file)
	{
		try
		{
			return new FileReader(file);
		}
		catch (FileNotFoundException x)
		{
			throw new IllegalArgumentException("File " + file + " not found");
		}
	}

	private void closeReader(Reader reader)
	{
		try
		{
			reader.close();
		}
		catch (IOException x)
		{
			x.printStackTrace();
		}
	}

	private Parser tryParser(Parser parser)
	{
		if ( parser.isFileFormatRecognized() )
			return parser;
		else
			return null;
	}
}
