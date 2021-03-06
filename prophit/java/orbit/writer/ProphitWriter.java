package orbit.writer;

import net.n3.nanoxml.IXMLElement;
import net.n3.nanoxml.XMLElement;
import net.n3.nanoxml.XMLWriter;
import orbit.model.CallGraph;
import orbit.model.CallID;
import orbit.model.RCC;
import orbit.model.StackTrace;
import orbit.util.XMLConstants;

import org.apache.log4j.Category;

import java.io.*;
import java.util.Date;

public class ProphitWriter
{

	public static void main(String[] args) throws Exception
	{

	}

	public static Category LOG = Category.getInstance(ProphitWriter.class);
	protected final CallGraph callgraph;
	//protected XMLWriter xmlwriter;
	protected Writer writer;
	
	public ProphitWriter(CallGraph cg, File file) throws FileNotFoundException, IOException
	{
		this(cg, new FileWriter(file));
	}

	public ProphitWriter(CallGraph cg, Writer writer)
	{
		this.callgraph = cg;
		this.writer = writer;
	}

	public void write() throws IOException
	{
		IXMLElement cg = callGraphToXML(this.callgraph);
		this.writer.write( XMLConstants.HEADER );
		XMLWriter xmlwriter = new XMLWriter(this.writer);
		xmlwriter.write( cg, true );
	}

	public IXMLElement callGraphToXML(CallGraph callgraph)
	{
		CallID[] calls = callgraph.getCallIDs();
		double[] callFractions = callgraph.getFractions();
		RCC[] rccs = RCC.getRCCs(calls);

		IXMLElement cg = new XMLElement(XMLConstants.CALLGRAPH);
		IXMLElement date = cg.createElement(XMLConstants.DATE);
		date.setContent( new Date().toString() );
		cg.addChild( date );
		addMeasurements(cg, rccs);
		addInvocations(cg, calls, callFractions);
		return ( cg );
	}
	
	public void addMeasurements(IXMLElement parent, RCC[] rccs)
	{
		for (int i = 0; i < rccs.length; i++ )
		{
			if (rccs[i] != null) 
			{
				//LOG.info("rcc at index: " + i + " is: " + rccs[i]);
				IXMLElement measurement = rccToXML(rccs[i]);
				parent.addChild(measurement);
			} //else LOG.info("rcc at index: " + i);
		}
	}		

	public void addInvocations(IXMLElement parent, CallID[] calls, double[] fractions)
	{
		for (int i = 0; i < calls.length; i++)
		{
			if (calls[i] != null)
			{
				IXMLElement invocation = callIDToXML(calls[i], fractions[i]);
				parent.addChild(invocation);
			} //else LOG.info("Call at index: " + i);
		}
	}

	public IXMLElement callIDToXML(CallID id, double fraction)
	{
		IXMLElement invocation = new XMLElement(XMLConstants.INVOCATION);
		
		if ( id.getParentRCCKey() != -1 )
		{
			invocation.setAttribute(XMLConstants.PARENTID, String.valueOf(id.getParentRCCKey()));
		}
		invocation.setAttribute(XMLConstants.MYID, String.valueOf(id.getRCC().getKey()));
		if ( id.isProxy() )
		{
			invocation.setAttribute(XMLConstants.FRACTION, String.valueOf(fraction));
		}
		return ( invocation );
	}

	public IXMLElement stackTraceToXML(StackTrace st)
	{
		IXMLElement stacktrace = new XMLElement(XMLConstants.STACKTRACE);
		int size = st.size();
		for (int i = size - 1; i >= 0; --i )
		{
			IXMLElement method = stacktrace.createElement(XMLConstants.METHOD);
			method.setContent(st.getMethod(i));
			stacktrace.addChild(method);
		}
		return ( stacktrace );
	}

	public IXMLElement rccToXML(RCC rcc)
	{
		//System.out.println("TIME: " + rcc.getTime());
		//System.out.println(rcc.toString());
		IXMLElement measurement = new XMLElement(XMLConstants.MEASUREMENT);
		measurement.setAttribute(XMLConstants.ID, String.valueOf(rcc.getKey()));
		if (rcc.getTime() != -1) 
			measurement.setAttribute(XMLConstants.TIME, String.valueOf(rcc.getTime()));
		else
			measurement.setAttribute(XMLConstants.TIME, String.valueOf(rcc.getExclusiveTime()));

		measurement.setAttribute(XMLConstants.NUMCALLS, String.valueOf(rcc.getCallCount()));
		IXMLElement stacktrace = stackTraceToXML(rcc.getStack());		
		measurement.addChild(stacktrace);
		return ( measurement );
	}

}
