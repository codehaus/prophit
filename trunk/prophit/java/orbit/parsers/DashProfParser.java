package orbit.parsers;

import orbit.model.*;
import orbit.util.Log;

import org.apache.log4j.Category;

import java.io.*;
import java.util.*;

public class DashProfParser
	extends AbstractParser
{
	public static void main(String[] args) throws Exception
	{
		DashProfParser parser;
		ModelBuilder builder = ModelBuilderFactory.newModelBuilder();
		{
			FileReader reader = new FileReader(args[0]);
			parser = new DashProfParser(reader);
			parser.execute(builder);
		}

		AbstractParser.main(args, parser, builder);
	}

	public static Category LOG = Category.getInstance(DashProfParser.class);
	
	private boolean      readHeader = false;
	private ModelBuilder builder = null;
	
	public DashProfParser(Reader reader)
	{
		super(new LineNumberReader(reader));
	}
	
	public boolean isFileFormatRecognized()
	{
		try
		{
			checkHeader();
			return true;
		}
		catch (ParseException x)
		{
		}
		catch (IOException x)
		{
		}
		return false;
	}

	public void execute(ModelBuilder builder) throws ParseException
	{
		this.builder = builder;
		builder.initialize(TimeData.Inclusive);
		
		String line;
		StringTokenizer tok;
		try
		{
			if ( !readHeader )
				checkHeader();
			while ( ( line = nextLine(false) ) != null &&
					!"".equals( line = line.trim() ) )
			{
				tok = new StringTokenizer(line, " ");
				int count = Integer.parseInt(nextToken(tok, true));
				String callee = nextToken(tok, true);
				String caller = nextToken(tok, true);
				long time = Long.parseLong(nextToken(tok, true));
				if ( time > 0 )
				{
					ModelBuilder.ID stackID = builder.newStackTrace(new String[]{ callee, caller });
					builder.newRecordedCall(stackID, count, time);
				}
			}
		}
		catch (IOException x)
		{
			throw new ParseException("IOException at line " + lineNumber() + " : " + x.getMessage());
		}
		finally
		{
			builder.end();
		}
	}

	private void checkHeader() throws ParseException, IOException
	{
		String line = nextLine(true);
		StringTokenizer tok = new StringTokenizer(line, " ");
		String countHeader = nextToken(tok, true);
		assertEqual("count", countHeader);
		String calleeHeader = nextToken(tok, true);
		assertEqual("callee", calleeHeader);
		String callerHeader = nextToken(tok, true);
		assertEqual("caller", callerHeader);
		String timeHeader = nextToken(tok, true);
		assertEqual("time", timeHeader);

		readHeader = true;
	}
}
