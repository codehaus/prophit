package util;

public class HelloWorld
{
	static int ITERATIONS = 100000;
	static int DEPTH = 4;
	
	public static void main(String[] args)
	{
		for ( int i = 0; i < ITERATIONS; ++i )
			helloWorld(0);
	}

	public static void helloWorld(int depth)
	{
		if ( depth == DEPTH )
			return;
		
		String hello = new String("Hello World " + depth);
		helloWorld(depth + 1);
	}
}
