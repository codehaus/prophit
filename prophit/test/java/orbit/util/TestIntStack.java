package orbit.util;

import util.*;

import junit.framework.TestCase;

import java.util.EmptyStackException;

public class TestIntStack
	extends TestCase
{
	public TestIntStack(String name)
	{
		super(name);
	}

	public void testIntStack()
	{
		IntIterator iterator;
		IntStack stack = new IntStack();

		// Test an empty stack
		boolean exception = false;
		try
		{
			stack.pop();
		}
		catch (EmptyStackException x)
		{
			exception = true;
		}
		assert("Expected EmptyStackException", exception);

		// Test basic pushing, hashCode, size
		stack.push(1);
		stack.push(2);
		stack.push(3);

		assert("Expected hashCode = 6", stack.hashCode() == 6);
		assert("Expected size = 3", stack.size() == 3);

		// Test clone, pushing on a clone
		IntStack copy = (IntStack)stack.clone();

		assert("Expected copy == stack", copy.equals(stack));
		assert("Expected hashCode = 6", copy.hashCode() == 6);
		assert("Expected size = 3", copy.size() == 3);

		copy.push(4);
		assert("Expected copy != stack", !copy.equals(stack));
		assert("Expected hashCode = 10", copy.hashCode() == 10);
		assert("Expected size = 4", copy.size() == 4);

		// Grow it
		IntStack big = new IntStack();
		for ( int i = 1; i <= 200; ++i )
			big.push(i);
		assert("Expected size = 200", big.size() == 200);
		assert("Expected hashCode = 10", big.hashCode() == 10);

		// Test clear
		IntStack toClear = new IntStack();
		toClear.push(1);
		toClear.push(2);
		toClear.push(3);

		assert("Expected size = 3", toClear.size() == 3);
		toClear.clear();
		assert("Expected size = 0", toClear.size() == 0);

		toClear.push(1);
		assert("Expected size = 1", toClear.size() == 1);
		iterator = toClear.iterator();
		assert("Expected iterator.next = 1", iterator.next() == 1);
		assert("Expected iterator.hasNext = 0", !iterator.hasNext());

		// Test addAll
		IntStack destination = new IntStack();
		destination.push(1);
		destination.push(2);
		destination.push(3);

		destination.addAll(big);

		assert("Expected size = 203, is " + destination.size(), destination.size() == 203);
		iterator = destination.iterator();
		assert("Expected iterator.next = 1", iterator.next() == 1);
		assert("Expected iterator.next = 2", iterator.next() == 2);
		assert("Expected iterator.next = 3", iterator.next() == 3);
		assert("Expected iterator.next = 1", iterator.next() == 1);
		assert("Expected iterator.next = 2", iterator.next() == 2);
		assert("Expected iterator.next = 3", iterator.next() == 3);
	}
}
