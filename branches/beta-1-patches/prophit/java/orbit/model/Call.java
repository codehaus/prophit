package orbit.model;

import java.util.List;

/**
 * Represents a particular method invocation in a {@link CallGraph}.
 */
public interface Call
{
	public int getKey();
		
	public String getName();

	public int getDepth();

	public int getCallCount();
	
	public double getTime();

	public Call getParent();
	
	public List getChildren();

	public String toString();
}
