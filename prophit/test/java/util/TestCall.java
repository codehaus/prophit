package util;

import orbit.model.*;
import orbit.gui.*;

import java.util.ArrayList;
import java.util.List;

public class TestCall
	implements Call
{
	private final ArrayList children = new ArrayList();
	private final String name;
	private final int count;
	private final int key;
	private final long time;

	private int depth = 0;
	private Call parent = null;

	private static int KEY = 0;
	
	public TestCall(String name, int count, long time)
	{
		this.name = name;
		this.count = count;
		this.time = time;
		this.key = ++KEY;
	}

	public void addChild(TestCall child)
	{
		children.add(child);
		child.depth = this.depth + 1;
		child.parent = this;
	}
	
	public int getKey() { return key; }
	public String getName() { return name; }
	public int getMaxDepth() { throw new RuntimeException("getMaxDepth not implemented"); }
	public Call filter(Filter filter) { throw new RuntimeException("filter not implemented"); }
	public void depthFirstTraverse(Visitor visitor) { throw new RuntimeException("depthFirstTraverse not implemented"); }
	public int getDepth() { return depth; }
	public int getCallCount() { return count; }	
	public double getTime() { return time; }
	public Call getParent() { return parent; }	
	public List getChildren() { return children; }
	
	public String toString(int depth) { throw new RuntimeException("toString(int) not implemented"); }
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(getName());sb.append(" { numInvocations : ");sb.append(getCallCount());
		sb.append(", timeMillis : ");sb.append(getTime());sb.append(" }");
		return sb.toString();
	}
}
