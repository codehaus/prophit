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
		try
		{
			if ( !readHeader )
				checkHeader();
			seek("THREAD START (", true);
			String line = seek("TRACE", true);
			final HashMap stackTracesByID = new HashMap();
			line = parseTraces(line, stackTracesByID);

			if ( line.startsWith("CPU SAMPLES BEGIN") )
			{
				// eat the header line
				seek("rank", true);

				final ArrayList rccList = new ArrayList();

				class ParseSamples
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
						while ( !( line = nextLine(true) ).startsWith("CPU SAMPLES END") )
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

				ParseSamples ps = new ParseSamples();
				ps.execute();
				
				/*
				 * At each stack depth, if there is no existing stacktrace for a sub-stack of
				 * a stack trace, make a new RCC for the sub-stack and add it to the list
				 */
				int nextKey = ps.getKey();
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
			assertEqual(nextToken(tok, true), "TRACE");
			Integer id = Integer.valueOf(nextToken(tok, true));
			stack.clear();
			while ( ( line = nextLine(false) ) != null &&
					!line.startsWith("TRACE") &&
					!line.startsWith("CPU SAMPLES BEGIN") )
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
		while ( line != null && !line.startsWith("CPU SAMPLES BEGIN") );

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
}
