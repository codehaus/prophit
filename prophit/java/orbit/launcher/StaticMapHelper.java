package orbit.launcher;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StaticMapHelper
{
	private static Map instance = Collections.synchronizedMap(new HashMap());

	public static Map getMap()
	{
		return instance;
	}
	
	private StaticMapHelper()
	{
	}

}
