package test;

public class BasicTestCalls
{
	// 100 seconds in the root
	public final TestCall root = new TestCall("root", 1, 100);

	public final TestCall main = new TestCall("main", 1, 70);
	public final TestCall events = new TestCall("events", 1, 30);
	
	public final TestCall dbOpen = new TestCall("dbOpen", 1, 40);
	public final TestCall dbClose = new TestCall("dbClose", 1, 5);
	public final TestCall dbInit = new TestCall("dbInit", 1, 5);

	{
		root.addChild(main);
		root.addChild(events);

		main.addChild(dbOpen);
		main.addChild(dbClose);
		main.addChild(dbInit);
	}
}
