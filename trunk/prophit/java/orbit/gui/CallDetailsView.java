package orbit.gui;

import orbit.model.Call;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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

	public Dimension getMinimumSize() { return new Dimension(100, 300); }

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
		
		JSplitPane bottomSplitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
															  new JScrollPane(tblCallers),
															  new JScrollPane(tblCallees))
			{
				public Dimension getMinimumSize() { return new Dimension(100, 200); }
			};

		JSplitPane topSplitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
															  new JScrollPane(tblCallInfo),
															  bottomSplitter)
			{
				public Dimension getMinimumSize() { return new Dimension(100, 300); }
			};

		topSplitter.setDividerLocation(0.333);
		bottomSplitter.setDividerLocation(0.5);

		add(topSplitter, BorderLayout.CENTER);
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
					return Strings.getUILabel(CallDetailsView.class, "columnName.name");
				case 1:
					return details.getCallName();
				}
			case 1:
				switch ( column )
				{
				case 0:
					return Strings.getUILabel(CallDetailsView.class, "columnName.inclusive");
				case 1:
					return new Double(details.getInclusiveTime());
				}
			case 2:
				switch ( column )
				{
				case 0:
					return Strings.getUILabel(CallDetailsView.class, "columnName.exclusive");
				case 1:
					return new Double(details.getExclusiveTime());
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
}
