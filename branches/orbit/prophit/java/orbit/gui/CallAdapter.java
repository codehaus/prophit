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

	public CallAdapter()
	{
	}
	
	public CallAdapter(Call call)
	{
		initialize(call);
	}
	
	public void initialize(Call call)
	{
		this.call = call;
		this.parent = null;
		this.children = null;
	}

	public double getTime(TimeMeasure measure)
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

	public double getTimeInSelf(TimeMeasure measure)
	{
		return getTime(measure) - getTimeInChildren(measure);
	}

	/**
	 * In some cases, the profiler data will be inconsistent in that the sum of the child times
	 * for a Call can be more than the parent time. This method never returns a greater value than
	 * {@link #getTime(TimeMeasure)}.
	 */
	public double getTimeInChildren(TimeMeasure measure)
	{
		double timeInChildren = 0;
		CallAdapter adapter = new CallAdapter();
		for ( Iterator i = getChildren().iterator(); i.hasNext(); )
		{
			Call call = (Call)i.next();
			adapter.initialize(call);
			timeInChildren += adapter.getTime(measure);
		}
		if ( measure == TimeMeasure.TotalTime )
		{
			double time = getTime(measure);
			if ( timeInChildren > time )
				timeInChildren = time;
		}
		return timeInChildren;
	}

	public double getSelfFractionOfParentTime(TimeMeasure measure)
	{
		if ( getParent() != null )
		{
			double parentTime = new CallAdapter(getParent()).getTime(measure);
			if ( parentTime != 0 )
			{
				double fraction = getTimeInSelf(measure) / parentTime;
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
	
	public double getFractionOfParentTime(TimeMeasure measure)
	{
		if ( getParent() != null )
		{
			double parentTime = new CallAdapter(getParent()).getTime(measure);
			if ( parentTime != 0 )
			{
				double fraction = getTime(measure) / parentTime;
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

	public double getFractionOfParentChildTimes(TimeMeasure measure)
	{
		if ( getParent() != null )
		{
			double childrenTime = new CallAdapter(getParent()).getTimeInChildren(measure);
			if ( childrenTime != 0 )
			{
				double fraction = getTime(measure) / childrenTime;
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
	public String toString() { return call.toString(); }
}
