package orbit.parsers;

import orbit.model.*;
import orbit.util.Log;
import orbit.util.XMLConstants;

import org.apache.log4j.Category;
import net.n3.nanoxml.*;

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;
import java.util.zip.*;

public class ProphitParserLoader implements Parser, Loader
{
	public static void main(String[] args) throws Exception
	{
		ProphitParserLoader parser;
		ProphitParserLoader loader;
		{
			/** At some point, I'll make this File Reader read a zipped
			 *  File Reader so that we can deal with compressed data. 
			 *  Or we can make it a subclass/commandline flag, etc. 
			 */ 
			FileReader reader = new FileReader(args[0]);
			parser = new ProphitParserLoader(reader);
			// Builder argument can be null to ProphitParserLoader
			parser.execute(null);
			loader = new ProphitParserLoader(parser, null, new File(args[0]));
		}
	
	}

	public static Category LOG = Category.getInstance(ProphitParserLoader.class);
    
	private boolean readHeader = false;
	protected final Reader reader;
	protected final Solver solver;
	protected final File file;
	protected final Parser parser;
	protected boolean parsed = false;
	protected CallGraph callgraph = null;

	/** 
	 * If we're being instantiated as a parser, use this constructor.
	 */
	public ProphitParserLoader(Reader reader)
	{
		// we need to convert this to zip format at some point soon...
		this.reader = (new LineNumberReader(reader)); // not sure if we want to do this.
		this.parser = this;  // we are our own parser. (and we are also a loader)
		this.solver = null; // we don't need this, as our solve step is essentially empty
		this.file = null;  // this was throw away info anyway - we really just needed the reader
	}

	/**
	 * if we're being constructed as a loader, use this constructor.
	 */
	public ProphitParserLoader(Parser parser, Solver solver, File file)
	{
		this.reader = null;    // if we're passed the parser, it will take care of all the "reading" aspects anyway. 
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
		return ("");
	}

	public String getWarning()
	{
		return ("");
	}

	public synchronized void parse() throws ParseException
	{
		this.parser.execute(null);
		parsed = true;
	}

	public synchronized CallGraph solve()
	{
		if (!parsed) {
			try {
				this.parse();
			} catch (ParseException p) {
				System.out.println(p.getMessage());
			}
		}
		return ( this.callgraph );
	}

	private void verifyFile() throws ParseException, IOException
	{
		/* might call a DTD validator here, or just check for the 
		 * existence of a few elements from the xml file that we are expecting. 
		 */
		String match = new String(XMLConstants.FILEMATCH);
		String firstline = ((LineNumberReader)this.reader).readLine().trim();

		if ( !(firstline.startsWith(match)))
		{
			throw new ParseException("File Type does not match");
		}
	
	}

