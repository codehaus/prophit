package orbit.util;

import java.util.EmptyStackException;

public class IntStack
{
	private int[] stack;
	private int   current = 0;

	public IntStack()
	{
		stack = new int[10];
		for ( int i = 0; i < stack.length; ++i )
			stack[i] = -1;
	}

	public IntStack(IntStack other)
	{
		this(other, 0);
	}
	   
	public IntStack(IntStack other, int skip)
	{
		this.stack = new int[other.current - skip + 1];
		System.arraycopy(other.stack, skip, this.stack, 0, other.current - skip);
		this.current = other.current - skip;
	}
		
	public Object clone()
	{
		return new IntStack(this);
	}
		
	public boolean equals(Object other)
	{
		return equals((IntStack)other);
	}

	public boolean equals(IntStack other)
	{
		if ( current != other.current )
			return false;
		for ( int i = 0; i < current; ++i )
		{
			if ( stack[i] != other.stack[i] )
				return false;
		}
		return true;
	}
		
	public int hashCode()
	{
		int hash = 0;
		for ( int i = 0; i < 4 && i < current; ++i )
		{
			int bit = stack[i];
			hash += stack[i];
		}
		return hash;
	}

	public boolean isEmpty()
	{
		return size() == 0;
	}

	public void trimToSize()
	{
		int[] data = new int[size()];
		System.arraycopy(this.stack, 0, data, 0, data.length);
		this.stack = data;
	}
	
	public int size()
	{
		return current;
	}

	public int top()
	{
		return get(size() - 1);
	}

	public IntIterator iterator()
	{
		return new IntIterator()
			{
				int index = 0;

				public boolean hasNext()
				{
					return index < current;
				}

				public int next()
				{
					return stack[index++];
				}
			};
	}
	
	public IntIterator reverseIterator()
	{
		return new IntIterator()
			{
				int index = current - 1;

				public boolean hasNext()
				{
					return index >= 0;
				}

				public int next()
				{
					return stack[index--];
				}
			};
	}
	
	public int get(int index)
	{
		if ( index < 0 || index >= current )
		{
			throw new ArrayIndexOutOfBoundsException("IntStack index must be in the range [0..." + current + ")");
		}
		return stack[index];
	}
	
	public boolean contains(int bit)
	{
		for ( int i = 0; i < current; ++i )
		{
			if ( stack[i] == bit )
				return true;
		}
		return false;
	}

	public IntStack removeFirst()
	{
		return new IntStack(this, 1);
	}
		
	public void push(int bit)
	{
		if ( current == stack.length )
		{
			int[] newStack = new int[stack.length * 2];
			System.arraycopy(stack, 0, newStack, 0, stack.length);
			for ( int i = stack.length; i < newStack.length; ++i )
				newStack[i] = -1;
			stack = newStack;
		}
		stack[current] = bit;
		++current;
	}

	public int pop()
	{
		if ( current < 1 )
		{
			throw new EmptyStackException();
		}
		--current;
		int bit = stack[current];
		stack[current] = -1;
		return bit;
	}

	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(current);
		sb.append(" [");
		for ( int i = 0; i < current; ++i )
		{
			sb.append(" ");sb.append(stack[i]);
		}
		sb.append(" ]");
		return sb.toString();
	}
}
