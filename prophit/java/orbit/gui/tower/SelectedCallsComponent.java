package orbit.gui.tower;

import orbit.gui.BlockDiagramModel;
import orbit.model.Call;
import orbit.util.Log;

import org.apache.log4j.Category;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.List;

public class SelectedCallsComponent
	extends AbstractHighlightBlockComponent
{
	public static Category LOG = Category.getInstance(SelectedCallsComponent.class);
	
	protected void addListeners()
	{
		this.model.addListener(new PropertyChangeListener()
			{
				public void propertyChange(PropertyChangeEvent evt)
				{
					if ( BlockDiagramModel.RENDER_CALL_PROPERTY.equals(evt.getPropertyName()) ||
						 BlockDiagramModel.NUM_LEVELS_PROPERTY.equals(evt.getPropertyName()) ||
						 BlockDiagramModel.SELECTED_CALL_PROPERTY.equals(evt.getPropertyName()) )
					{
						Log.debug(LOG, "Got PropertyChangeEvent ", evt, ". Invalidating SelectedCallsComponent");
						invalidate();
					}
				}
			});
	}
	
	public void paintComponent()
	{
		List selectedCalls = model.getAllSelectedCalls();

		Log.debug(LOG, "selectedCalls are ", selectedCalls);
		if ( selectedCalls.isEmpty() )
			return;

		Iterator i = selectedCalls.iterator();
		Call selectedCall = (Call)i.next();
		if ( !highlightBlock(selectedCall, colorModel.getSelectedCallColor()) )
		{
			model.setSelectedCall(null);
			return;
		}

		while ( i.hasNext() )
		{
			Call call = (Call)i.next();
			highlightBlock(call, colorModel.getMatchingSelectedCallColor(), true);
			// highlightBlock(call, colorModel.getSelectedCallColor(), true);
		}
	}


}
