package orbit.parsers;

import orbit.model.CallGraph;
import orbit.model.CallID;
import orbit.solver.SocketConnection;

import java.io.*;
import java.util.*;

public abstract class AbstractParser
	implements Parser
{
	protected final LineNumberReader reader;
	private int token = 0;

	protected static void main(String[] args, AbstractParser parser) throws Exception
	{
		List callIDs = parser.getCallIDs();
		if ( "-debug".equals(args[1]) )
		{
			System.out.println(parser.getCallIDs());
		}
	}
	
	/**
	 * @return true if the parser believes it can read the file.
	 */
	public abstract boolean isFileFormatRecognized();

	public abstract List getCallIDs();
	
	public abstract void execute() throws ParseException;

	protected AbstractParser(LineNumberReader reader)
	{
		this.reader = reader;
	}
	
	protected void assertEqual(String expected, String actual) throws ParseException
	{
		if ( !expected.equals(actual) )
		{
			throw new ParseException("Expected '" + expected + "' at token " + token + ", line " + lineNumber() + ". Got '" + actual + "'");
		}
	}
	
	protected String nextToken(StringTokenizer tok, boolean must) throws ParseException
	{
		if ( !tok.hasMoreTokens() )
		{
			if ( must )
				throw new ParseException("Expected token " + token + " at line " + lineNumber());
			else
				return null;
		}
		String token = tok.nextToken();
		// Parse '<unknown caller>'
		if ( "<unknown".equals(token) )
		{
			return token + " " + nextToken(tok, true);
		}
		else
		{
			return token;
		}
	}

	protected String nextLine(boolean must) throws ParseException, IOException
	{
		String line = reader.readLine();
		if ( line == null && must )
		{
			throw new ParseException("Unexpected end of file at line " + lineNumber());
		}
		token = 0;
		return line;
	}

	protected int lineNumber()
	{
		return reader.getLineNumber();
	}
}
