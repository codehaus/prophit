package orbit.gui;

public interface BrowserLauncher
{
	/**
	 * @return true if there is a browser available and a network connection.
	 */
	public boolean isLaunchPossible();
	
	/**
	 * Attempt to show the given document in a browser.
	 *
	 * @param href document path relative to the document base
	 * @return true if the attempt was successful.
	 */
	public boolean showDocument(String href);
}
