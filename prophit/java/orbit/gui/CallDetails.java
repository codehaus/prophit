package orbit.gui;

import orbit.model.Call;
import orbit.util.Log;
import orbit.util.Pair;

import org.apache.log4j.Category;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.swing.table.TableModel;
import javax.swing.table.AbstractTableModel;

/**
 * Data model for the display of detailed information on a particular method call. This information includes:
 * <ul>
 * <li>Full name of the method
 * <li>Inclusive and exclusive amount of time taken up for all Calls whose name matches the method call
 * <li>Number of calls, similarly aggregated.
 * <li>Aggregated data on callers and callees
 * </ul>
 */
class CallDetails
{
	public static Category LOG = Category.getInstance(CallDetails.class);

	private final Call   root;
	private final String callName;

	private double inclusiveTime = 0;
	private double exclusiveTime = 0;
	private int nCalls = 0;
	private CallRollupList callersRollup = new CallRollupList();
	private CallRollupList calleesRollup = new CallRollupList();

	/**
	 * Construct the CallDetails.
	 * @param root The root of the currently rendered BlockDiagram.
	 * @param selected The selected Call for which the CallDetails will be computed.
	 */
	public CallDetails(Call root, final Call selected)
	{
		this.root = root;
		this.callName = selected.getName();

		/*
		 * This class recurses through the call graph, computing aggregated statistics on the selected call.
		 *
		 * The inclusive time of a matching call is added to the total inclusive time only if it is not a recursive
		 * invocation of the selected method.
		 *
		 * Similarly, callers and callees are only added to the corresponding CallRollupLists if they are the root-most
		 * instances of their caller/callee pair. This prevents time from being counted multiple times when there is recursion
		 * in the call graph.
		 */
		class SearchFor
		{
			private CallAdapter adapter = new CallAdapter();
			
			public void search(Call parent, Call call, boolean isRecursive, Set callerSet, Set calleeSet)
			{
				Pair callerPair = null;
				Pair calleePair = null;
				boolean isMatch = false;

				if ( call.getName().equals(selected.getName()) )
				{
					isMatch = true;

					Log.debug(LOG, "Call ", call, " matches selected call ", selected.getName());
					adapter.initialize(call);

					double callExclusiveTime = adapter.getExclusiveTime(TimeMeasure.TotalTime);

					Log.debug(LOG, "\tAdding exclusive time : ", callExclusiveTime);

					exclusiveTime += callExclusiveTime;
					nCalls += adapter.getCallCount();

					if ( !isRecursive )
					{
						inclusiveTime += adapter.getInclusiveTime(TimeMeasure.TotalTime);
					}

					if ( parent != null )
					{
						callerPair = new Pair(parent.getName(), call.getName());
						if ( callerSet.add(callerPair) )
							callersRollup.addCaller(parent, call);
						else
							callerPair = null;
					}
				}

				isRecursive |= isMatch;

				for ( Iterator i = call.getChildren().iterator(); i.hasNext(); )
				{
					Call child = (Call)i.next();
					if ( isMatch )
					{
						calleePair = new Pair(call.getName(), child.getName());
						if ( calleeSet.add(calleePair) )
							calleesRollup.addCallee(child);
						else
							calleePair = null;
					}

					search(call, child, isRecursive, callerSet, calleeSet);

					if ( calleePair != null )
						calleeSet.remove(calleePair);
				}
				
				if ( callerPair != null )
					callerSet.remove(callerPair);
			}
		}

		SearchFor search = new SearchFor();
		search.search(null, root, false, new HashSet(), new HashSet());

		callersRollup.sort();
		calleesRollup.sort();
	}

	public String getCallName()
	{
		return callName;
	}

	/**
	 * Get the total amount of time in the root call of the entire CallGraph.
	 */
	public double getRootTime()
	{
		Call root = this.root;
		while ( root.getParent() != null )
			root = root.getParent();
		return root.getTime();
	}

	public double getInclusiveTime()
	{
		return inclusiveTime;
	}

	public double getExclusiveTime()
	{
		return exclusiveTime;
	}

	public int getNumCalls()
	{
		return nCalls;
	}

	/**
	 * Construct a Swing TableModel which can be used to display the aggregated
	 * caller information.
	 * @see CallRollupList
	 */
	public TableModel getCallersModel()
	{
		return new CallList(Strings.getUILabel(CallDetails.class, "columnName.callers"), callersRollup);
	}

	/**
	 * Construct a Swing TableModel which can be used to display the aggregated
	 * callee information.
	 * @see CallRollupList
	 */
	public TableModel getCalleesModel()
	{
		return new CallList(Strings.getUILabel(CallDetails.class, "columnName.callees"), calleesRollup);
	};

	public interface CallTableModel
	{
		public String getCallName(int row);
	}

	/* Used for testing */
	CallRollupList getCallersRollupList() { return callersRollup; }
	CallRollupList getCalleesRollupList() { return calleesRollup; }

	private class CallList
		extends AbstractTableModel
		implements CallTableModel
	{
		private final String         name;
		private final CallRollupList list;
		
		public CallList(String listName, CallRollupList list)
		{
			this.name = listName;
			this.list = list;
		}

		public String getCallName(int row)
		{
			return list.getCallName(row);
		}
			
		public String getColumnName(int column) 
		{
			switch ( column )
			{
			case 0:
				return name;
			case 1:
				return Strings.getUILabel(CallDetails.class, "columnName.time");
			default:
				return "<unexpected column " + column + ">";
			}
		}

		public int getRowCount()
		{
			return list.size();
		}
		
		public int getColumnCount()
		{
			return 2;
		}
		
		public Object getValueAt(int row, int column)
		{
			switch ( column )
			{
			case 0:
				return UIUtil.getShortName(list.getCallName(row));
			case 1:
				return getTimeString(row);
			default:
				return "<unexpected column " + column + ">";
			}
		}

		private String getTimeString(int index)
		{
			double time = list.getTime(index);
			double totalTime = list.getTotalTime();
			return UIUtil.formatTime(time) + " (" + UIUtil.formatPercent(time / totalTime)+  ")";
		}
	}
}
