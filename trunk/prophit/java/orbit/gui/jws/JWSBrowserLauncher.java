package orbit.gui.jws;

import orbit.gui.BrowserLauncher;

import org.apache.log4j.Category;

import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Wraps the JavaWebStart (javax.jnlp) BasicService, which can be used to show documents
 * in a browser window.
 */
public class JWSBrowserLauncher
	implements BrowserLauncher
{
	public static Category LOG = Category.getInstance(JWSBrowserLauncher.class);

	public boolean isLaunchPossible()
	{
		BasicService bs = getBasicService();
		if ( bs != null )
		{
			return bs.isWebBrowserSupported() && !bs.isOffline();
		}
		else
		{
			return false;
		}
	}
	
	public boolean showDocument(String href)
	{
		BasicService bs = getBasicService();
		if ( bs != null )
		{
			try
			{
				URL url = new URL(bs.getCodeBase(), href);
				// Invoke the showDocument method
				return bs.showDocument(url);
			}
			catch (MalformedURLException x)
			{
				LOG.error(x);
			}			   
		}
		return false;
	}

	private BasicService getBasicService()
	{
		try
		{
			// Lookup the javax.jnlp.BasicService object
			return (BasicService)ServiceManager.lookup("javax.jnlp.BasicService");
		}
		catch(UnavailableServiceException x)
		{
			// Service is not supported
			LOG.debug("JNLP BasicService is not available");
			return null;
		} 		
	}
}
