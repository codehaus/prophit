package orbit.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class SampleProfileType
{
	private final String name;
	private final ArrayList profiles = new ArrayList();
	
	public SampleProfileType(String name)
	{
		this.name = name;
	}

	public String getName() { return name; }

	public void addProfile(SampleProfile profile)
	{
		profiles.add(profile);
	}

	public List getProfiles()
	{
		return Collections.unmodifiableList(profiles);
	}

	public String toString()
	{
		return name + "[ " + profiles.size() + " profiles ]";
	}
}
