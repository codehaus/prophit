package orbit.gui;

import orbit.model.Call;
import orbit.util.Log;

import org.apache.log4j.Category;

import java.util.Iterator;
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
		 * This class recurses through the call graph, computing aggregated data on the selected call.
		 */
		class SearchFor
		{
			private CallAdapter adapter = new CallAdapter();
			
			public void search(Call parent, Call call)
			{
				boolean match = false;
				if ( call.getName().equals(selected.getName()) )
				{
					Log.debug(LOG, "Call ", call, " matches selected call ", selected);
					match = true;
					adapter.initialize(call);
					inclusiveTime += adapter.getInclusiveTime(TimeMeasure.TotalTime);
					exclusiveTime += adapter.getExclusiveTime(TimeMeasure.TotalTime);
					nCalls += adapter.getCallCount();
					if ( parent != null )
					{
						callersRollup.addCall(parent);
					}
				}
				for ( Iterator i = call.getChildren().iterator(); i.hasNext(); )
				{
					Call child = (Call)i.next();
					if ( match )
						calleesRollup.addCall(child);
					search(call, child);
				}
			}
		}

		SearchFor search = new SearchFor();
		search.search(null, root);

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
