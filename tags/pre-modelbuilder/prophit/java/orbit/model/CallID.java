package orbit.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A CallID represents a location in the call graph. It is the combination of a parent {@link RCC} (stack trace)
 * calling a child RCC (stack trace). An RCC may appear many times in the call graph. Each of these appearences
 * is represented by a CallID.
 * <p>
 * A CallID which represents one of multiple instantiations of an RCC is referred to as a 'proxy call'. To understand
 * proxy calls, consider a profile file which contains the following entries:
 * <pre>
 * Caller       Callee
 * ------       ------
 * init         executeQuery
 * test         executeQuery
 * executeQuery sqlParse
 * </pre>
 * In this case, the RCC ( executeQuery, sqlParse ) is invoked from two parent RCCs: ( init, executeQuery ) and
 * ( test, executeQuery ). Thus, there will be two CallIDs whose RCC is ( executeQuery, sqlParse ).
 */
public class CallID
{
	private final RCC rcc;
	private final int key;

	private RCC parent = null;

	public CallID(RCC rcc, RCC parent)
	{
		this(rcc, parent, -1);
	}

	/**
	 * @param key in the case that this is a proxy call, the key should be an integer which is unique
	 * to all the CallIDs. For non-proxy calls, the {@link #getKey key} is the key of the RCC.
	 * @see #getKey
	 * @see RCC#getKey
	 */
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

	public RCC getRCC()       { return rcc; }
	public RCC getParentRCC() { return parent; }

	/**
	 * @return the {@link RCC#getKey} of the parent RCC, or -1 if this CallID has no parent.
	 */
	public int getParentRCCKey()
	{
		if ( parent != null )
			return parent.getKey();
		else
			return -1;
	}

	/**
	 * @return a number which uniquely identifies this CallID.
	 */
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
	 * From a List of callIDs, return the list of callIDs for whom {@link #isProxy} is true.
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

	void setParent(RCC rcc)
	{
		if ( parent != null )
			throw new IllegalArgumentException("parent is non-null in CallID.setParent");
		this.parent = rcc;
	}
}
