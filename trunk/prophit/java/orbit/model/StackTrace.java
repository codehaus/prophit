package orbit.model;

/**
 * A stack trace, represented by an array of strings. The 0th string should be the deepest (leaf) call,
 * the nth string should be the call closest to the root of the call stack.
 */
public class StackTrace
{
	private final String[] stack;
	private final int beginOffset;
	private final int endOffset;
	
	public StackTrace(String[] stack)
	{
		this(stack, 0, 0);
	}

	private StackTrace(String[] stack, int beginOffset, int endOffset)
	{
		this.stack = stack;
		this.beginOffset = beginOffset;
		this.endOffset = endOffset;
	}

	public int hashCode()
	{
		int code = 0;
		for ( int i = beginOffset; i < stack.length - endOffset; ++i )
		{
			code += stack[i].hashCode();
		}
		return code;
	}

	public boolean equals(Object other)
	{
		if ( other instanceof StackTrace )
			return equals((StackTrace)other);
		else
			return false;
	}

	/**
	 * True if other has the same size stack, and all the entries are equal.
	 */
	public boolean equals(StackTrace other)
	{
		if ( other == this )
			return true;
		int size = size();
		if ( size != other.size() )
			return false;
		for ( int i = 0; i < size; ++i )
		{
			if ( !stack[i + beginOffset].equals(other.stack[i + other.beginOffset]) )
				return false;
		}
		return true;
	}

	/**
	 * Retrieve the parent entries in the StackTrace. Return exactly <code>size</code>
	 * entries, starting with the one closest to the root. If there aren't that many entries, return null.
	 * @return null if the resulting stack is empty
	 */
	public StackTrace getParentStack(int size)
	{
		int delta = size() - size;
		if ( delta < 0 )
			return null;
		return new StackTrace(stack, beginOffset + delta, endOffset);
	}

	/**
	 * Retrieve the leaf-most entries in the StackTrace. Return exactly <code>size</code>
	 * entries, starting with the leaf. If there aren't that many entries, return null.
	 * @return null if the resulting stack is empty
	 */
	public StackTrace getLeafStack(int size)
	{
		int delta = size() - size;
		if ( delta < 0 )
			return null;
		return new StackTrace(stack, beginOffset, endOffset + delta);
	}

	public int size()
	{
		return stack.length - beginOffset - endOffset;
	}

	/**
	 * @return name of the leaf-most method (0th index).
	 */
	public String getLeafMethod()
	{
		if ( size() > 0 )
			return stack[beginOffset];
		else
			return null;
	}

	/**
	 * @return name of the parent of the leaf-most method (1st index), or null.
	 */
	public String getLeafParentMethod()
	{
		if ( size() > 1 )
			return stack[beginOffset + 1];
		else
			return null;
	}

	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		for ( int i = beginOffset; i < stack.length - endOffset; ++i )
		{
			sb.append(stack[i]);sb.append("\n");
		}
		return sb.toString();
	}
}
