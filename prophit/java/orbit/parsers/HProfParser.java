package orbit.parsers;

import orbit.model.*;

import java.io.*;
import java.util.*;

/**
 * Parses text output of 'java -Xrunhprof'.
 */
public class HProfParser
	extends AbstractParser
{
	public static void main(String[] args) throws Exception
	{
		HProfParser parser;
		ModelBuilder builder = ModelBuilderFactory.newModelBuilder();
		{
			FileReader reader = new FileReader(args[0]);
			parser = new HProfParser(reader);
			parser.execute(builder);
		}

		AbstractParser.main(args, parser, builder);
	}

	private static final String TRACE = "TRACE";
	private static final String CPU_SAMPLES_BEGIN = "CPU SAMPLES BEGIN";
	private static final String CPU_SAMPLES_END = "CPU SAMPLES END";
	private static final String CPU_TIME_BEGIN = "CPU TIME (ms) BEGIN";
	private static final String CPU_TIME_END = "CPU TIME (ms) END";

	private ModelBuilder builder = null;
	private boolean      readHeader = false;
	
	public HProfParser(Reader reader)
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
		/*
		 * If the header has not been read, read it
		 * Look for the first 'THREAD START ('
		 * Look for the first 'TRACE'
		 * Parse all the traces
		 * If next line is CPU TIME
		 *   parse CPU times
		 * Else if the next line is CPU SAMPLES BEGIN
		 *   parse CPU samples
		 */
		this.builder = builder;
		builder.initialize(TimeData.Exclusive);
		
		try
		{
			if ( !readHeader )
				checkHeader();
			seek("THREAD START (", true);
			String line = seek(TRACE, true);
			HashMap stackTracesByID = new HashMap();
			line = parseTraces(line, stackTracesByID);
			if ( line == null )
			{
				throw new ParseException("File does not contain any CPU profile data. Make sure you are using a 'cpu' option with hprof");
			}

			ParseTimingData parseTiming = null;
				
			if ( line.startsWith(CPU_SAMPLES_BEGIN) )
			{
				seek("rank", true);
				parseTiming = new ParseSamples();
			}
			else if ( line.startsWith(CPU_TIME_BEGIN) )
			{
				String totalEquals = "total = ";
				int totalBegin = line.indexOf(totalEquals);
				if ( totalBegin == -1 )
					throw new ParseException("ParseException at line " + lineNumber() + 
											 ". Expected line to contain '(" + totalEquals + "'");
				int totalEnd = line.indexOf(")", totalBegin);
				if ( totalBegin == -1 )
					throw new ParseException("ParseException at line " + lineNumber() + 
											 ". Expected line to contain '(" + totalEquals + "<some number>)'");
				long totalTime = Long.parseLong(line.substring(totalBegin + totalEquals.length(), totalEnd));

				seek("rank", true);
				parseTiming = new ParseTimes(totalTime);
			}
			else
			{
				throw new ParseException("Unexpected line " + line + " at line " + lineNumber());
			}
			
			ArrayList rccList = new ArrayList();

			parseTiming.initialize(stackTracesByID, rccList);
			parseTiming.execute();
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

	/**
	 * Reads TRACE blocks from the file
	 * This method terminates when it encounteres a line that does not start with one of 
	 *   'TRACE', 'CPU SAMPLES BEGIN', 'CPU TIME'
	 * It returns the first line that is not parsed as a trace (e.g. 'CPU SAMPLES BEGIN')
	 */
	private String parseTraces(String line, Map stackTracesByID) throws ParseException, IOException
	{
		ArrayList stack = new ArrayList();
		do
		{
			// These are the non-useful characters in the file. They look fun in this order.
			StringTokenizer tok = new StringTokenizer(line, "( =:)");
			assertEqual(nextToken(tok, true), TRACE);
			Integer id = Integer.valueOf(nextToken(tok, true));
			stack.clear();
			while ( ( line = nextLine(false) ) != null &&
					!"".equals( line = line.trim() ) &&
					!line.startsWith(TRACE) &&
					!line.startsWith(CPU_TIME_BEGIN) &&
					!line.startsWith(CPU_SAMPLES_BEGIN) )
			{
				line = line.trim();
				// Strip out line number, "Native method", "Unknown line"
				int colon = line.lastIndexOf(':');
				// String lineNumber = "<Unknown line>";
				if ( colon != -1 )
				{
					int paren = line.lastIndexOf(')');
					int size = colon + line.length() - paren;
					// lineNumber = line.substring(colon + 1, paren);
					StringBuffer sb = new StringBuffer(size);
					sb.append(line.substring(0, colon));
					sb.append(line.substring(paren, line.length()));
					line = sb.toString();
				}
				stack.add(line);
			}
			String[] stackArray = (String[])stack.toArray(new String[stack.size()]);
			ModelBuilder.ID stackID = builder.newStackTrace(stackArray);
			// System.out.println(st);
			stackTracesByID.put(id, stackID);
		}
		while ( line != null && 
				!line.startsWith(CPU_TIME_BEGIN) &&
				!line.startsWith(CPU_SAMPLES_BEGIN) );

		// System.out.println("Found " + lineCache.size() + " unique lines among " + lineCount + " lines");

		return line;
	}

	private void checkHeader() throws ParseException, IOException
	{
		String line = nextLine(true);
		if ( !line.startsWith("JAVA PROFILE") )
			throw new ParseException("File is not an HProf text file");
		readHeader = true;
	}

	/**
	 * @return the next line that startsWith the 'start' string.
	 */
	private String seek(String start, boolean must) throws ParseException, IOException
	{
		String line;
		// Eat space up until the file ends or the 'start' text is found
		while ( ( line = nextLine(must) ) != null &&
				!line.startsWith(start) )
		{ }
		return line;
	}

	abstract class ParseTimingData
	{
		private Map  rccsByStack     = new HashMap();
		private Map  stackTracesByID = null;
		private List rccList         = null;

		public void initialize(Map stackTracesByID, List rccList)
		{
			this.stackTracesByID = stackTracesByID;
			this.rccList = rccList;
		}
		
		public abstract void execute() throws ParseException, IOException;

		protected void addRCC(Integer traceID, int nCalls, long time) throws ParseException
		{
			ModelBuilder.ID stackID = (ModelBuilder.ID)stackTracesByID.get(traceID);
			if ( stackID == null )
				throw new ParseException("TRACE id " + traceID + " not found at line " + lineNumber());
			builder.newRecordedCall(stackID, nCalls, time);
		}
	}

	class ParseSamples
		extends ParseTimingData
	{
		public void execute() throws ParseException, IOException
		{
			String line;
			while ( !( line = nextLine(true) ).startsWith(CPU_SAMPLES_END) )
			{
				StringTokenizer tok = new StringTokenizer(line, " ");
				String rank = nextToken(tok, true);
				String self = nextToken(tok, true);
				String accum = nextToken(tok, true);
				int count = Integer.parseInt(nextToken(tok, true));
				Integer traceID = Integer.valueOf(nextToken(tok, true));
				String method = nextToken(tok, true);

				addRCC(traceID, count, count);
			}
		}
	}			

	class ParseTimes
		extends ParseTimingData
	{
		private final long totalTime;
	
		public ParseTimes(long totalTime)
		{
			this.totalTime = totalTime;
		}

		public void execute() throws ParseException, IOException
		{
			String line;
			while ( !( line = nextLine(true) ).startsWith(CPU_TIME_END) )
			{
				StringTokenizer tok = new StringTokenizer(line, " ");
				String rank = nextToken(tok, true);
				String selfPercentStr = nextToken(tok, true);
								// Strip off the '%' and divide by 100
				double selfFraction = Double.parseDouble(selfPercentStr.substring(0, selfPercentStr.length() - 1)) / 100.0;
				String accum = nextToken(tok, true);
				int count = Integer.parseInt(nextToken(tok, true));
				Integer traceID = Integer.valueOf(nextToken(tok, true));
				String method = nextToken(tok, true);

				double time = selfFraction * totalTime;

				addRCC(traceID, count, (long)time);
			}
		}
	}
}
