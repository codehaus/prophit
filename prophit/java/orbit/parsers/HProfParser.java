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
		{
			FileReader reader = new FileReader(args[0]);
			parser = new HProfParser(reader);
			parser.execute();
		}

		AbstractParser.main(args, parser);
	}

	private static final String TRACE = "TRACE";
	private static final String CPU_SAMPLES_BEGIN = "CPU SAMPLES BEGIN";
	private static final String CPU_SAMPLES_END = "CPU SAMPLES END";
	private static final String CPU_TIME_BEGIN = "CPU TIME (ms) BEGIN";
	private static final String CPU_TIME_END = "CPU TIME (ms) END";
		
	private ArrayList callIDs = null;
	private boolean readHeader = false;
	private int maxStackSize = 0;
	
	public HProfParser(Reader reader)
	{
		super(new LineNumberReader(reader));
	}

	public List getCallIDs()
	{
		return callIDs;
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

	public void execute() throws ParseException
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
		try
		{
			if ( !readHeader )
				checkHeader();
			seek("THREAD START (", true);
			String line = seek(TRACE, true);
			final HashMap stackTracesByID = new HashMap();
			line = parseTraces(line, stackTracesByID);
			if ( line == null )
			{
				throw new ParseException("File does not contain any CPU profile data. Make sure you are using a 'cpu' option with hprof");
			}
			else
			{
				final ArrayList rccList = new ArrayList();

				ParseTimingData parseTiming = null;
				
				if ( line.startsWith(CPU_SAMPLES_BEGIN) )
				{
					class ParseSamples
						implements ParseTimingData
					{
						int key = 1;
						HashMap rccsByStack = new HashMap();

						public int getKey()
						{
							return key;
						}

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

								StackTrace st = (StackTrace)stackTracesByID.get(traceID);
								RCC rcc = (RCC)rccsByStack.get(st);
								if ( rcc == null )
								{
									rcc = new RCC(st, count, count, key++);
									rccsByStack.put(st, rcc);
									rccList.add(rcc);
								}
								else
								{
									rcc.adjustTime(count);
									rcc.adjustCalls(count);
								}
							}
						}
					}			

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
					final int totalTime = Integer.parseInt(line.substring(totalBegin + totalEquals.length(), totalEnd));

					class ParseTimes
						implements ParseTimingData
					{
						int key = 1;
						HashMap rccsByStack = new HashMap();

						public int getKey()
						{
							return key;
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

								StackTrace st = (StackTrace)stackTracesByID.get(traceID);
								RCC rcc = (RCC)rccsByStack.get(st);
								if ( rcc == null )
								{
									rcc = new RCC(st, count, (long)time, key++);
									rccsByStack.put(st, rcc);
									rccList.add(rcc);
								}
								else
								{
									rcc.adjustTime(count);
									rcc.adjustCalls(count);
								}
							}
						}
					}			

					seek("rank", true);
					parseTiming = new ParseTimes();
				}

				parseTiming.execute();
				
				/*
				 * At each stack depth, if there is no existing stacktrace for a sub-stack of
				 * a stack trace, make a new RCC for the sub-stack and add it to the list
				 */
				int nextKey = parseTiming.getKey();
				HashSet stackTraceSet = new HashSet();
				ArrayList newRCCs = new ArrayList();
				for ( int size = maxStackSize; size > 0; --size )
				{
					// First add the 'natural' stacks at the current size
					for ( Iterator i = rccList.iterator(); i.hasNext(); )
					{
						RCC rcc = (RCC)i.next();
						if ( rcc.getStack().size() == size )
						{
							hashStack(stackTraceSet, rcc.getStack());
						}
					}
					newRCCs.clear();
					for ( Iterator i = rccList.iterator(); i.hasNext(); )
					{
						RCC rcc = (RCC)i.next();
						if ( rcc.getStack().size() > size )
						{
							StackTrace parentStack = rcc.getParentStack(size);
							if ( parentStack != null &&
								 stackTraceSet.add(parentStack) )
							{
								RCC newRCC = new RCC(parentStack, rcc.getCallCount(), 0, nextKey++);
								// System.out.println("Adding new rcc " + newRCC);
								newRCCs.add(newRCC);
								hashStack(stackTraceSet, parentStack);
							}
						}
					}
					rccList.addAll(newRCCs);
				}

				/*
				 * In general, the parent of a stack trace (the 'leaf') may only have a sub-set of the
				 *   parent calls that are listed in the leaf.
				 * This algorithm matches each stack trace up with its parent traces in a greedy manner,
				 *   finding all parents which match 'n' calls in the leaf before moving to 'n - 1'.
				 * Each time, the algorithm is only run on the RCCs which are still marked as being 'roots'
				 * Some of these roots may get matched to a parent, the rest will be tried again during
				 *   the next iteration
				 */
				ArrayList rootRCCList = new ArrayList(rccList.size());
				rootRCCList.addAll(rccList);

				ConstructCallsAlgorithm algorithm = new ConstructCallsAlgorithm(rccList.size());

				int maxSize = maxStackSize - 1;
				for ( int size = maxSize; size > 0; )
				{
					// System.out.println("Size : " + size);
					// System.out.println("Looking for " + rootRCCList);
					
					Map rccListByCallee = mapByCallee(rccList, size);

					// System.out.println("Callee map " + rccListByCallee);

					algorithm.execute(rootRCCList, rccListByCallee, size);
					callIDs = algorithm.getCallIDs();

					--size;
					// Don't bother to re-build the root list on the last time through
					if ( size > 0 )
					{
						rootRCCList.clear();
						for ( Iterator j = rccList.iterator(); j.hasNext(); )
						{
							RCC rcc = (RCC)j.next();
							int key = rcc.getKey();
							CallID callID = (CallID)callIDs.get(key);
							if ( callID != null && callID.getParentRCC() == null )
								rootRCCList.add(callID.getRCC());
						}
					}
				}
			}
		}
		catch (IOException x)
		{
			throw new ParseException("IOException at line " + lineNumber() + " : " + x.getMessage());
		}
	}

	private void hashStack(HashSet set, StackTrace st)
	{
		for ( int size = st.size(); size >= 1; --size )
		{
			set.add(st.getLeafStack(size));
		}
	}
	
	private Map mapByCallee(List rccList, int stackSize)
	{
		HashMap rccListByCallee = new HashMap();
		for ( Iterator i = rccList.iterator(); i.hasNext(); )
		{
			RCC rcc = (RCC)i.next();
			/*
			 * If there are longer stacks in the trace file, then the entire stack
			 * should be matched
			 */
			StackTrace calleeStack = rcc.getLeafStack(stackSize);
			/*
			if ( rcc.getStack().toString().startsWith("test.HelloList.buildAsStrings") ||
				 rcc.getStack().toString().startsWith("test.HelloList.linkedAndString") )
				System.out.println("Adding callee stack for " + rcc + " : " + calleeStack);
			*/
			if ( calleeStack != null )
			{
				ArrayList subList = (ArrayList)rccListByCallee.get(calleeStack);
				if ( subList == null )
				{
					subList = new ArrayList();
					rccListByCallee.put(calleeStack, subList);
				}
				subList.add(rcc);
			}
		}
		return rccListByCallee;
	}

	/**
	 * Reads TRACE blocks from the file
	 * This method terminates when it encounteres a line that does not start with one of 
	 *   'TRACE', 'CPU SAMPLES BEGIN', 'CPU TIME'
	 * It returns the first line that is not parsed as a trace (e.g. 'CPU SAMPLES BEGIN')
	 */
	private String parseTraces(String line, Map stackTracesByID) throws ParseException, IOException
	{
		HashMap map = new HashMap();
		HashMap lineCache = new HashMap();
		int lineCount = 0;
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
					String existing = (String)lineCache.get(line);
					if ( existing != null )
						line = existing;
					else
						lineCache.put(line, line);
					++lineCount;
				}
				stack.add(line);
			}
			String[] stackArray = (String[])stack.toArray(new String[stack.size()]);
			StackTrace st = new StackTrace(stackArray);
			// System.out.println(st);
			stackTracesByID.put(id, st);
			if ( stack.size() > maxStackSize )
				maxStackSize = stack.size();
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

	interface ParseTimingData
	{
		public int getKey();
		
		public void execute() throws ParseException, IOException;
	}
}
