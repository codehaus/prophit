package orbit.launcher;
import java.util.HashMap;
import java.util.Map;

// Base class (via delegation) for redirecting access to objects based
// on the threadgroup of the thread they run in.  This is used by
// classes that wish to replace static data in java.lang.System so
// that multiple thread can have a different view of the world within
// the same java process.
public class ThreadRedirector implements Redirector
{
	Map threadMap = new HashMap();
	Object defaultResource;

	public ThreadRedirector (Object defaultResource)
	{
		// Prevent cycles of redirectors for when we want to save user
		// by returning defaultResource in getResource
		if (defaultResource instanceof Redirector)
			throw new IllegalArgumentException("Cannot use a redirector as the default resource for a redirector");
		this.defaultResource = defaultResource;
	}

	public void addMap(ThreadGroup tgKey, Object resource)
	{
		threadMap.put(tgKey, resource);
	}

	public void removeMap(ThreadGroup tgKey)
	{
		threadMap.remove(tgKey);
	}

	public Object getDefaultResource()
	{
		return defaultResource;
	}

	public Object getResource()
	{
		ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
		Object resultResource = defaultResource;

		while (threadGroup != null) 
		{
			Object currentResource = threadMap.get(threadGroup);
			if (currentResource == null) 
			{
				threadGroup = threadGroup.getParent();
			}
			else 
			{
				resultResource = currentResource;
				break;
			}
		}
		// If user inadvertantly adds this as a mapped resource, 
		if (resultResource == this)
			return defaultResource;
		else
			return resultResource;
	}

	// Does group directly own the current thread i.e. group is a
	// parent of thread without owning some other group in the map
	// which in turn owns thread
	public boolean isGroupCurrentResourceOwner(ThreadGroup group)
	{
		Thread thread = Thread.currentThread();
		ThreadGroup initialThreadGroup = thread.getThreadGroup();

		if (group != null && group.parentOf(initialThreadGroup)) 
		{
			if (group == initialThreadGroup) 
			{
				return true;
			}
			else 
			{
				// group is a parent of current thread, but there are
				// other groups between them in the ancestor heirarchy, so
				// we check all those to make sure that they don't
				// directly own the thread
				ThreadGroup threadGroup = initialThreadGroup.getParent();
				while (threadGroup != group)
				{
					if (threadMap.containsKey(threadGroup))
					{
						return false;
					}
					else 
					{
						threadGroup = threadGroup.getParent();
					}
				}
				return true;
			}
		}
		return false;
	}

}
