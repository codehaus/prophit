package orbit.gui;

import orbit.model.Call;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class CallAdapter
	implements Call
{
	private Call call = null;
	private Call parent = null;
	private List children = null;
	private double rawTotalTimeInChildren = -1;
	private double rawAverageTimeInChildren = -1;

	/**
	 * If this constructor is used, the {@link #initialize} method must be called
	 * before this object can be used.
	 */
	public CallAdapter()
	{
	}
	
	public CallAdapter(Call call)
	{
		initialize(call);
	}

	public int hashCode()
	{
		return call.hashCode();
	}

	public boolean equals(Object other)
	{
		if ( other instanceof CallAdapter )
			return equals((CallAdapter)other);
		else
			return false;
	}

	public boolean equals(CallAdapter other)
	{
		return other != null && call.equals(other.call);
	}

	public Call getCall()
	{
		return call;
	}

	/**
	 * Re-sets all the internal state of the CallAdapter to reflect the new Call. CallAdapters
	 * are meant to be used in this way to avoid constructing lots of wrapper objects.
	 */
	public void initialize(Call call)
	{
		this.call = call;
		this.parent = null;
		this.children = null;
		rawTotalTimeInChildren = -1;
		rawAverageTimeInChildren = -1;
	}

	public double getInclusiveTime(TimeMeasure measure)
	{
		switch ( measure.code )
		{
		case TimeMeasure.TOTAL_TIME:
			return getTime();
		case TimeMeasure.AVERAGE_TIME:
			return getTime() / (double)getCallCount();
		default:
			throw new IllegalArgumentException("Unrecognized measure " + measure);
		}		
	}

	public double getExclusiveTime(TimeMeasure measure)
	{
		return getInclusiveTime(measure) - getTimeInChildren(measure);
	}

	/**
	 * In some cases, the profiler data will be inconsistent in that the sum of the child times
	 * for a Call can be more than the parent time. This method never returns a greater value than
	 * {@link #getInclusiveTime(TimeMeasure)}.
	 */
	public double getTimeInChildren(TimeMeasure measure)
	{
		double timeInChildren = getRawTimeInChildren(measure);
		if ( measure == TimeMeasure.TotalTime )
		{
			double time = getInclusiveTime(measure);
			if ( timeInChildren > time )
				timeInChildren = time;
		}
		return timeInChildren;
	}

	public double getExclusiveTimeFractionOfParentInclusiveTime(TimeMeasure measure)
	{
		if ( getParent() != null )
		{
			double parentTime = new CallAdapter(getParent()).getInclusiveTime(measure);
			if ( parentTime != 0 )
			{
				double fraction = getExclusiveTime(measure) / parentTime;
				return fraction <= 1 ? fraction : 1;
			}
			else
				return 0;
		}
		else
		{
			return 1.0;
		}
	}
	
	public double getInclusiveTimeFractionOfParentInclusiveTime(TimeMeasure measure)
	{
		if ( getParent() != null )
		{
			double parentTime = new CallAdapter(getParent()).getInclusiveTime(measure);
			if ( parentTime != 0 )
			{
				double fraction = getInclusiveTime(measure) / parentTime;
				return fraction <= 1 ? fraction : 1;
			}
			else
				return 0;
		}
		else
		{
			return 1.0;
		}
	}

	public double getInclusiveFractionOfParentChildTimes(TimeMeasure measure)
	{
		if ( getParent() != null )
		{
			double childrenTime = new CallAdapter(getParent()).getTimeInChildren(measure);
			if ( childrenTime != 0 )
			{
				double fraction = getInclusiveTime(measure) / childrenTime;
				return fraction <= 1 ? fraction : 1;
			}
			else
				return 0;
		}
		else
		{
			return 1.0;
		}
	}

	/**
	 * Get the degree to which the total inclusive time of all the child calls is greater
	 * than the inclusive time of this call. In general, of course, the sum of child times
	 * should not be greater than the parent time. But profiler data is not always accurate enough
	 * to ensure that this doesn't happen.
	 */
	public double getChildTimeScaleFactor()
	{
		double inclusiveTime = getTime();
		double rawChildTime = getRawTimeInChildren(TimeMeasure.TotalTime);
		double ratio = rawChildTime / inclusiveTime;
		if ( ratio <= 1 )
			return 1.0;
		else
			return ratio;
	}

	public int getKey() { return call.getKey(); }
	public String getName() { return call.getName(); }
	public int getDepth() { return call.getDepth(); }
	public int getCallCount() { return call.getCallCount(); }
	public double getTime() { return call.getTime(); }
	public Call getParent()
	{
		if ( parent == null )
			parent = call.getParent();
		return parent;
	}
	public List getChildren()
	{
		if ( children == null )
			children = Collections.unmodifiableList(call.getChildren());
		return children;
	}
	public String toString() { return "Adapter for " + call.toString(); }

	private double getRawTimeInChildren(TimeMeasure measure)
	{
		double time = -1;
		if ( measure == TimeMeasure.TotalTime && rawTotalTimeInChildren != -1 )
		{
			time = rawTotalTimeInChildren;
		}
		else if ( measure == TimeMeasure.AverageTime && rawAverageTimeInChildren != -1 )
		{
			time = rawAverageTimeInChildren;
		}
		if ( time == -1 )
		{
			time = 0;
			CallAdapter adapter = new CallAdapter();
			for ( Iterator i = getChildren().iterator(); i.hasNext(); )
			{
				Call call = (Call)i.next();
				adapter.initialize(call);
				time += adapter.getInclusiveTime(measure);
			}
			if ( measure == TimeMeasure.TotalTime )
				rawTotalTimeInChildren = time;
			else if ( measure == TimeMeasure.AverageTime )
				rawAverageTimeInChildren = time;
		}
		return time;
	}
}
