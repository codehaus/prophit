package orbit.gui;

import orbit.util.ConfigurationException;

import org.apache.log4j.Category;

import java.io.*;

class SampleProfile
{	
	public static Category LOG = Category.getInstance(SampleProfile.class);

	private final String name;
	private final String resource;
	private final String docURL;

	public SampleProfile(String name, String resource, String docURL)
	{
		this.name = name;
		this.resource = resource;
		this.docURL = docURL;
	}

	public String getName()
	{
		return name;
	}

	public String getDocURL()
	{
		return docURL;
	}

	public File getFile()
	{
		File tempFile = null;
		try
		{
			InputStream is = getClass().getClassLoader().getResourceAsStream(resource);
			if ( is == null )
				throw new ConfigurationException("Unable to load sample profile " + resource);
			tempFile = File.createTempFile(name, null);
			FileOutputStream fos = new FileOutputStream(tempFile);
			int read;
			byte[] bytes = new byte[8192];
			while ( ( read = is.read(bytes) ) != -1 )
			{
				fos.write(bytes, 0, read);
			}
			fos.close();
			tempFile.deleteOnExit();
			return tempFile;
		}
		catch (IOException x)
		{
			String error = "Unable to extract sample profile " + name + " to temp file " + tempFile;
			LOG.error(error, x);
			throw new RuntimeException(error);
		}
	}
}
