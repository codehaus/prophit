package orbit.parsers;

import orbit.model.*;

import java.io.*;
import java.util.*;

public class DashProfParser
	extends AbstractParser
{
	public static void main(String[] args) throws Exception
	{
		DashProfParser parser;
		{
			FileReader reader = new FileReader(args[0]);
			parser = new DashProfParser(reader);
			parser.execute();
		}

		AbstractParser.main(args, parser);
	}
		
	private ArrayList callIDs = null;
	private boolean readHeader = false;
	
	public DashProfParser(Reader reader)
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
		String line;
		StringTokenizer tok;

		final ArrayList rccList = new ArrayList();
		final HashMap   rccListByCallee = new HashMap();
		int key = 1;
		try
		{
			if ( !readHeader )
				checkHeader();
			while ( ( line = nextLine(false) ) != null )
			{
				tok = new StringTokenizer(line, " ");
				int count = Integer.parseInt(nextToken(tok, true));
				String callee = nextToken(tok, true);
				String caller = nextToken(tok, true);
				long time = Long.parseLong(nextToken(tok, true));
				if ( time > 0 )
				{
					StackTrace st = new StackTrace(new String[]{ callee, caller });
					RCC rcc = new RCC(st, count, time, key++);
					rccList.add(rcc);
					ArrayList subList = (ArrayList)rccListByCallee.get(rcc.getLeafStack(1));
					if ( subList == null )
					{
						subList = new ArrayList();
						rccListByCallee.put(rcc.getLeafStack(1), subList);
					}
					subList.add(rcc);
				}
			}
		}
		catch (IOException x)
		{
			throw new ParseException("IOException at line " + lineNumber() + " : " + x.getMessage());
		}
		System.out.println("Found " + rccList.size() + " recorded caller/callees in file");

		// System.out.println("Map : " + rccListByCallee);

		ConstructCallsAlgorithm algorithm = new ConstructCallsAlgorithm(rccList.size());
		algorithm.execute(rccList, rccListByCallee, 1);
		callIDs = algorithm.getCallIDs();
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
