package bugs;

import java.util.ArrayList;

/**
Original bug report, Owen Jones (Owen.Jones@bankofengland.co.uk)

For example if an object has a child object of the same type, forming a hierarchy, and calling a method in the parent object immediately calls the same named method in the child object, then the time for that method will be reported as twice the value it should be. 
 
This can get completely out of hand, for example I'm profiling a program at the moment where calls to JComponent.paint are reported to include 337% of  the total Program Time, because I'm using a JlayeredPane, which calls the JComponent.paintChildren then JComponent.paint  alternately down a tree, so JComponent typically appears four or five times in a single stack.
 
Clearly the exclusive time of each call should be summed, but only the inclusive time of the call closest to the root on each individual branch should be summed.
*/
public class SelfCallerHierarchy
{
	public static void main(String[] args)
	{
		int count = Integer.parseInt(args[0]);
		
		SumContainer root = new SumContainer();
		SumContainer middle1 = new SumContainer();
		SumContainer middle2 = new SumContainer();

		root.add(middle1);
		root.add(middle2);
		root.add(new SimpleSum());

		SumContainer leaf = new SumContainer();

		middle1.add(leaf);
		leaf.add(new SimpleSum());
		leaf.add(new SimpleSum());

		middle2.add(new SimpleSum());
		middle2.add(new SimpleSum());
		middle2.add(new SimpleSum());

		int total = 0;
		for ( int i = 0; i < count; ++i )
		{
			total += root.add(i, i + 1);
		}
		System.out.println(total);
	}
}

class SumContainer
{
	ArrayList sums = new ArrayList();

	public void add(SumContainer sc)
	{
		sums.add(sc);
	}

	public int addChildren(int x, int y)
	{
		int sum = 0;
		for ( int i = 0; i < sums.size(); ++i )
		{
			sum += ((SumContainer)sums.get(i)).add(x, y);
		}
		return sum;
	}

	public int add(int x, int y)
	{
		return addChildren(x, y);
	}
}

class SimpleSum
	extends SumContainer
{
	public int add(int x, int y)
	{
		return x + y;
	}
}
