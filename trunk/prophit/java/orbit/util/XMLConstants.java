package orbit.util;

public interface XMLConstants
{

	/** these are the element string names **/
	public static final String CALLGRAPH = "call-graph";
	public static final String MEASUREMENT = "measurement";
	public static final String STACKTRACE = "stacktrace";
	public static final String METHOD = "method";
	public static final String INVOCATION = "invocation";
	public static final String DATE = "date";
	public static final String USER = "user";
	
	/** these are the attribute string names **/
	public static final String ID = "id";
	public static final String TIME = "time";
	public static final String NUMCALLS = "numCalls";
	public static final String PARENTID = "parentMeasurementID";
	public static final String MYID = "measurementID";
	public static final String FRACTION = "callFraction";

	/** identify as an appropriate xml file **/
	public static final String FILEMATCH = "<?xml";

	/** complete header string for xml documents. **/
	public static final String HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n<!DOCTYPE call-graph SYSTEM \"profile-data.dtd\" >\n\n";
	public static final int HEADERLEN = HEADER.length();
   
}
