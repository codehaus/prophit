package orbit.model;

import java.util.Iterator;

/**
 * Abstract base class for traversing a call graph. The Traversal iterates through the call graph,
 * calling the {@link #nextCall} function for each {@link Call}.
 */
public abstract class Traversal
{
	public void execute(Call call)
	{
		traverse(call, 0);
	}

	/**
	 * @param depth the depth in the CallGraph, relative to the call used to
	 * {@link #execute} the tranversal, at which this <code>call</code> occurs
	 * @return true if the traversal should continue.
	 */
	protected abstract boolean beginCall(Call call, int depth);

	/**
	 * Called after all the children of a <code>call</code> have been processed.
	 * beginCall and endCall invocations are guaranteed to occur in pairs.
	 */
	protected abstract void endCall();

	private void traverse(Call call, int depth)
	{
		try
		{
			if ( !beginCall(call, depth) )
				return;
			
			for ( Iterator i = call.getChildren().iterator(); i.hasNext(); )
			{
				Call child = (Call)i.next();
				traverse(child, depth + 1);
			}
		}
		finally
		{
			endCall();
		}
	}
}
