package orbit.gui;

import orbit.model.Call;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;

/**
 * Contains the detailed information about the selected call. This includes the time and numCalls information for the
 * selected call, as well as the tables of callers and callees.
 * <p>
 * This class uses the {@link BlockDiagramModel} as its model. When a new profile is loaded, {@link #setModel} can be used
 * to update the CallDetailsView.
 * <p>
 * This class fires PropertyChangeEvents when a caller or callee is double-clicked by the user.
 *
 * @see #addListener
 */
class CallDetailsView
	extends JPanel
{
	/**
	 * PropertyChangeEvent fired when the user double-clicks or otherwise selects a row in the callers table.
	 * @see #addListener
	 */
	public static final String SELECTED_CALLER_PROPERTY = "selectedCaller";
	/**
	 * PropertyChangeEvent fired when the user double-clicks or otherwise selects a row in the callees table.
	 * @see #addListener
	 */
	public static final String SELECTED_CALLEE_PROPERTY = "selectedCallee";

	public static final int MINIMUM_WIDTH = 150;
	public static final int MINIMUM_HEIGHT = 100;
	public static final int PREFERRED_WIDTH = 300;
	public static final int PREFERRED_HEIGHT = 200;
	
	private JTable tblCallInfo;
	private JTable tblCallers;
	private JTable tblCallees;
	private BlockDiagramModel blockModel;
	private PropertyChangeSupport changeSupport;

	public CallDetailsView(BlockDiagramModel blockModel)
	{
		super(new BorderLayout());

		changeSupport = new PropertyChangeSupport(this);
		
		addComponents();

		setModel(blockModel);
	}

	public void setModel(BlockDiagramModel blockModel)
	{
		this.blockModel = blockModel;
		this.blockModel.addListener(new PropertyChangeListener()
			{
				public void propertyChange(PropertyChangeEvent evt)
				{
					if ( BlockDiagramModel.RENDER_CALL_PROPERTY.equals(evt.getPropertyName()) ||
						 BlockDiagramModel.SELECTED_CALL_PROPERTY.equals(evt.getPropertyName()) )
					{
						selectedCallChanged();
					}
				}
			});

		selectedCallChanged();
	}		

	/**
	 * @see #removeListener
	 */
	public synchronized void addListener(PropertyChangeListener listener)
	{
		changeSupport.addPropertyChangeListener(listener);
	}
	
	/**
	 * @see #addListener
	 */
	public synchronized void removeListener(PropertyChangeListener listener)
	{
		changeSupport.removePropertyChangeListener(listener);
	}

	/**
	 * Invoke this method when the selected call is changed. It can be changed by the user selecting
	 * a new block with the mouse, or by navigating through the callers and callees by double-clicking on
	 * the corresponding tables, when the diagram is drawn with a new root render call, or when a new
	 * profile is loaded.
	 */
	public void selectedCallChanged()
	{
		Call rootCall = blockModel.getRootRenderState().getRenderCall();
		Call selectedCall = blockModel.getSelectedCall();

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
		else
		{
			tblCallInfo.setModel(new DefaultTableModel());
			tblCallers.setModel(new DefaultTableModel());
			tblCallees.setModel(new DefaultTableModel());
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
						String callName = ((CallDetails.CallTableModel)tblCallers.getModel()).getCallName(row);
						changeSupport.firePropertyChange(SELECTED_CALLER_PROPERTY, null, callName);
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
						String callName = ((CallDetails.CallTableModel)tblCallees.getModel()).getCallName(row);
						changeSupport.firePropertyChange(SELECTED_CALLEE_PROPERTY, null, callName);
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
