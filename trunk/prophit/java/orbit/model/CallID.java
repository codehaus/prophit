package orbit.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CallID
{
	private final RCC rcc;
	private final RCC parent;
	private final int key;
	
	public CallID(RCC rcc, RCC parent)
	{
		this(rcc, parent, -1);
	}

	public CallID(RCC rcc, RCC parent, int key)
	{
		this.rcc = rcc;
		this.parent = parent;
		this.key = key;
	}

	public boolean isProxy()
	{
		return key != -1;
	}

	public RCC getRCC() { return rcc; }
	public RCC getParentRCC() { return parent; }
	public int getParentRCCKey()
	{
		if ( parent != null )
			return parent.getKey();
		else
			return -1;
	}
	public int getKey()
	{
		if ( key != -1 )
			return key;
		else
			return rcc.getKey();
	}
	
	public String toString()
	{
		return rcc.getLeafParentMethodName() + " -> " + rcc.getLeafMethodName() + " [ " + getParentRCCKey() + " -> " + ( key != -1 ? "p" : "" ) + getKey() + " ]";
	}

		/**
	 * From a List of callIDs, return the list of callIDs for whom {@link CallID#isProxy} is true.
	 */
	public static List getProxyCallIDs(List callIDs)
	{
		ArrayList proxyCalls = new ArrayList(callIDs.size());
		for ( Iterator i = callIDs.iterator(); i.hasNext(); )
		{
			CallID callID = (CallID)i.next();
			if ( callID != null && callID.isProxy() )
				proxyCalls.add(callID);
		}
		return proxyCalls;
	}
}