	public boolean isFileFormatRecognized()
	{
		try
		{
			verifyFile();
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

	/* 
	 * We're not really going to use the builder passed in, but the interface defines and expects
	 * a builder to be passed in. Since we won't need a "solving" step when loading our own 
	 * internal format, we'll be fine with just accessing the various classes directly. 
	 */
	/**
	 * Parse all of the data, build a call-graph data model from the data.
	 * call-graph  --> CallGraph
	 * measurement --> RCC
	 * invocation  --> CallID
	 * stacktrace  --> stacktrace
	 *
	 * @param builder   This param is ignored in this particular implementation of the Parser interface.
	 * It can be null.
	 * @throws ParseException   If there is an exception it will likely be rethrown as a parsing exception.
	 */
	public void execute(ModelBuilder builder) throws ParseException
	{
		try {
			// initialize the xml parser
			IXMLParser xmlparser = (IXMLParser)XMLParserFactory.createDefaultXMLParser();
			IXMLReader xmlreader = new XMLReader(this.reader);
			xmlparser.setReader(xmlreader);
			// get the toplevel xml element from the profile data document.
			IXMLElement callgraph = (IXMLElement) xmlparser.parse();
	    
			Log.debug(LOG, "ELEMENT: ", callgraph.getFullName());
			//Log.debug(LOG, "User: ", callgraph.getFirstChildNamed(XMLConstants.USER).getContent());

			this.callgraph = makeNewCallGraph( callgraph );

			//XMLWriter writer = new XMLWriter(System.out);
			//writer.write(callgraph);
		}

		catch (XMLException x)
		{
			Log.error(LOG, x);
			if (x.getException() != null) 
			{
				Log.error(LOG, x.getException());
				throw new ParseException("XMLException at line " + x.getLineNr() + " : " + x.getException().getMessage());
			} 
			else 
			{
				throw new ParseException("XMLException at line " + x.getLineNr() + " : " + x.getMessage());
			}
		
		}
		catch (ClassNotFoundException x)
		{
			Log.error(LOG, x);
			throw new ParseException("ClassNotFoundException : XML Parser not found : " + x.getMessage());
		}
		catch (InstantiationException x)
		{
			Log.error(LOG, x);
			throw new ParseException("InstantiationException : XML Parser not instantiated : " + x.getMessage());
		}
		catch (IllegalAccessException x)
		{
			Log.error(LOG, x);
			throw new ParseException("IllegalAccessException : XML Parser not accessible : " + x.getMessage());
		}
	}

	/**
	 * given an xml element for a stacktrace member, create a stack trace object. this requires
	 * examining the child elements (methods) and putting them in a string array. 
	 * @param st    stacktrace xml element structure. 
	 * @return      the StackTrace instance which corresponds to the XML Element passed in.
	 */
	private StackTrace makeNewStackTrace(IXMLElement st) throws XMLException
	{
		Vector vMethods = st.getChildrenNamed(XMLConstants.METHOD);
		String[] methods = new String[vMethods.size()];

		for (int i = 0; i < vMethods.size(); i++)
		{
			IXMLElement m = (IXMLElement)vMethods.elementAt(i);
			methods[i] = m.getContent();
			Log.debug(LOG, "METHOD: ", m.getContent());
		}    
		StackTrace s = new StackTrace( methods );
		return ( s );
	}

	/**
	 * given a measurement xml element, generate an RCC instance, which represents that measurement. 
	 * this also calls makeNewStackTrace. 
	 * @param measurement    The measured time and number of calls for a particular measurement. 
	 * @return               An RCC instance with all the relevant data. 
	 */
	private RCC makeNewRCC(IXMLElement measurement) throws XMLException
	{
		StackTrace st = makeNewStackTrace(measurement.getFirstChildNamed(XMLConstants.STACKTRACE));
	
		RCC rcc = new RCC(st, 
								Integer.parseInt(measurement.getAttribute(XMLConstants.NUMCALLS)), 
								Long.parseLong(measurement.getAttribute(XMLConstants.TIME)), 
								-1, 
								Integer.parseInt(measurement.getAttribute(XMLConstants.ID)));
	
		return ( rcc );
	}

	/**
	 * Make a new CallID instance. Link to the appropriate RCC instances, and associate a key. 
	 * @param invocation   the xml element which represents a callID.
	 * @param rccArray     the array of RCC instances, for easy access.
	 * @param key          the unique integer key of the CallID (must be larger than the largest RCC id.
	 * @return             A new CallID instance with appropriate initializations. 
	 */
	private CallID makeNewCallID(IXMLElement invocation, RCC[] rccArray, int key) throws XMLException
	{
		/*try {
		  XMLWriter writer = new XMLWriter(System.out);
		  System.out.println("WRITING:  key = " + String.valueOf(key));
		  writer.write(invocation);
		  } catch (Exception x) { 
		  System.out.println(x.getMessage());
		  }
		*/
	
		int  rccID   = Integer.parseInt(invocation.getAttribute(XMLConstants.MYID));
		int parentID = Integer.parseInt(invocation.getAttribute(XMLConstants.PARENTID, "-1"));
		CallID cid = null;
	
		cid = new CallID( rccArray[rccID], ((parentID != -1) ?  rccArray[parentID] : null), key );
		Log.debug(LOG, "New CallID : ", cid);
		return (cid);
	}

	/**
	 * extracts the call fraction from the prophit data file. 
	 * @param invocation    The unique instance of caller->callee
	 * @return  The call fraction attribute of the xml element, OR -1 if no such call fraction is recorded.
	 */
	private double makeNewCallFraction(IXMLElement invocation) throws XMLException
	{
		// this should get the attribute callFraction, or -1 if there is no such attribute. 
		// then turn it into a double and store on the local var "fraction"
		//System.out.println("Fraction: " + invocation.getAttribute("callFraction", "-1"));
		double fraction = Double.parseDouble(invocation.getAttribute(XMLConstants.FRACTION, "-1"));
		return ( fraction );
	}

	/** 
	 * Make all the RCC instances we need for the call graph. 
	 * @param measurements  the xml elements representing measurements of recorded calls. 
	 * @param rccArray      the array of recorded calls (RCC's) to return constructed instances in. 
	 */
	private void processMeasurements( Vector measurements, RCC[] rccArray ) throws XMLException
	{
		RCC r;
		IXMLElement measurement;
		for (int i = 0; i < measurements.size(); i++)
		{
			measurement = (IXMLElement)measurements.elementAt(i);
			r = makeNewRCC(measurement);
			rccArray[r.getKey()] = r;
		}

	}
    
	/**
	 * process all the invocation xml elements and produce the data structures to hold call id's and 
	 * call fractions... 
	 * @param invocations  The XML elements representing a unique invocation type.
	 * @param rccArray     The array of RCC's
	 * @param calls        The array to return CallIDs into. 
	 * @param callFractions The array to return fractions into. 
	 *
	 */
	private void processInvocations( Vector invocations, RCC[] rccArray, CallID[] calls, double[] callFractions ) throws XMLException
	{
		CallID c;
		double fraction;
		IXMLElement invocation;
		int callkey = rccArray.length;

		for (int i = 0; i < invocations.size(); i++)
		{
			invocation = (IXMLElement)invocations.elementAt(i);
			fraction = makeNewCallFraction(invocation);
			// if fraction != -1 then we have a callfraction to process as well (not a proxy call)
			c = makeNewCallID(invocation, rccArray, (fraction != -1) ? callkey + i : -1);
	    
			// we use getKey() to place the callID into the index. 
			// quite simply, that is what the CallGraph constructor is expecting, that 
			// a callID's key will match the parent RCC key, or else it will be a proxy call.
			callFractions[c.getKey()] = fraction;
			calls[c.getKey()] = c;
			//System.out.println("CALLID: " + calls[c.getKey()]);
		}

	}

	/**
	 * taking the xml structure of a callgraph from a data file, walk the structure and create the 
	 * objects which prophIt will reason about:  RCC's, CallIDs, and StackTraces. 
	 * @param callgraph    the top-level xml element for the call graph. 
	 * @return   A completed CallGraph instance. 
	 */
	private CallGraph makeNewCallGraph(IXMLElement callgraph) throws XMLException
	{
		Vector vMeasurements = callgraph.getChildrenNamed(XMLConstants.MEASUREMENT);
		Vector vInvocations = callgraph.getChildrenNamed(XMLConstants.INVOCATION);

		RCC[] rccArray = new RCC[vMeasurements.size()+1];	    
		processMeasurements( vMeasurements, rccArray );

		LOG.info("Finished Measurements.");	

		// we need to guarantee the array is long enough... 
		CallID[] calls = new CallID[vInvocations.size() + rccArray.length]; 
		double[] callFractions = new double[vInvocations.size() + rccArray.length];

		processInvocations( vInvocations, rccArray, calls, callFractions );

		LOG.info("Finished Invocations. " + calls.length);
	
		CallGraph cg;
		try 
		{
			cg = new CallGraph(calls, callFractions);
			LOG.info("ProphitParserLoader completed successfully");
			return (cg);
		}
		catch (Exception x) 
		{
			LOG.error("Exception occurred creating callgraph");
			Log.error(LOG, x);
			return ( null );
		}
	}

	/** Resolves the DTD 'profile-data.dtd' as a resource */
	private class XMLReader
		extends StdXMLReader
	{
		public XMLReader(Reader reader)
		{
			super(reader);
		}

		public Reader openStream(String publicID, String systemID)
			throws MalformedURLException,
					 FileNotFoundException,
					 IOException
		{
			if ( systemID != null && systemID.endsWith("profile-data.dtd") )
			{
				Log.debug(LOG, "Loading profile-data.dtd as a resource");
				return new InputStreamReader(getClass().getClassLoader().getResourceAsStream("profile-data.dtd"));
			}
			return super.openStream(publicID, systemID);
		}		
	}
}
    
