package orbit.gui;

import junit.framework.TestCase;

public class TestUIUtil
	extends TestCase
{
	public TestUIUtil(String name)
	{
		super(name);
	}

	public void testShortName()
	{
		String name;

		name = "orbit.gui.UIUtil.getShortName(UIUtil.java)";
		assertEquals("UIUtil.getShortName(UIUtil.java)", UIUtil.getShortName(name));
		
		name = "orbit.gui.UIUtil.getShortName(<unknown caller>)";
		assertEquals("UIUtil.getShortName(<unknown caller>)", UIUtil.getShortName(name));

		name = "orbit.gui.UIUtil.getShortName";
		assertEquals("UIUtil.getShortName", UIUtil.getShortName(name));

		name = "UIUtil.getShortName";
		assertEquals("UIUtil.getShortName", UIUtil.getShortName(name));

		name = "UIUtil";
		assertEquals("UIUtil", UIUtil.getShortName(name));

		name = "UIUtil.<init>";
		assertEquals("UIUtil.<init>", UIUtil.getShortName(name));

		name = "orbit.gui.UIUtil.<init>(UIUtil.java:38)";
		assertEquals("UIUtil.<init>(UIUtil.java:38)", UIUtil.getShortName(name));

		name = "orbit\\gui\\UIUtil.<init>(UIUtil.java:38)";
		assertEquals("UIUtil.<init>(UIUtil.java:38)", UIUtil.getShortName(name));

		name = "orbit/gui/UIUtil.<init>(UIUtil.java:38)";
		assertEquals("UIUtil.<init>(UIUtil.java:38)", UIUtil.getShortName(name));

		name = "UIUtil.getShortName(UIUtil.java)";
		assertEquals("UIUtil.getShortName(UIUtil.java)", UIUtil.getShortName(name));
	}
}
