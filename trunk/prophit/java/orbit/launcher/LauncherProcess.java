package orbit.launcher;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;

/**
 * This is the abstraction for Processes that run within the launcher
 *
 */
public interface LauncherProcess
{
	/**
	 * Adds a listener to be notified when the process exits
	 *
	 * @param listener an <code>ProcessListener</code> value
	 */
	public void addProcessListener(ProcessListener listener);

	/**
	 * Removes the given listener if it exists
	 *
	 * @param listener an <code>ProcessListener</code> value
	 */
	public void removeProcessListener(ProcessListener listener);

	/**
	 * Prints msg to the stdout for the process
	 *
	 * @see #setStreams
	 * @param msg a <code>String</code> value
	 */
	public void println(String msg);

	/**
	 * Prints msg to the stderr for the process
	 *
	 * @see #setStreams
	 * @param msg a <code>String</code> value
	 */
	public void error(String msg);


	/**
	 * Blocking read of a line of input from the process' stdin
	 *
	 * @see #setStreams
	 * @return a <code>String</code> value
	 */
	public String readline();

	/**
	 * Returns a descriptive name for the process
	 *
	 * @return a <code>String</code> value
	 */
	public String getName();

	/**
	 * Returns a unique integer representation of the process.  
	 * This is unique per LaunchManager instance.  See the LaunchManager docs
	 * for details on when multiple instances are created.
	 *
	 * @return an <code>int</code> value
	 * @see LaunchManager
	 */
	public int getPID();

	/**
	 * Returns the exit code produced by the process
	 *
	 * @return an <code>int</code> value
	 */
	public int getRetcode();

	/**
	 * Gets the context for the process.  Items added to this map will
	 * be avaiable to the underlying jython script by accessing
	 * launcher.context, and likewise if values are set in the script via
	 * launcher.context, they can be accessed by doing a lookup in this map.
	 *
	 * @return a <code>Map</code> value */
	public Map getContext();

	/**
	 * Gets the ThreadGroup object that all sub threads of this process
	 * run under.
	 *
	 * @return a <code>ThreadGroup</code> value */
	public ThreadGroup getThreadGroup();

	/**
	 * Start running the process.
	 *
	 */
	public void start();

	/**
	 * Block until the process completes
	 *
	 */
	public void join();

	/**
	 * Kills the process
	 *
	 */
	public void kill();

	/**
	 * Returns true if the process is running
	 *
	 * @return a <code>boolean</code> value
	 */
	public boolean isAlive();

	/**
	 * Sets the Streams used by a process for directing its stdout,
	 * stderr and stdin.  If these are not set, then the process will
	 * use some default set of streams - typically System.out/err/in
	 *
	 * @param stdout an <code>OutputStream</code> value to map stdout for the process to
	 * @param stderr an <code>OutputStream</code> value to map stderr for the process to
	 * @param stdin an <code>InputStream</code> value to map stdin for the process to*/
	public void setStreams(OutputStream stdout, OutputStream stderr, InputStream stdin);

	/**
	 * Returns the stream being used by the process for standard output
	 *
	 * @return an <code>OutputStream</code> value
	 */
	public OutputStream getOutputStream();

	/**
	 * Returns the stream being used by the process for standard error
	 *
	 * @return an <code>OutputStream</code> value
	 */
	public OutputStream getErrorStream();

	/**
	 * Returns the stream being used by the process for standard input
	 *
	 * @return an <code>InputStream</code> value
	 */
	public InputStream getInputStream();

	/**
	 * Sets the working directory (cwd) for the process.
	 *
	 * @param dir a <code>String</code> representing the directory to run in
	 */
	public void setWorkingDirectory(String dir);

	/**
	 * Gets the value the working directory is set to.  Returns null if none set.
	 *
	 * @return a <code>String</code> value, null if none set
	 */
	public String getWorkingDirectory();

	/**
	 * Gets the property set that was used to configure this process
	 *
	 * @return a <code>PropertiesOverride</code> value, null if none set
	 */
	public Properties getProperties();


}
