package util;

import java.util.LinkedList;
import java.util.ArrayList;

public class HelloList
{
	static String HELLO_WORLD = new String("Hello World");	
	static int ITERATIONS = 100;
	
	public static void main(String[] args)
	{
		for ( int i = 0; i < ITERATIONS; ++i )
		{
			arrayAndString();
			arrayAndBuffer();
			linkedAndString();
			linkedAndBuffer();
		}
	}

	static void arrayAndString()
	{
		ArrayList list = new ArrayList();
		for ( int i = 0; i < ITERATIONS; ++i )
		{
			list.add(buildAsStrings());
		}
	}

	static void linkedAndString()
	{
		LinkedList list = new LinkedList();
		for ( int i = 0; i < ITERATIONS; ++i )
		{
			list.add(buildAsStrings());
		}
		list = (LinkedList)list.clone();
	}

	static void arrayAndBuffer()
	{
		ArrayList list = new ArrayList();
		for ( int i = 0; i < ITERATIONS; ++i )
		{
			list.add(buildAsBuffer());
		}
		list = (ArrayList)list.clone();
	}

	static void linkedAndBuffer()
	{
		LinkedList list = new LinkedList();
		for ( int i = 0; i < ITERATIONS; ++i )
		{
			list.add(buildAsBuffer());
		}
		list = (LinkedList)list.clone();
	}

	static String buildAsStrings()
	{
		String helloWorld = "";
		for ( int i = 0; i < HELLO_WORLD.length(); ++i )
			helloWorld += HELLO_WORLD.charAt(i);
		return helloWorld;
	}
	
	static String buildAsBuffer()
	{
		StringBuffer helloWorld = new StringBuffer();
		for ( int i = 0; i < HELLO_WORLD.length(); ++i )
			helloWorld.append(HELLO_WORLD.charAt(i));
		return helloWorld.toString();
	}
}
