package orbit.launcher;

/**
 * The interface implemented by those objects that want to be notified when a LauncherProcess starts or exits
 *
 * @see LauncherProcess
 */
public interface ProcessListener
{
	/**
	 * When process exits, it will call this method on all the
	 * ProcessListeners it knows about
	 *
	 * @param process the <code>LauncherProcess</code> which is signalling the exit
	 */
	public void receiveExit(LauncherProcess process);
	
	/**
	 * When process starts, it will call this method on all the
	 * ProcessListeners it knows about
	 *
	 * @param process the <code>LauncherProcess</code> which is signalling the start
	 */
	public void receiveStart(LauncherProcess process);
}
