package orbit.model;

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
}
