package orbit.gui;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

/**
 * Tests loading the sample profiles from the sampleProfiles.properties resource
 */
public class TestSampleProfiles
	extends TestCase
{
	private SampleProfiles samples;

	public TestSampleProfiles(String name) 
	{ 
		super(name); 
	}
	
	public void setUp() throws IOException
	{
		samples = SampleProfiles.load("sampleProfiles.properties");
	}
	
	public void testDocURL()
	{
		assertEquals("http://ironworks.no-ip.org/orbit/samples.html", samples.getDocURL());
	}

	/**
	 * Test that the basic profile data fields are extracted properly.
	 */
	public void testProfileData()
	{
		assertEquals(2, samples.getProfileTypes().size());

		SampleProfileType pt = (SampleProfileType)samples.getProfileTypes().get(0);
		assertEquals("hprof", pt.getName());
		assertEquals(2, pt.getProfiles().size());

		SampleProfile profile = (SampleProfile)pt.getProfiles().get(0);
		assertEquals("HelloList", profile.getName());
			
		profile = (SampleProfile)pt.getProfiles().get(1);
		assertEquals("hsqldb", profile.getName());

		pt = (SampleProfileType)samples.getProfileTypes().get(1);
		assertEquals("prof", pt.getName());
		assertEquals(1, pt.getProfiles().size());
		
		profile = (SampleProfile)pt.getProfiles().get(0);
		assertEquals("HelloList", profile.getName());
		assertEquals("docs/hello-list-prof-sample.html", profile.getDocURL());
	}

	/**
	 * Test extracting the sample data file into a temp File
	 */
	public void testFileExtract()
	{
		{
			SampleProfileType hprof = (SampleProfileType)samples.getProfileTypes().get(0);
			SampleProfile helloList = (SampleProfile)hprof.getProfiles().get(0);
			File tmpFile = helloList.getFile();
			assertEquals(73992, tmpFile.length());
		}	
		{
			SampleProfileType prof = (SampleProfileType)samples.getProfileTypes().get(1);
			SampleProfile helloList = (SampleProfile)prof.getProfiles().get(0);
			File tmpFile = helloList.getFile();
			assertEquals(153267, tmpFile.length());
		}
	}
}
