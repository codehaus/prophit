package orbit.launcher;

/**
  * Utility code for working with Threads
  */
public class ThreadUtil
{

	/**
	 * Finds and returns the top most thread group for the current thread.
	 * @return ThreadGroup
	 */
	public static ThreadGroup getMainThreadGroup()
	{
		ThreadGroup currentTG = Thread.currentThread().getThreadGroup();
		ThreadGroup mainTG = currentTG;
		while(true)
		{
			ThreadGroup parent = mainTG.getParent();
			if (parent == null)
				break;
			else
				mainTG = parent;
		}
		return mainTG;
	}
}
