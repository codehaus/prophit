package orbit.gui;

import orbit.model.Call;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

class CallDetailsView
	extends JPanel
{
	private JTable tblCallInfo;
	private JTable tblCallers;
	private JTable tblCallees;

	public CallDetailsView()
	{
		super(new BorderLayout());

		addComponents();
	}

	public Dimension getMinimumSize() { return new Dimension(400, 100); }

	public void selectedCallChanged(Call rootCall, Call selectedCall)
	{
		// Might as well leave it alone if the user clicks empty space
		if ( selectedCall != null )
		{
			CallDetails details = new CallDetails(rootCall, selectedCall);
			
			tblCallInfo.setModel(new CallInfoModel(details));
			tblCallers.setModel(details.getCallersModel());
			tblCallees.setModel(details.getCalleesModel());
		}
	}

	private void addComponents()
	{
		tblCallInfo = new JTable();
		tblCallers = new JTable();
		tblCallees = new JTable();
		
		JSplitPane rightSplitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
															  tblCallers,
															  tblCallees)
			{
				public Dimension getMinimumSize() { return new Dimension(400, 200); }
			};

		JSplitPane leftSplitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
															  tblCallInfo,
															  rightSplitter)
			{
				public Dimension getMinimumSize() { return new Dimension(200, 200); }
			};

		leftSplitter.setDividerLocation(0.333);
		rightSplitter.setDividerLocation(0.5);

		add(leftSplitter, BorderLayout.CENTER);
	}

	private class CallInfoModel
		extends AbstractTableModel
	{
		private final CallDetails details;
		
		public CallInfoModel(CallDetails details)
		{
			this.details = details;
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
					return "Name";
				case 1:
					return details.getCallName();
				}
			case 1:
				switch ( column )
				{
				case 0:
					return "Inclusive time";
				case 1:
					return new Double(details.getInclusiveTime());
				}
			case 2:
				switch ( column )
				{
				case 0:
					return "Exclusive time";
				case 1:
					return new Double(details.getExclusiveTime());
				}
			case 3:
				switch ( column )
				{
				case 0:
					return "Number of calls";
				case 1:
					return new Integer(details.getNumCalls());
				}
			}
			return "<unexpected row, column " + row + ", " + column + ">";
		}
	}
}
