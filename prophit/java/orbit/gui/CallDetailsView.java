package orbit.gui;

import orbit.model.Call;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

abstract class CallDetailsView
	extends JPanel
{
	public static final int MINIMUM_WIDTH = 150;
	public static final int MINIMUM_HEIGHT = 100;
	public static final int PREFERRED_WIDTH = 300;
	public static final int PREFERRED_HEIGHT = 200;
	
	private JTable tblCallInfo;
	private JTable tblCallers;
	private JTable tblCallees;

	public CallDetailsView()
	{
		super(new BorderLayout());

		addComponents();
	}

	public abstract void callerSelected(String callerName);

	public abstract void calleeSelected(String calleeName);

	public void selectedCallChanged(Call rootCall, Call selectedCall)
	{
		// Might as well leave it alone if the user clicks empty space
		if ( selectedCall != null )
		{
			CallDetails details = new CallDetails(rootCall, selectedCall);
			
			tblCallInfo.setModel(new CallInfoModel(details));
			tblCallInfo.getColumnModel().getColumn(0).setPreferredWidth(40);

			tblCallers.setModel(details.getCallersModel());
			tblCallers.getColumnModel().getColumn(1).setPreferredWidth(40);
			tblCallees.setModel(details.getCalleesModel());
			tblCallees.getColumnModel().getColumn(1).setPreferredWidth(40);
		}
	}

	private void addComponents()
	{
		tblCallInfo = new JTable()
			{
				public String getToolTipText(MouseEvent event)
				{
					/*
					 * Get the row index
					 * Get the call name of the row & return it
					 */
					int row = rowAtPoint( event.getPoint() );
					int column = columnAtPoint( event.getPoint() );
					if ( row == 0 && column == 1 )
					{
						return ((CallInfoModel)getModel()).getDetails().getCallName();
					}
					else
					{
						return null;
					}
				}			
			};
		tblCallers = new CallListTable();
		tblCallers.addMouseListener(new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if ( ( e.getModifiers() & MouseEvent.BUTTON1_MASK ) != 0 &&
						 e.getClickCount() == 2 )
					{
						int row = tblCallers.rowAtPoint(e.getPoint());
						// System.out.println("Double-clicked " + row);
						callerSelected(((CallDetails.CallTableModel)tblCallers.getModel()).getCallName(row));
					}
				}
			});
		tblCallees = new CallListTable();
		tblCallees.addMouseListener(new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if ( ( e.getModifiers() & MouseEvent.BUTTON1_MASK ) != 0 &&
						 e.getClickCount() == 2 )
					{
						int row = tblCallees.rowAtPoint(e.getPoint());
						// System.out.println("Double-clicked " + row);
						calleeSelected(((CallDetails.CallTableModel)tblCallees.getModel()).getCallName(row));
					}
				}
			});

		JScrollPane scrollCallers = new JScrollPane(tblCallers);
		JScrollPane scrollCallees = new JScrollPane(tblCallees);
		JScrollPane scrollInfo    = new JScrollPane(tblCallInfo);

		JSplitPane bottomSplitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
												   scrollCallers,
												   scrollCallees);
		JSplitPane topSplitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
												scrollInfo,
												bottomSplitter);

		Dimension minimum = new Dimension(150, 100);
		Dimension preferred = new Dimension(300, 200);
		
		scrollCallers.setMinimumSize(minimum);
		scrollCallers.setPreferredSize(preferred);
		scrollCallees.setMinimumSize(minimum);
		scrollCallees.setPreferredSize(preferred);
		scrollInfo.setMinimumSize(minimum);
		scrollInfo.setPreferredSize(preferred);
		
		add(topSplitter, BorderLayout.CENTER);

		/*
		bottomSplitter.resetToPreferredSizes();
		topSplitter.resetToPreferredSizes();
		topSplitter.setDividerLocation(0.333);
		bottomSplitter.setDividerLocation(0.5);
		*/
	}

	private class CallInfoModel
		extends AbstractTableModel
	{
		private final CallDetails details;
		
		public CallInfoModel(CallDetails details)
		{
			this.details = details;
		}

		public CallDetails getDetails() { return details; }
		
		public String getColumnName(int column) 
		{
			return "";
		}

		public int getRowCount()
		{
			return 4;
		}
		
		public int getColumnCount()
		{
			return 2;
		}
		
		public Object getValueAt(int row, int column)
		{
			switch ( row )
			{
			case 0:
				switch ( column )
				{
				case 0:
					return Strings.getUILabel(CallDetailsView.class, "columnName.name");
				case 1:
					return UIUtil.getShortName(details.getCallName());
				}
			case 1:
				switch ( column )
				{
				case 0:
					return Strings.getUILabel(CallDetailsView.class, "columnName.inclusive");
				case 1:
					return UIUtil.formatTime(details.getInclusiveTime()) +
						" (" + UIUtil.formatPercent(details.getInclusiveTime() / details.getRootTime()) +
						" " + Strings.getUILabel(CallDetailsView.class, "ofProgram") + ")";
				}
			case 2:
				switch ( column )
				{
				case 0:
					return Strings.getUILabel(CallDetailsView.class, "columnName.exclusive");
				case 1:
					return UIUtil.formatTime(details.getExclusiveTime()) +
						" (" + UIUtil.formatPercent(details.getExclusiveTime() / details.getRootTime()) +
						" " + Strings.getUILabel(CallDetailsView.class, "ofProgram") + ")";
				}
			case 3:
				switch ( column )
				{
				case 0:
					return Strings.getUILabel(CallDetailsView.class, "columnName.nCalls");
				case 1:
					return new Integer(details.getNumCalls());
				}
			}
			return "<unexpected row, column " + row + ", " + column + ">";
		}
	}

	private static class CallListTable
		extends JTable
	{
		public CallListTable()
		{
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
		
		public String getToolTipText(MouseEvent event)
		{
			/*
			 * Get the row index
			 * Get the call name of the row & return it
			 */
			int row = rowAtPoint( event.getPoint() );
			if (row == -1)
				return null;

			CallDetails.CallTableModel model = (CallDetails.CallTableModel)getModel();
			return model.getCallName(row);
		}			
	}
}
