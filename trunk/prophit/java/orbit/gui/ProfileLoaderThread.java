package orbit.gui;

import orbit.model.CallGraph;
import orbit.parsers.Loader;
import orbit.parsers.LoaderFactory;
import orbit.util.Log;
import orbit.util.Util;

import org.apache.log4j.Category;

import java.io.File;

class ProfileLoaderThread
	extends Thread
{
	public static Category LOG = Category.getInstance(ProfileLoaderThread.class);

	private final File     profileFile;
	private final Callback cb;

	public ProfileLoaderThread(File profileFile, Callback cb)
	{
		this.profileFile = profileFile;
		this.cb = cb;
	}

	public void run()
	{
		long startTime = System.currentTimeMillis();
		Log.debug(LOG, "Parsing ", profileFile);

		CallGraph cg = null;
		String error = null;
		try
		{
			Loader loader = LoaderFactory.instance().createLoader(profileFile);
			loader.parse();

			cb.parsed();
			
			System.gc();
			Log.debug(LOG, "\tParsed in " + ( System.currentTimeMillis() - startTime ) + " ms");
				
			startTime = System.currentTimeMillis();

			cb.solved();

			cg = loader.solve();
			error = loader.getError();
			
			System.gc();
			Log.debug(LOG, "\tSolved in " + ( System.currentTimeMillis() - startTime ) + " ms");
		}
		catch (Exception x)
		{
			Util.handleTrace(getClass(), x);
			error = x.getMessage();
		}
		finally
		{
			cb.loadComplete(cg, error);
		}
	}

	interface Callback
	{
		public void parsed();

		public void solved();

		public void loadComplete(CallGraph cg, String errorText);
	}
}		
