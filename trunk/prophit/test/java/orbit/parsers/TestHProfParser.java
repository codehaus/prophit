package orbit.parsers;

import orbit.model.*;
import orbit.parsers.HProfParser;

import junit.framework.TestCase;

import java.util.*;
import java.io.*;

public class TestHProfParser
	extends TestCase
{
	public TestHProfParser(String name)
	{
		super(name);
	}

	public void testParse() throws Exception
	{
		File file = new File(System.getProperty("basedir") + "/test/data/hello.hprof.txt");
		
		HProfParser parser = new HProfParser(new FileReader(file));
		parser.execute();

		List callIDs = parser.getCallIDs();

		// System.out.println(callIDs);
		System.out.println("TestHProfParser needs more work");
	}

	static CallID callID(List callIDs, int index)
	{
		return (CallID)callIDs.get(index);
	}
}
