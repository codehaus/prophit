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

	/**
	 * Construct a Swing TableModel which can be used to display the aggregated
	 * caller information.
	 * @see CallRollupList
	 */
	public TableModel getCallersModel()
	{
		return new CallList(callersRollup);
	}

	/**
	 * Construct a Swing TableModel which can be used to display the aggregated
	 * callee information.
	 * @see CallRollupList
	 */
	public TableModel getCalleesModel()
	{
		return new CallList(calleesRollup);
	};

	private class CallList
		extends AbstractTableModel
	{
		private final CallRollupList list;
		
		public CallList(CallRollupList list)
		{
			this.list = list;
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
				return list.getCallName(row);
			case 1:
				return new Double(list.getTime(row));
			default:
				return "<unexpected column " + column + ">";
			}
		}
	}
}
