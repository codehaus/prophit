package orbit.gui;

import junit.framework.TestCase;

public class TestWildcardMatchExpression
	extends TestCase
{
	public TestWildcardMatchExpression(String name)
	{
		super(name);
	}

	public void testExpressions()
	{
		String name = "Kevin Gilpin";
		
		WildcardMatchExpression expr;

		expr = new WildcardMatchExpression("*Gilpin*");
		assertTrue(expr.match(name));

		expr = new WildcardMatchExpression("*");
		assertTrue(expr.match(name));

		expr = new WildcardMatchExpression("K*");
		assertTrue(expr.match(name));

		expr = new WildcardMatchExpression("*n");
		assertTrue(expr.match(name));

		expr = new WildcardMatchExpression("*evin Gilpin");
		assertTrue(expr.match(name));

		expr = new WildcardMatchExpression("*Kevin Gilpin*");
		assertTrue(expr.match(name));

		expr = new WildcardMatchExpression("K*");
		assertTrue(expr.match(name));
		
		expr = new WildcardMatchExpression("Kg*");
		assertTrue(!expr.match(name));

		expr = new WildcardMatchExpression("Kevin*");
		assertTrue(expr.match(name));

		expr = new WildcardMatchExpression("?evin*");
		assertTrue(expr.match(name));

		expr = new WildcardMatchExpression("??vin *il?in*");
		assertTrue(expr.match(name));

		expr = new WildcardMatchExpression("?");
		assertTrue(!expr.match(name));

		expr = new WildcardMatchExpression("?*n?");
		assertTrue(!expr.match(name));

		expr = new WildcardMatchExpression("KE*");
		assertTrue(!expr.match(name));

		expr = new WildcardMatchExpression("<root>");
		assertTrue(expr.match("<root>"));
		
		expr = new WildcardMatchExpression("*<root>*");
		assertTrue(expr.match("<root>"));
	}
}
