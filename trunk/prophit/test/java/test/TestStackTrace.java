package test;

import orbit.model.StackTrace;
import junit.framework.TestCase;

public class TestStackTrace
	extends TestCase
{
	public TestStackTrace(String name)
	{
		super(name);
	}

	public void testStackTrace()
	{
		String[] allCalls = { "append", "DBExec", "test", "main" };
		StackTrace st = new StackTrace(allCalls);
		assertEquals("Expected " + st.getLeafMethod() + " = 'append'", "append", st.getLeafMethod());

		assertEquals("Expected leafMethod = 'append'", "append", st.getLeafMethod());
		assertEquals("Expected leafParentMethod = 'DBExec'", "DBExec", st.getLeafParentMethod());

		StackTrace self = st.getParentStack(4);
		assertEquals(self, st);
		self = st.getLeafStack(4);
		assertEquals(self, st);

		StackTrace exec1 = st.getParentStack(3);
		StackTrace exec2 = st.getParentStack(3);
		assertEquals("Expected exec1.size == 3", exec1.size(), 3);
		assertEquals("Expected exec1 = exec2", exec1, exec2);
		assertTrue("Expected exec1 != stack", !exec1.equals(st));
		assertEquals("Expected leafMethod = 'DBExec'", "DBExec", exec1.getLeafMethod());
		assertEquals("Expected leafParentMethod = 'test'", "test", exec1.getLeafParentMethod());

		StackTrace test = exec1.getParentStack(2).getParentStack(2);
		assertEquals("Expected test.size == 2", test.size(), 2);
		assertEquals("Expected leafMethod = 'test'", "test", test.getLeafMethod());
		assertEquals("Expected leafParentMethod = 'main'", "main", test.getLeafParentMethod());

		test = st.getParentStack(2).getLeafStack(1);
		assertEquals("Expected test.size == 1", test.size(), 1);
		assertEquals("Expected leafMethod = 'test'", "test", test.getLeafMethod());
		assertNull("Expected leafParentMethod = null, is " + test.getLeafParentMethod(), test.getLeafParentMethod());
		
		StackTrace shouldBeNull = st.getParentStack(5);
		assertNull(shouldBeNull);

		String[] parentCalls = { "DBExec", "test", "main" };
		StackTrace parent = new StackTrace(parentCalls);
		assertEquals("Expected parent.size == 3", parent.size(), 3);
		assertEquals("Expected \n" + exec1 + "  = \n" + parent, exec1, parent);
	}
}
