package orbit.gui;

import orbit.model.Call;
import orbit.util.Log;

import org.apache.log4j.Category;

import java.util.*;

/**
 * Aggregates information about calls which all reference the same class method.
 * For instance, if 3 calls are {@link #addCall added} to the CallRollupList which
 * represent the same method being called in different contexts, the CallRollupList will
 * aggregate information about the amount of time spent in
 * the calls. The calls can be {@link #sort sorted} according to the aggregate time,
 * and accessed by index.
 * <p>
 * The CallRollupList is meant to serve as the basic data structure for a table which shows
 * aggregate information about the callers and callees of a particular function.
 */
class CallRollupList
{
	private final Category LOG = Category.getInstance(CallRollupList.class);
	
	private final HashMap timeByCallName = new HashMap();
	private List callNames = null;
	private double totalTime = -1;

	/**
	 * Add a new call. The time spent in the call will be aggregated with the time
	 * spent in other calls whose {@link Call#getName name} is the same.
	 */
	public void addCall(Call call)
	{
		Double time = (Double)timeByCallName.get(call.getName());
		if ( time == null )
		{
			time = new Double(call.getTime());
		}
		else
		{
			time = new Double(time.doubleValue() + call.getTime());
		}
		timeByCallName.put(call.getName(), time);
	}

	/**
	 * After this method is invoked, the other methods of CallRollupList aside from {@link #addCall}
	 * can be used.
	 */
	public void sort()
	{
		Log.debug(LOG, "Sorting calls by time : ", timeByCallName);
		
		callNames = new ArrayList();
		callNames.addAll(timeByCallName.keySet());
		Collections.sort(callNames, new Comparator()
			{
				public int compare(Object first, Object second)
				{
					return (int)( getTime((String)second) - getTime((String)first) );
				}
			});

		Log.debug(LOG, "Result : ", callNames);
	}

	public String getCallName(int index)
	{
		checkForNull();
		return (String)callNames.get(index);
	}

	public double getTime(int index)
	{
		checkForNull();
		return getTime(getCallName(index));
	}

	public double getTotalTime()
	{
		if ( totalTime == -1 )
		{
			totalTime = 0;
			for ( Iterator i = timeByCallName.values().iterator(); i.hasNext(); )
			{
				Double t = (Double)i.next();
				totalTime += t.doubleValue();
			}
		}
		return totalTime;
	}

	public int size()
	{
		checkForNull();
		return callNames.size();
	}

	private void checkForNull()
	{
		if ( callNames == null )
			throw new NullPointerException("CallRollupList#sort must be invoked before the other methods can be used");
	}

	private double getTime(String callName)
	{
		Double time = (Double)timeByCallName.get(callName);
		if ( time == null )
			throw new IllegalArgumentException("No time found for call " + callName);
		return time.doubleValue();
	}	
}
