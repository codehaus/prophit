package orbit.parsers;

import orbit.model.CallID;
import orbit.model.CallGraph;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

/**
 * Once the raw list of {@link CallID}s has been read from the profile file, the Solver
 * applies additional processing that is required for Orbit to display it.
 */
public interface Solver
{
	/**
	 * Construct a CallGraph from the list of CallIDs.
	 */
	public CallGraph solve(List callIDs);

	/**
	 * @return null if the CallGraph cannot be read from the cache.
	 */
	public CallGraph readFromCache(List callIDs, Reader reader) throws IOException;

	/**
	 * This method will be called after {@link #solve} returns successfully. The solved data
	 * should be stored using the Writer in such a way that it can be re-loaded in the
	 * {@link #readFromCache} method.
	 */
	public void writeToCache(Writer writer);
}
