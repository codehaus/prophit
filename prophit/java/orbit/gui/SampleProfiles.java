package orbit.gui;

import orbit.util.ConfigurationException;
import orbit.util.Log;

import org.apache.log4j.Category;

import java.io.InputStream;
import java.io.IOException;
import java.util.*;

/**
 * Contains the sample profile data files. 
 */
class SampleProfiles
{
	public static Category LOG = Category.getInstance(SampleProfiles.class);

	private final ArrayList profileTypes = new ArrayList();
	private String docURL = null;

	/**
	 * Load the sample data information from the named resource. This resource is loaded
	 * as a class loader resource, not a class resource. See the 'sampleProfiles.properties' resource
	 * for a description of its format.
	 */
	public static SampleProfiles load(String resourceName) throws IOException
	{
		InputStream is = SampleProfiles.class.getClassLoader().getResourceAsStream(resourceName);
		Properties props = new Properties();
		props.load(is);
		return new SampleProfiles(props);
	}
	
	public SampleProfiles(Properties sampleDataFiles)
	{
		TreeSet profileIDs = new TreeSet();
		/*
		 * For each of the property names
		 *   pick out the doc URL
		 *   tokenize the 
		 */
		for ( Enumeration names = sampleDataFiles.propertyNames(); names.hasMoreElements(); )
		{
			String name = (String)names.nextElement();
			if ( "documentation.url".equals(name) )
			{
				docURL = sampleDataFiles.getProperty(name);
			}
			else
			{
				// Each propertyName should be a <sample-id>.<value> pair
				StringTokenizer tok = new StringTokenizer(name, ".");
				String profileID = tok.nextToken();
				String value = tok.nextToken();
				if ( profileIDs.add(profileID) )
				{
					Log.debug(LOG, "Adding profileID ", profileID);
				}
				if ( "profileType".equals(value) )
				{
					String profileTypeName = sampleDataFiles.getProperty(name);
					SampleProfileType pt = getProfileType(profileTypeName, false);
					if ( pt == null )
					{
						Log.debug(LOG, "Creating SampleProfileType for ", profileTypeName);
						pt = new SampleProfileType(profileTypeName);
						profileTypes.add(pt);
					}
				}
			}
		}

		if ( docURL == null )
		{
			throw new ConfigurationException("documentation.url not found in " + sampleDataFiles);
		}

		Collections.sort(profileTypes, new Comparator()
			{
				public int compare(Object o1, Object o2) 
				{ return ((SampleProfileType)o1).getName().compareTo(((SampleProfileType)o2).getName()); }
			});

		for ( Iterator i = profileIDs.iterator(); i.hasNext(); )
		{
			String profileID = (String)i.next();
			String profileTypeName = sampleDataFiles.getProperty(profileID + ".profileType");
			String name = sampleDataFiles.getProperty(profileID + ".name");
			String dataResource = sampleDataFiles.getProperty(profileID + ".dataResource");
			if ( profileTypeName == null || name == null || dataResource == null )
				throw new ConfigurationException("profileType, name, or dataResource missing for profileID " + profileID);
			SampleProfileType pt = getProfileType(profileTypeName, true);
			pt.addProfile(new SampleProfile(name, dataResource));
		}
	}

	public String getDocURL()
	{
		return docURL;
	}

	public List getProfileTypes()
	{
		return Collections.unmodifiableList(profileTypes);
	}

	private SampleProfileType getProfileType(String name, boolean must)
	{
		for ( Iterator i = profileTypes.iterator(); i.hasNext(); )
		{
			SampleProfileType pt = (SampleProfileType)i.next();
			if ( name.equals(pt.getName()) )
				return pt;
		}
		if ( must )
			throw new ConfigurationException("Profile type " + name + " not found in " + profileTypes);
		else
			return null;
	}
}
