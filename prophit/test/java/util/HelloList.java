package util;

import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;

public class HelloList
{
	static String HELLO_WORLD = new String("Hello World");	
	static int ITERATIONS = 200;
	static List theList = null;
	
	public static void main(String[] args)
	{
		int i;
		for ( i = 0; i < ITERATIONS; ++i )
		{
			arrayAndString();
			arrayAndBuffer();
			linkedAndString();
			linkedAndBuffer();
		}
		if ( ( i + 1 ) % 2 == 0 )
			System.out.println(theList);
	}

	static void arrayAndString()
	{
		ArrayList list = new ArrayList();
		for ( int i = 0; i < ITERATIONS; ++i )
		{
			list.add(buildAsStrings());
		}
		theList = (List)list.clone();
	}

	static void linkedAndString()
	{
		LinkedList list = new LinkedList();
		for ( int i = 0; i < ITERATIONS; ++i )
		{
			list.add(buildAsStrings());
		}
		theList = (List)list.clone();
	}

	static void arrayAndBuffer()
	{
		ArrayList list = new ArrayList();
		for ( int i = 0; i < ITERATIONS; ++i )
		{
			list.add(buildAsBuffer());
		}
		theList = (List)list.clone();
	}

	static void linkedAndBuffer()
	{
		LinkedList list = new LinkedList();
		for ( int i = 0; i < ITERATIONS; ++i )
		{
			list.add(buildAsBuffer());
		}
		theList = (List)list.clone();
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
